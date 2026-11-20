package com.scaleup.order;

import com.scaleup.common.BadRequestException;
import com.scaleup.common.PaymentDeclinedException;
import com.scaleup.common.PaymentTimeoutException;
import com.scaleup.notification.NotificationSender;
import com.scaleup.payment.PaymentGateway;
import com.scaleup.payment.PaymentOutcome;
import org.springframework.stereotype.Service;

// Decides the order of the stages. Deliberately NOT @Transactional: it calls the short
// transactions in OrderTransactions, and the slow payment and notification calls,
// which must run outside any database transaction.
@Service
public class OrderService {

    private static final String ORDER_CONFIRMATION = "ORDER_CONFIRMATION";

    private final OrderTransactions orderTransactions;
    private final PaymentGateway paymentGateway;
    private final NotificationSender notificationSender;

    public OrderService(OrderTransactions orderTransactions,
                        PaymentGateway paymentGateway,
                        NotificationSender notificationSender) {
        this.orderTransactions = orderTransactions;
        this.paymentGateway = paymentGateway;
        this.notificationSender = notificationSender;
    }

    public OrderResponse placeOrder(OrderRequest request) {
        rejectRepeatedProducts(request);

        // Transaction 1 (short): reserve stock and create the order.
        OrderResponse reserved = orderTransactions.createAndReserve(request);
        Long orderId = reserved.orderId();

        // Stage 4: the slow external call. No database connection is held while it runs.
        PaymentOutcome outcome = paymentGateway.charge(orderId, reserved.totalAmount());

        return switch (outcome) {
            case SUCCESS -> {
                // Transaction 2 (short): record the payment, stock becomes sold, order is PAID.
                orderTransactions.recordPaymentSuccess(orderId, reserved.totalAmount());

                // Stage 6: slow external call, again outside any transaction.
                // Baseline behaviour (P07): if it fails, the order stays PAID and the API returns 500.
                notificationSender.send(orderId, ORDER_CONFIRMATION);

                // Transaction 3 (short): record the notification and confirm the order.
                yield orderTransactions.recordConfirmation(orderId, ORDER_CONFIRMATION);
            }
            case DECLINED -> {
                // The transaction commits first; only then do we report the failure.
                orderTransactions.recordPaymentDeclined(orderId, reserved.totalAmount());
                throw new PaymentDeclinedException(orderId);
            }
            // Baseline behaviour (P12): nothing is released, the order stays STOCK_RESERVED.
            case TIMEOUT -> throw new PaymentTimeoutException(orderId);
            case ERROR -> throw new IllegalStateException(
                    "Unexpected payment error for order " + orderId);
        };
    }

    public OrderResponse getById(Long orderId) {
        return orderTransactions.getById(orderId);
    }

    private void rejectRepeatedProducts(OrderRequest request) {
        long distinctProducts = request.items().stream()
                .map(OrderItemRequest::productId)
                .distinct()
                .count();
        if (distinctProducts != request.items().size()) {
            throw new BadRequestException("The same product appears more than once in the order");
        }
    }
}