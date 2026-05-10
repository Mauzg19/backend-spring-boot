package com.Restaurant.repository;

import com.Restaurant.model.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    Invoice findByOrderId(Long orderId);
}