package com.scaleup.inventory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    // Moves stock from available to reserved, only if enough is available.
    // Returns the number of rows changed: 1 = reserved, 0 = not enough stock.
    @Modifying
    @Query(value = """
            UPDATE inventory
            SET available_qty = available_qty - :quantity,
                reserved_qty  = reserved_qty + :quantity,
                version       = version + 1,
                updated_at    = now()
            WHERE product_id = :productId
              AND available_qty >= :quantity
            """, nativeQuery = true)
    int reserve(@Param("productId") Long productId, @Param("quantity") int quantity);

    // Moves stock back from reserved to available, only if that much is reserved.
    // Returns the number of rows changed: 1 = released, 0 = fewer units were reserved.
    @Modifying
    @Query(value = """
            UPDATE inventory
            SET available_qty = available_qty + :quantity,
                reserved_qty  = reserved_qty - :quantity,
                version       = version + 1,
                updated_at    = now()
            WHERE product_id = :productId
              AND reserved_qty >= :quantity
            """, nativeQuery = true)
    int release(@Param("productId") Long productId, @Param("quantity") int quantity);

    // Marks reserved units as sold: they leave the reserved count for good.
    // Returns the number of rows changed: 1 = marked, 0 = fewer units were reserved.
    @Modifying
    @Query(value = """
            UPDATE inventory
            SET reserved_qty = reserved_qty - :quantity,
                version      = version + 1,
                updated_at   = now()
            WHERE product_id = :productId
              AND reserved_qty >= :quantity
            """, nativeQuery = true)
    int markSold(@Param("productId") Long productId, @Param("quantity") int quantity);
}