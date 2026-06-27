package com.example.rental.billing.service;

import com.example.rental.billing.dto.InvoiceItemRequest;
import com.example.rental.billing.dto.InvoiceItemResponse;
import com.example.rental.billing.dto.InvoiceRequest;
import com.example.rental.billing.dto.InvoiceResponse;
import com.example.rental.billing.entity.Invoice;
import com.example.rental.billing.entity.InvoiceItem;
import com.example.rental.billing.entity.InvoiceStatus;
import com.example.rental.billing.repository.InvoiceRepository;
import com.example.rental.common.exception.ConflictException;
import com.example.rental.common.exception.NotFoundException;
import com.example.rental.common.security.CurrentUserService;
import com.example.rental.contract.entity.RentalContract;
import com.example.rental.contract.service.ContractService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InvoiceService {
    private final InvoiceRepository invoiceRepository;
    private final ContractService contractService;
    private final CurrentUserService currentUserService;

    public InvoiceService(InvoiceRepository invoiceRepository, ContractService contractService, CurrentUserService currentUserService) {
        this.invoiceRepository = invoiceRepository;
        this.contractService = contractService;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponse> listMine() {
        return invoiceRepository.findByContractRoomPropertyLandlordIdAndDeletedAtIsNullOrderByCreatedAtDesc(currentUserService.currentUserId())
            .stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public InvoiceResponse get(Long id) {
        return toResponse(getOwnedInvoice(id));
    }

    @Transactional
    public InvoiceResponse create(InvoiceRequest request) {
        RentalContract contract = contractService.getOwnedContract(request.contractId());
        if (invoiceRepository.existsByContractIdAndBillingYearAndBillingMonthAndDeletedAtIsNull(
            contract.getId(), request.billingYear(), request.billingMonth())) {
            throw new ConflictException("Invoice already exists for this contract and period");
        }

        Invoice invoice = new Invoice();
        invoice.setContract(contract);
        invoice.setInvoiceNumber(resolveInvoiceNumber(request));
        invoice.setBillingYear(request.billingYear());
        invoice.setBillingMonth(request.billingMonth());
        invoice.setDueDate(request.dueDate());
        invoice.setDiscountAmount(request.discountAmount() == null ? BigDecimal.ZERO : request.discountAmount());
        invoice.setNotes(request.notes());
        for (InvoiceItemRequest itemRequest : request.items()) {
            invoice.addItem(toItem(itemRequest));
        }
        invoice.setStatus(InvoiceStatus.UNPAID);
        invoice.recalculateTotals();
        invoiceRepository.save(invoice);
        return toResponse(invoice);
    }

    public Invoice getOwnedInvoice(Long id) {
        return invoiceRepository.findByIdAndContractRoomPropertyLandlordIdAndDeletedAtIsNull(id, currentUserService.currentUserId())
            .orElseThrow(() -> new NotFoundException("Invoice not found"));
    }

    private String resolveInvoiceNumber(InvoiceRequest request) {
        if (request.invoiceNumber() != null && !request.invoiceNumber().isBlank()) {
            return request.invoiceNumber().trim();
        }
        return "INV-" + request.billingYear()
            + "%02d".formatted(request.billingMonth())
            + "-"
            + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
            + "-"
            + System.nanoTime();
    }

    private InvoiceItem toItem(InvoiceItemRequest request) {
        InvoiceItem item = new InvoiceItem();
        item.setItemType(request.itemType());
        item.setDescription(request.description().trim());
        item.setQuantity(request.quantity());
        item.setUnitPrice(request.unitPrice());
        return item;
    }

    public InvoiceResponse toResponse(Invoice invoice) {
        List<InvoiceItemResponse> items = invoice.getItems().stream()
            .map(item -> new InvoiceItemResponse(
                item.getId(),
                item.getItemType(),
                item.getDescription(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getAmount()
            ))
            .toList();

        return new InvoiceResponse(
            invoice.getId(),
            invoice.getContract().getId(),
            invoice.getInvoiceNumber(),
            invoice.getBillingYear(),
            invoice.getBillingMonth(),
            invoice.getIssueDate(),
            invoice.getDueDate(),
            invoice.getSubtotal(),
            invoice.getDiscountAmount(),
            invoice.getTotalAmount(),
            invoice.getPaidAmount(),
            invoice.getStatus(),
            invoice.getNotes(),
            items,
            invoice.getCreatedAt(),
            invoice.getUpdatedAt()
        );
    }
}
