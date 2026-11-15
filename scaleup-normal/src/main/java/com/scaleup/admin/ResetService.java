package com.scaleup.admin;

import com.scaleup.simulation.SimulationSettings;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(name = "scaleup.admin.enabled", havingValue = "true")
public class ResetService {

    private final JdbcTemplate jdbcTemplate;
    private final SimulationSettings simulationSettings;

    public ResetService(JdbcTemplate jdbcTemplate, SimulationSettings simulationSettings) {
        this.jdbcTemplate = jdbcTemplate;
        this.simulationSettings = simulationSettings;
    }

    @Transactional
    public ResetResponse reset(SeedProfile profile) {
        // Empty everything except Flyway's own history table. Ids start from 1 again.
        jdbcTemplate.execute("""
                TRUNCATE TABLE order_items, payments, notifications, orders,
                               inventory, products, customers
                RESTART IDENTITY CASCADE
                """);

        jdbcTemplate.update("""
                INSERT INTO customers (name, email)
                SELECT 'Customer ' || n, 'customer_' || n || '@example.test'
                FROM generate_series(1, ?) AS n
                ORDER BY n
                """, profile.getCustomers());

        jdbcTemplate.update("""
                INSERT INTO products (sku, name, category, price)
                SELECT CASE
                           WHEN n <= 20 THEN 'HOT-' || lpad(n::text, 3, '0')
                           WHEN n = 21  THEN 'FLASH-001'
                           WHEN n = 22  THEN 'ROW-HOT-001'
                           ELSE 'SKU-' || lpad(n::text, 6, '0')
                       END,
                       'Product ' || n,
                       CASE
                           WHEN n <= 20 THEN 'hot'
                           WHEN n = 21  THEN 'flash'
                           WHEN n = 22  THEN 'hot-row'
                           ELSE 'category-' || (n % 10)
                       END,
                       (5 + ((n * 37) % 1000) / 10.0)::numeric(12,2)
                FROM generate_series(1, ?) AS n
                ORDER BY n
                """, profile.getProducts());

        jdbcTemplate.update("""
                INSERT INTO inventory (product_id, initial_qty, available_qty, reserved_qty)
                SELECT id,
                       CASE WHEN sku = 'FLASH-001' THEN 10 ELSE 1000000 END,
                       CASE WHEN sku = 'FLASH-001' THEN 10 ELSE 1000000 END,
                       0
                FROM products
                ORDER BY id
                """);

        // Refresh PostgreSQL's statistics so query plans match the new data.
        jdbcTemplate.execute("""
                ANALYZE customers, products, inventory, orders, order_items, payments, notifications
                """);

        // Every run starts without injected delays or failures.
        simulationSettings.reset();

        return new ResetResponse(
                profile.name(),
                count("customers"),
                count("products"),
                count("inventory"));
    }

    private long count(String table) {
        Long value = jdbcTemplate.queryForObject("SELECT count(*) FROM " + table, Long.class);
        return value == null ? 0 : value;
    }
}