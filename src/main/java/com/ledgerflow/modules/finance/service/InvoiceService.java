package com.ledgerflow.modules.finance.service;

import com.ledgerflow.modules.client.entity.Client;
import com.ledgerflow.modules.client.repository.ClientRepository;
import com.ledgerflow.modules.finance.dto.FinanceDtos.CreateFeeRequest;
import com.ledgerflow.modules.finance.dto.FinanceDtos.CreateInvoiceRequest;
import com.ledgerflow.modules.finance.dto.FinanceDtos.FeeResponse;
import com.ledgerflow.modules.finance.dto.FinanceDtos.InvoiceResponse;
import com.ledgerflow.modules.finance.entity.InvoiceStatus;
import com.ledgerflow.modules.finance.entity.OfficeFee;
import com.ledgerflow.modules.finance.entity.OfficeInvoice;
import com.ledgerflow.modules.finance.event.InvoiceEvents.InvoiceCreatedEvent;
import com.ledgerflow.modules.finance.gateway.ChargeModels.ChargeRequest;
import com.ledgerflow.modules.finance.gateway.ChargeModels.ChargeResult;
import com.ledgerflow.modules.finance.gateway.PaymentGateway;
import com.ledgerflow.modules.finance.gateway.PaymentGatewayFactory;
import com.ledgerflow.modules.finance.repository.OfficeFeeRepository;
import com.ledgerflow.modules.finance.repository.OfficeInvoiceRepository;
import com.ledgerflow.security.SecurityUtils;
import com.ledgerflow.shared.exception.ResourceNotFoundException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class InvoiceService {

    private final OfficeFeeRepository feeRepository;
    private final OfficeInvoiceRepository invoiceRepository;
    private final ClientRepository clientRepository;
    private final PaymentGatewayFactory gatewayFactory;
    private final ApplicationEventPublisher eventPublisher;

    public InvoiceService(OfficeFeeRepository feeRepository, OfficeInvoiceRepository invoiceRepository,
                          ClientRepository clientRepository, PaymentGatewayFactory gatewayFactory,
                          ApplicationEventPublisher eventPublisher) {
        this.feeRepository = feeRepository;
        this.invoiceRepository = invoiceRepository;
        this.clientRepository = clientRepository;
        this.gatewayFactory = gatewayFactory;
        this.eventPublisher = eventPublisher;
    }

    public FeeResponse createFee(CreateFeeRequest request) {
        OfficeFee fee = new OfficeFee();
        fee.setEmpresaId(SecurityUtils.currentEmpresaId());
        fee.setClienteId(request.clienteId());
        fee.setDescricao(request.descricao());
        fee.setValor(request.valor());
        fee.setPeriodicidade(request.periodicidade());
        return toResponse(feeRepository.save(fee));
    }

    @Transactional(readOnly = true)
    public List<FeeResponse> listFees() {
        return feeRepository.findByEmpresaId(SecurityUtils.currentEmpresaId())
                .stream().map(this::toResponse).toList();
    }

    public InvoiceResponse createInvoice(CreateInvoiceRequest request) {
        UUID empresaId = SecurityUtils.currentEmpresaId();
        Client cliente = clientRepository.findByIdAndEmpresaId(request.clienteId(), empresaId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente nao encontrado."));

        PaymentGateway gateway = gatewayFactory.active();
        ChargeResult charge = gateway.criarCobranca(new ChargeRequest(
                cliente.getNome(), cliente.getCnpjCpf(), request.valor(),
                request.vencimento(), request.descricao()));

        OfficeInvoice invoice = new OfficeInvoice();
        invoice.setEmpresaId(empresaId);
        invoice.setClienteId(request.clienteId());
        invoice.setFeeId(request.feeId());
        invoice.setValor(request.valor());
        invoice.setVencimento(request.vencimento());
        invoice.setStatus(InvoiceStatus.PENDENTE);
        invoice.setProvider(gateway.provider());
        invoice.setExternalId(charge.externalId());
        invoice.setBoletoUrl(charge.boletoUrl());
        invoice.setPixCopiaCola(charge.pixCopiaECola());
        invoice.setPixQrcode(charge.pixQrCode());
        OfficeInvoice saved = invoiceRepository.save(invoice);

        eventPublisher.publishEvent(new InvoiceCreatedEvent(
                saved.getId(), empresaId, saved.getClienteId(), saved.getValor(), saved.getBoletoUrl()));
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<InvoiceResponse> listInvoices(Pageable pageable) {
        return invoiceRepository.findByEmpresaId(SecurityUtils.currentEmpresaId(), pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public InvoiceResponse getInvoice(UUID id) {
        return toResponse(invoiceRepository.findByIdAndEmpresaId(id, SecurityUtils.currentEmpresaId())
                .orElseThrow(() -> new ResourceNotFoundException("Cobranca nao encontrada.")));
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponse> overdue() {
        return invoiceRepository.findByEmpresaIdAndStatusAndVencimentoBefore(
                        SecurityUtils.currentEmpresaId(), InvoiceStatus.PENDENTE, LocalDate.now())
                .stream().map(this::toResponse).toList();
    }

    private FeeResponse toResponse(OfficeFee fee) {
        return new FeeResponse(fee.getId(), fee.getClienteId(), fee.getDescricao(),
                fee.getValor(), fee.getPeriodicidade(), fee.isAtivo());
    }

    private InvoiceResponse toResponse(OfficeInvoice i) {
        return new InvoiceResponse(i.getId(), i.getClienteId(), i.getValor(), i.getVencimento(),
                i.getStatus(), i.getProvider(), i.getExternalId(), i.getBoletoUrl(), i.getPixCopiaCola());
    }
}
