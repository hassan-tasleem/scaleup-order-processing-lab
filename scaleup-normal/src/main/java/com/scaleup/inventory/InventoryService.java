package com.scaleup.inventory;

import com.scaleup.common.InsufficientStockException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    public InventoryService(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    @Transactional(readOnly = true)
    public int getAvailableQuantity(Long productId) {
        return inventoryRepository.findById(productId)
                .map(Inventory::getAvailableQty)
                .orElse(0);
    }

    // Joins the caller's transaction: if a later step of the order fails,
    // the reservation is undone together with everything else in that transaction.
    @Transactional
    public void reserve(Long productId, int quantity) {
        int changedRows = inventoryRepository.reserve(productId, quantity);
        if (changedRows == 0) {
            throw new InsufficientStockException(productId, quantity);
        }
    }

    @Transactional
    public void release(Long productId, int quantity) {
        int changedRows = inventoryRepository.release(productId, quantity);
        if (changedRows == 0) {
            throw new IllegalStateException(
                    "Cannot release " + quantity + " units of product " + productId
                            + ": fewer are reserved");
        }
    }

    @Transactional
    public void markSold(Long productId, int quantity) {
        int changedRows = inventoryRepository.markSold(productId, quantity);
        if (changedRows == 0) {
            throw new IllegalStateException(
                    "Cannot mark " + quantity + " units of product " + productId
                            + " as sold: fewer are reserved");
        }
    }
}