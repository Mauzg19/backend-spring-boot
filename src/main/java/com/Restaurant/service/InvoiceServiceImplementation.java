package com.Restaurant.service;

import com.Restaurant.model.Invoice;
import com.Restaurant.model.Order;
import com.Restaurant.repository.InvoiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class InvoiceServiceImplementation implements InvoiceService {

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Value("${facturapi.api.key}")
    private String facturapiApiKey;

    @Override
    public Invoice generateInvoice(Order order, String rfcCliente, String razonSocial) {
        Invoice invoice = new Invoice();
        invoice.setOrder(order);
        invoice.setCreatedAt(new Date());
        invoice.setTotal(order.getTotalAmount() != null ? order.getTotalAmount().doubleValue() : null);
        invoice.setRfcCliente(rfcCliente);
        invoice.setRazonSocial(razonSocial);
        invoice.setFolio("FOLIO-" + order.getId());
        invoice.setXmlUrl(""); // Aquí pondrás la URL real del XML timbrado
        invoice.setPdfUrl(""); // Aquí pondrás la URL real del PDF generado

        // --- INTEGRACIÓN CON FACTURAPI ---
        try {
            RestTemplate restTemplate = new RestTemplate();

            // 1. Crear el cliente en Facturapi
            Map<String, Object> customerData = new HashMap<>();
            customerData.put("legal_name", razonSocial);
            customerData.put("tax_id", rfcCliente);
            customerData.put("email", order.getCustomer().getEmail());

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(facturapiApiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> customerRequest = new HttpEntity<>(customerData, headers);
            ResponseEntity<Map> customerResponse = restTemplate.postForEntity(
                "https://www.facturapi.io/v2/customers", customerRequest, Map.class
            );
            String customerId = (String) customerResponse.getBody().get("id");

            // 2. Crear la factura
            Map<String, Object> invoiceData = new HashMap<>();
            invoiceData.put("customer", customerId);
            invoiceData.put("payment_form", "01"); // Efectivo, ajusta según tu caso
            invoiceData.put("items", List.of(
                Map.of(
                    "quantity", 1,
                    "product", Map.of(
                        "description", "Consumo en restaurante",
                        "product_key", "01010101",
                        "price", invoice.getTotal()
                    )
                )
            ));

            HttpEntity<Map<String, Object>> invoiceRequest = new HttpEntity<>(invoiceData, headers);
            ResponseEntity<Map> invoiceResponse = restTemplate.postForEntity(
                "https://www.facturapi.io/v2/invoices", invoiceRequest, Map.class
            );

            Map invoiceMap = invoiceResponse.getBody();
            invoice.setXmlUrl((String) invoiceMap.get("xml_url"));
            invoice.setPdfUrl((String) invoiceMap.get("pdf_url"));

        } catch (Exception e) {
            // Si hay error, deja los campos vacíos y registra el error
            invoice.setXmlUrl("");
            invoice.setPdfUrl("");
            e.printStackTrace();
        }
        // --- FIN INTEGRACIÓN ---

        return invoiceRepository.save(invoice);
    }

    @Override
    public Invoice getInvoiceByOrderId(Long orderId) {
        return invoiceRepository.findByOrderId(orderId);
    }
}