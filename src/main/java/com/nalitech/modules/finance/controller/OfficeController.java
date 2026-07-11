package com.nalitech.modules.finance.controller;

import com.nalitech.modules.finance.dto.FinanceDtos.CreateFeeRequest;
import com.nalitech.modules.finance.dto.FinanceDtos.CreateInvoiceRequest;
import com.nalitech.modules.finance.dto.FinanceDtos.FeeResponse;
import com.nalitech.modules.finance.dto.FinanceDtos.InvoiceResponse;
import com.nalitech.modules.finance.service.InvoiceService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/office")
@PreAuthorize("hasAnyRole('ADMIN', 'CONTADOR')")
public class OfficeController {

    private final InvoiceService invoiceService;

    public OfficeController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @GetMapping("/fees")
    public List<FeeResponse> listFees() {
        return invoiceService.listFees();
    }

    @PostMapping("/fees")
    @ResponseStatus(HttpStatus.CREATED)
    public FeeResponse createFee(@Valid @RequestBody CreateFeeRequest request) {
        return invoiceService.createFee(request);
    }

    @PostMapping("/invoices")
    @ResponseStatus(HttpStatus.CREATED)
    public InvoiceResponse createInvoice(@Valid @RequestBody CreateInvoiceRequest request) {
        return invoiceService.createInvoice(request);
    }

    @GetMapping("/invoices")
    public Page<InvoiceResponse> listInvoices(Pageable pageable) {
        return invoiceService.listInvoices(pageable);
    }

    @GetMapping("/invoices/{id}")
    public InvoiceResponse getInvoice(@PathVariable UUID id) {
        return invoiceService.getInvoice(id);
    }

    @GetMapping("/receivables/overdue")
    public List<InvoiceResponse> overdue() {
        return invoiceService.overdue();
    }
}
