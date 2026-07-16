package com.example.rental.billing.service;

import com.example.rental.billing.dto.InvoiceItemRequest;
import com.example.rental.billing.dto.InvoiceItemResponse;
import com.example.rental.billing.dto.InvoiceRequest;
import com.example.rental.billing.dto.InvoiceResponse;
import com.example.rental.billing.entity.Invoice;
import com.example.rental.billing.entity.InvoiceItem;
import com.example.rental.billing.entity.InvoiceItemType;
import com.example.rental.billing.entity.InvoiceStatus;
import com.example.rental.billing.entity.UtilityReading;
import com.example.rental.billing.repository.InvoiceRepository;
import com.example.rental.billing.repository.UtilityReadingRepository;
import com.example.rental.common.exception.ConflictException;
import com.example.rental.common.exception.NotFoundException;
import com.example.rental.common.exception.BadRequestException;
import com.example.rental.common.security.CurrentUserService;
import com.example.rental.contract.entity.RentalContract;
import com.example.rental.contract.entity.ContractStatus;
import com.example.rental.contract.service.ContractService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InvoiceService {
    private final InvoiceRepository invoiceRepository;
    private final UtilityReadingRepository utilityReadingRepository;
    private final ContractService contractService;
    private final CurrentUserService currentUserService;

    public InvoiceService(
        InvoiceRepository invoiceRepository,
        UtilityReadingRepository utilityReadingRepository,
        ContractService contractService,
        CurrentUserService currentUserService
    ) {
        this.invoiceRepository = invoiceRepository;
        this.utilityReadingRepository = utilityReadingRepository;
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
        RentalContract contract = contractService.getOwnedContractForRoomUpdate(request.contractId());
        if (contract.getStatus() != ContractStatus.ACTIVE) {
            throw new BadRequestException("Invoices can only be created for active contracts");
        }
        validateBillingPeriod(contract, request.billingYear(), request.billingMonth());
        if (invoiceRepository.existsOpenForContractPeriod(
            contract.getId(), request.billingYear(), request.billingMonth())) {
            throw new ConflictException("Invoice already exists for this contract and period");
        }
        UtilityReading utilityReading = resolveUtilityReading(contract, request);

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
        try {
            invoiceRepository.saveAndFlush(invoice);
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException("Invoice number or billing period already exists");
        }
        if (utilityReading != null) {
            utilityReading.setInvoice(invoice);
        }
        return toResponse(invoice);
    }

    @Transactional
    public InvoiceResponse cancel(Long id) {
        Invoice invoice = getOwnedInvoiceForUpdate(id);
        if (invoice.getPaidAmount().signum() > 0) {
            throw new BadRequestException("A paid or partially paid invoice cannot be cancelled");
        }
        if (invoice.getStatus() == InvoiceStatus.CANCELLED) {
            throw new BadRequestException("Invoice is already cancelled");
        }
        invoice.setStatus(InvoiceStatus.CANCELLED);
        utilityReadingRepository.findByInvoiceIdAndDeletedAtIsNull(invoice.getId())
            .ifPresent(reading -> reading.setInvoice(null));
        return toResponse(invoice);
    }

    public Invoice getOwnedInvoice(Long id) {
        return invoiceRepository.findByIdAndContractRoomPropertyLandlordIdAndDeletedAtIsNull(id, currentUserService.currentUserId())
            .orElseThrow(() -> new NotFoundException("Invoice not found"));
    }

    public Invoice getOwnedInvoiceForUpdate(Long id) {
        return invoiceRepository.findOwnedByIdForUpdate(id, currentUserService.currentUserId())
            .orElseThrow(() -> new NotFoundException("Invoice not found"));
    }

    private UtilityReading resolveUtilityReading(RentalContract contract, InvoiceRequest request) {
        boolean hasUtilityItems = request.items().stream()
            .anyMatch(item -> item.itemType() == InvoiceItemType.ELECTRICITY || item.itemType() == InvoiceItemType.WATER);
        if (invoiceRepository.existsOpenForRoomPeriod(
            contract.getRoom().getId(), request.billingYear(), request.billingMonth())) {
            throw new ConflictException("Utility charges for this room and period have already been invoiced");
        }

        UtilityReading reading = utilityReadingRepository
            .findByRoomIdAndBillingYearAndBillingMonthAndDeletedAtIsNull(
                contract.getRoom().getId(), request.billingYear(), request.billingMonth())
            .orElse(null);
        if (reading == null) {
            if (hasUtilityItems) {
                throw new BadRequestException("A utility reading is required before invoicing electricity or water");
            }
            return null;
        }
        if (reading.getInvoice() != null) {
            throw new ConflictException("Utility reading is already linked to an invoice");
        }

        validateUtilityItem(request.items(), InvoiceItemType.ELECTRICITY, reading.getElectricityUsage(), reading.getElectricityUnitPrice());
        validateUtilityItem(request.items(), InvoiceItemType.WATER, reading.getWaterUsage(), reading.getWaterUnitPrice());
        return reading;
    }

    private void validateBillingPeriod(RentalContract contract, Integer year, Integer month) {
        YearMonth billingPeriod = YearMonth.of(year, month);
        YearMonth contractStart = YearMonth.from(contract.getStartDate());
        YearMonth contractEnd = contract.getEndDate() == null ? null : YearMonth.from(contract.getEndDate());
        if (billingPeriod.isBefore(contractStart)
            || billingPeriod.isAfter(YearMonth.now())
            || (contractEnd != null && billingPeriod.isAfter(contractEnd))) {
            throw new BadRequestException("Invoice billing period must be within the contract term and cannot be in the future");
        }
    }

    private void validateUtilityItem(
        List<InvoiceItemRequest> items,
        InvoiceItemType itemType,
        BigDecimal expectedQuantity,
        BigDecimal expectedUnitPrice
    ) {
        List<InvoiceItemRequest> matchingItems = items.stream()
            .filter(item -> item.itemType() == itemType)
            .toList();
        boolean shouldBeBilled = expectedQuantity.signum() > 0;
        if ((!shouldBeBilled && !matchingItems.isEmpty()) || (shouldBeBilled && matchingItems.size() != 1)) {
            throw new BadRequestException("Invoice utility items must match the room reading for the billing period");
        }
        if (shouldBeBilled) {
            InvoiceItemRequest item = matchingItems.get(0);
            if (item.quantity().compareTo(expectedQuantity) != 0 || item.unitPrice().compareTo(expectedUnitPrice) != 0) {
                throw new BadRequestException("Invoice utility quantities and unit prices must match the room reading");
            }
        }
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
            effectiveStatus(invoice),
            invoice.getNotes(),
            items,
            invoice.getCreatedAt(),
            invoice.getUpdatedAt()
        );
    }

    private InvoiceStatus effectiveStatus(Invoice invoice) {
        if (invoice.getStatus() == InvoiceStatus.CANCELLED || invoice.getStatus() == InvoiceStatus.DRAFT) {
            return invoice.getStatus();
        }
        if (invoice.getPaidAmount().signum() == 0) {
            return invoice.getDueDate() != null && invoice.getDueDate().isBefore(LocalDate.now())
                ? InvoiceStatus.OVERDUE
                : InvoiceStatus.UNPAID;
        }
        return invoice.getPaidAmount().compareTo(invoice.getTotalAmount()) >= 0
            ? InvoiceStatus.PAID
            : InvoiceStatus.PARTIALLY_PAID;
    }
}
