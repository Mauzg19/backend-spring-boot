package com.Restaurant.service;

import com.Restaurant.model.Invoice;
import com.Restaurant.model.Order;

public interface InvoiceService {
    Invoice generateInvoice(Order order, String rfcCliente, String razonSocial);
    Invoice getInvoiceByOrderId(Long orderId);
}