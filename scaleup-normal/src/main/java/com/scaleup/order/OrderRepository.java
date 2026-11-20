package com.scaleup.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    // One query that loads the order together with its items and their products.
    @Query("""
            select distinct o
            from Order o
            join fetch o.items i
            join fetch i.product
            where o.id = :id
            """)
    Optional<Order> findWithItemsById(@Param("id") Long id);
}