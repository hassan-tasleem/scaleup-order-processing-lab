package com.scaleup.product;

import com.scaleup.common.BadRequestException;
import com.scaleup.common.NotFoundException;
import com.scaleup.inventory.InventoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final InventoryService inventoryService;

    public ProductService(ProductRepository productRepository, InventoryService inventoryService) {
        this.productRepository = productRepository;
        this.inventoryService = inventoryService;
    }

    @Transactional(readOnly = true)
    public ProductResponse getById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Product " + id + " not found"));
        int available = inventoryService.getAvailableQuantity(id);
        return ProductResponse.from(product, available);
    }

    // Loads several products with one query. Every id must exist and the product must be active.
    @Transactional(readOnly = true)
    public Map<Long, Product> getEntitiesByIds(Collection<Long> ids) {
        Map<Long, Product> found = productRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
        for (Long id : ids) {
            Product product = found.get(id);
            if (product == null) {
                throw new NotFoundException("Product " + id + " not found");
            }
            if (!product.isActive()) {
                throw new BadRequestException("Product " + id + " is not active");
            }
        }
        return found;
    }
}