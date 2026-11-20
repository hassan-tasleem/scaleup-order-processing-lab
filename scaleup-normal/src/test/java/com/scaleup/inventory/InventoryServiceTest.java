package com.scaleup.inventory;

import com.scaleup.common.InsufficientStockException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Runs against the real PostgreSQL database. Every test runs in a transaction
// that is rolled back at the end, so the data is left exactly as it was.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(InventoryService.class)
class InventoryServiceTest {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long productId;

    @BeforeEach
    void createProductWithStock() {
        productId = jdbcTemplate.queryForObject("""
                INSERT INTO products (sku, name, category, price)
                VALUES ('TEST-STOCK', 'Test product', 'test', 1.00)
                RETURNING id
                """, Long.class);
        jdbcTemplate.update(
                "INSERT INTO inventory (product_id, initial_qty, available_qty) VALUES (?, 10, 10)",
                productId);
    }

    private Map<String, Object> stock() {
        return jdbcTemplate.queryForMap(
                "SELECT available_qty, reserved_qty FROM inventory WHERE product_id = ?", productId);
    }

    private void assertStock(int available, int reserved) {
        Map<String, Object> stock = stock();
        assertThat(((Number) stock.get("available_qty")).intValue()).isEqualTo(available);
        assertThat(((Number) stock.get("reserved_qty")).intValue()).isEqualTo(reserved);
    }

    @Test
    void reserveMovesQuantityFromAvailableToReserved() {
        inventoryService.reserve(productId, 3);

        assertStock(7, 3);
    }

    @Test
    void reservingExactlyTheAvailableAmountWorks() {
        inventoryService.reserve(productId, 10);

        assertStock(0, 10);
    }

    @Test
    void reservingMoreThanAvailableFailsAndChangesNothing() {
        assertThatThrownBy(() -> inventoryService.reserve(productId, 11))
                .isInstanceOf(InsufficientStockException.class);

        assertStock(10, 0);
    }

    @Test
    void twoReservationsCannotTakeMoreThanIsAvailable() {
        inventoryService.reserve(productId, 6);

        assertThatThrownBy(() -> inventoryService.reserve(productId, 6))
                .isInstanceOf(InsufficientStockException.class);

        assertStock(4, 6);
    }

    @Test
    void releaseMovesQuantityBack() {
        inventoryService.reserve(productId, 4);

        inventoryService.release(productId, 4);

        assertStock(10, 0);
    }

    @Test
    void releasingMoreThanReservedFails() {
        assertThatThrownBy(() -> inventoryService.release(productId, 1))
                .isInstanceOf(IllegalStateException.class);

        assertStock(10, 0);
    }
}