package com.Restaurant.controller;

import com.Restaurant.model.Invoice;
import com.Restaurant.service.InvoiceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/invoice")
public class InvoiceController {

    // remove field injection
    private final InvoiceService invoiceService;

    // add constructor for injection
    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }
    @GetMapping("/{orderId}")
    public Invoice getInvoice(@PathVariable Long orderId) {
        return invoiceService.getInvoiceByOrderId(orderId);
    }
}