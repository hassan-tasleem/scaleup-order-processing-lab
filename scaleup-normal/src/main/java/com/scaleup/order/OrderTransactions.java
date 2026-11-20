package com.scaleup.order;

import com.scaleup.common.NotFoundException;
import com.scaleup.customer.Customer;
import com.scaleup.customer.CustomerService;
import com.scaleup.inventory.InventoryService;
import com.scaleup.notification.Notification;
import com.scaleup.notification.NotificationRepository;
import com.scaleup.notification.NotificationStatus;
import com.scaleup.payment.Payment;
import com.scaleup.payment.PaymentOutcome;
import com.scaleup.payment.PaymentRepository;
import com.scaleup.product.Product;
import com.scaleup.product.ProductService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

// The database transactions of the order flow. Each public method is one transaction.
@Service
public class OrderTransactions {

    private final CustomerService customerService;
    private final ProductService productService;
    private final InventoryService inventoryService;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final NotificationRepository notificationRepository;

    public OrderTransactions(CustomerService customerService,
                             ProductService productService,
                             InventoryService inventoryService,
                             OrderRepository orderRepository,
                             PaymentRepository paymentRepository,
                             NotificationRepository notificationRepository) {
        this.customerService = customerService;
        this.productService = productService;
        this.inventoryService = inventoryService;
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.notificationRepository = notificationRepository;
    }

    // Transaction 1: look up, reserve stock, create the order. All or nothing.
    @Transactional
    public OrderResponse createAndReserve(OrderRequest request) {
        Customer customer = customerService.getEntityById(request.customerId());

        List<Long> productIds = request.items().stream()
                .map(OrderItemRequest::productId)
                .toList();
        Map<Long, Product> products = productService.getEntitiesByIds(productIds);

        // Ascending product id order: two orders can never wait for each other in a circle.
        List<OrderItemRequest> sortedItems = request.items().stream()
                .sorted(Comparator.comparing(OrderItemRequest::productId))
                .toList();

        BigDecimal total = BigDecimal.ZERO;
        for (OrderItemRequest item : sortedItems) {
            // Throws InsufficientStockException if there is not enough: everything rolls back.
            inventoryService.reserve(item.productId(), item.quantity());
            Product product = products.get(item.productId());
            total = total.add(product.getPrice().multiply(BigDecimal.valueOf(item.quantity())));
        }

        Order order = new Order(customer, OrderStatus.STOCK_RESERVED, total);
        for (OrderItemRequest item : sortedItems) {
            Product product = products.get(item.productId());
            order.addItem(product, item.quantity(), product.getPrice());
        }

        return OrderResponse.from(orderRepository.save(order));
    }

    // Transaction 2a: payment succeeded. Reserved units become sold, the payment is recorded,
    // the order becomes PAID.
    @Transactional
    public OrderResponse recordPaymentSuccess(Long orderId, BigDecimal amount) {
        Order order = loadOrderWithItems(orderId);
        for (OrderItem item : itemsInProductOrder(order)) {
            inventoryService.markSold(item.getProduct().getId(), item.getQuantity());
        }
        paymentRepository.save(new Payment(orderId, PaymentOutcome.SUCCESS, amount));
        order.markPaid();
        return OrderResponse.from(order);
    }

    // Transaction 2b: payment declined. The stock goes back, the payment is recorded,
    // the order becomes PAYMENT_FAILED.
    @Transactional
    public void recordPaymentDeclined(Long orderId, BigDecimal amount) {
        Order order = loadOrderWithItems(orderId);
        for (OrderItem item : itemsInProductOrder(order)) {
            inventoryService.release(item.getProduct().getId(), item.getQuantity());
        }
        paymentRepository.save(new Payment(orderId, PaymentOutcome.DECLINED, amount));
        order.markPaymentFailed();
    }

    // Transaction 3: the notification was sent. Record it and confirm the order.
    @Transactional
    public OrderResponse recordConfirmation(Long orderId, String notificationType) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order " + orderId + " not found"));
        notificationRepository.save(new Notification(orderId, notificationType, NotificationStatus.SENT));
        order.markConfirmed();
        return OrderResponse.from(order);
    }

    @Transactional(readOnly = true)
    public OrderResponse getById(Long orderId) {
        return orderRepository.findById(orderId)
                .map(OrderResponse::from)
                .orElseThrow(() -> new NotFoundException("Order " + orderId + " not found"));
    }

    private Order loadOrderWithItems(Long orderId) {
        return orderRepository.findWithItemsById(orderId)
                .orElseThrow(() -> new NotFoundException("Order " + orderId + " not found"));
    }

    private List<OrderItem> itemsInProductOrder(Order order) {
        return order.getItems().stream()
                .sorted(Comparator.comparing((OrderItem item) -> item.getProduct().getId()))
                .toList();
    }
}