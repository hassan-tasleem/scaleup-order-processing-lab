package com.scaleup.admin;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@ConditionalOnProperty(name = "scaleup.admin.enabled", havingValue = "true")
public class VerifyService {

    private static final List<String> TABLES = List.of(
            "customers", "products", "inventory", "orders",
            "order_items", "payments", "notifications");

    private final JdbcTemplate jdbcTemplate;

    public VerifyService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(readOnly = true)
    public VerifyResponse verify() {
        Map<String, Long> rowCounts = new LinkedHashMap<>();
        for (String table : TABLES) {
            rowCounts.put(table, queryForLong("SELECT count(*) FROM " + table));
        }

        Map<String, Long> ordersByStatus = new LinkedHashMap<>();
        List<Map<String, Object>> statusRows = jdbcTemplate.queryForList(
                "SELECT status, count(*) AS total FROM orders GROUP BY status ORDER BY status");
        for (Map<String, Object> row : statusRows) {
            ordersByStatus.put((String) row.get("status"), ((Number) row.get("total")).longValue());
        }
        long inFlight = ordersByStatus.getOrDefault("PENDING", 0L)
                + ordersByStatus.getOrDefault("STOCK_RESERVED", 0L)
                + ordersByStatus.getOrDefault("PAID", 0L);

        List<VerifyResponse.Check> checks = new ArrayList<>();

        checks.add(check("noNegativeStock",
                "SELECT count(*) FROM inventory WHERE available_qty < 0 OR reserved_qty < 0"));

        checks.add(check("stockBalances", """
                SELECT count(*)
                FROM inventory i
                LEFT JOIN (SELECT oi.product_id, sum(oi.quantity) AS sold
                           FROM order_items oi
                           JOIN orders o ON o.id = oi.order_id
                           WHERE o.status IN ('PAID', 'CONFIRMED')
                           GROUP BY oi.product_id) s ON s.product_id = i.product_id
                WHERE i.initial_qty <> i.available_qty + i.reserved_qty + coalesce(s.sold, 0)
                """));

        checks.add(check("everyProductHasInventory", """
                SELECT count(*)
                FROM products p
                LEFT JOIN inventory i ON i.product_id = p.id
                WHERE i.product_id IS NULL
                """));

        checks.add(check("ordersHaveItems", """
                SELECT count(*)
                FROM orders o
                WHERE NOT EXISTS (SELECT 1 FROM order_items oi WHERE oi.order_id = o.id)
                """));

        checks.add(check("orderTotalsMatchItems", """
                SELECT count(*)
                FROM orders o
                JOIN (SELECT order_id, sum(quantity * unit_price) AS total
                      FROM order_items GROUP BY order_id) t ON t.order_id = o.id
                WHERE o.total_amount <> t.total
                """));

        checks.add(check("specialProductsSeeded", """
                SELECT 2 - count(*)
                FROM products p
                JOIN inventory i ON i.product_id = p.id
                WHERE (p.sku = 'FLASH-001' AND i.initial_qty = 10)
                   OR (p.sku = 'ROW-HOT-001' AND i.initial_qty = 1000000)
                """));

        boolean allPassed = checks.stream().allMatch(VerifyResponse.Check::passed);

        return new VerifyResponse(allPassed, seedChecksum(), inFlight,
                rowCounts, ordersByStatus, checks);
    }

    private VerifyResponse.Check check(String name, String sql) {
        long violations = queryForLong(sql);
        return new VerifyResponse.Check(name, violations == 0, violations);
    }

    private String seedChecksum() {
        return jdbcTemplate.queryForObject("""
                SELECT md5(
                    coalesce((SELECT string_agg(id || ':' || name || ':' || email, ',' ORDER BY id)
                              FROM customers), '') || '|' ||
                    coalesce((SELECT string_agg(id || ':' || sku || ':' || name || ':' || category || ':' || price,
                                                ',' ORDER BY id)
                              FROM products), '') || '|' ||
                    coalesce((SELECT string_agg(product_id || ':' || initial_qty, ',' ORDER BY product_id)
                              FROM inventory), '')
                )
                """, String.class);
    }

    private long queryForLong(String sql) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class);
        return value == null ? 0 : value;
    }
}