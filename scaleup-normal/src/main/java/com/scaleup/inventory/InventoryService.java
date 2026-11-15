package com.scaleup.inventory;

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
}