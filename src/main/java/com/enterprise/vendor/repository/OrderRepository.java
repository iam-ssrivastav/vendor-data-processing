package com.enterprise.vendor.repository;

import com.enterprise.vendor.model.Order;
import com.enterprise.vendor.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Order entity.
 * 
 * @author Shivam Srivastav
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByCustomerId(String customerId);

    List<Order> findByStatus(OrderStatus status);

    List<Order> findByPaymentTransactionId(String transactionId);
}
