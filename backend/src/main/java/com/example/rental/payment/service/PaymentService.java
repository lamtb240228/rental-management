package com.example.rental.payment.service;

import com.example.rental.billing.dto.InvoiceResponse;
import com.example.rental.billing.entity.Invoice;
import com.example.rental.billing.entity.InvoiceStatus;
import com.example.rental.billing.service.InvoiceService;
import com.example.rental.common.exception.BadRequestException;
import com.example.rental.common.exception.NotFoundException;
import com.example.rental.common.exception.ConflictException;
import com.example.rental.common.security.CurrentUserService;
import com.example.rental.payment.dto.PaymentRequest;
import com.example.rental.payment.dto.PaymentResponse;
import com.example.rental.payment.entity.Payment;
import com.example.rental.payment.entity.PaymentStatus;
import com.example.rental.payment.repository.PaymentRepository;
import com.example.rental.user.entity.UserAccount;
import com.example.rental.user.repository.UserAccountRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final InvoiceService invoiceService;
    private final UserAccountRepository userAccountRepository;
    private final CurrentUserService currentUserService;

    public PaymentService(
        PaymentRepository paymentRepository,
        InvoiceService invoiceService,
        UserAccountRepository userAccountRepository,
        CurrentUserService currentUserService
    ) {
        this.paymentRepository = paymentRepository;
        this.invoiceService = invoiceService;
        this.userAccountRepository = userAccountRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> listByInvoice(Long invoiceId) {
        invoiceService.getOwnedInvoice(invoiceId);
        return paymentRepository.findByInvoiceIdAndDeletedAtIsNullOrderByPaidAtDesc(invoiceId)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public InvoiceResponse create(Long invoiceId, PaymentRequest request) {
        Invoice invoice = invoiceService.getOwnedInvoice(invoiceId);
        if (StringUtils.hasText(request.transactionReference())
            && paymentRepository.existsByTransactionReferenceAndDeletedAtIsNull(request.transactionReference().trim())) {
            throw new ConflictException("Transaction reference already exists");
        }
        PaymentStatus status = request.paymentStatus() == null ? PaymentStatus.COMPLETED : request.paymentStatus();
        if (invoice.getStatus() == InvoiceStatus.CANCELLED) {
            throw new BadRequestException("Cannot pay a cancelled invoice");
        }
        if (status == PaymentStatus.COMPLETED) {
            BigDecimal nextPaidAmount = invoice.getPaidAmount().add(request.amount());
            if (nextPaidAmount.compareTo(invoice.getTotalAmount()) > 0) {
                throw new BadRequestException("Payment amount exceeds invoice balance");
            }
            invoice.setPaidAmount(nextPaidAmount);
            invoice.updatePaymentStatus();
        }

        UserAccount receiver = userAccountRepository.findById(currentUserService.currentUserId())
            .orElseThrow(() -> new NotFoundException("Current user not found"));

        Payment payment = new Payment();
        payment.setInvoice(invoice);
        payment.setAmount(request.amount());
        payment.setPaidAt(request.paidAt() == null ? OffsetDateTime.now() : request.paidAt());
        payment.setPaymentMethod(request.paymentMethod());
        payment.setPaymentStatus(status);
        payment.setTransactionReference(StringUtils.hasText(request.transactionReference())
            ? request.transactionReference().trim() : null);
        payment.setNote(request.note());
        payment.setReceivedBy(receiver);
        paymentRepository.save(payment);

        return invoiceService.get(invoice.getId());
    }

    public PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
            payment.getId(),
            payment.getInvoice().getId(),
            payment.getAmount(),
            payment.getPaidAt(),
            payment.getPaymentMethod(),
            payment.getPaymentStatus(),
            payment.getTransactionReference(),
            payment.getNote(),
            payment.getReceivedBy() == null ? null : payment.getReceivedBy().getId()
        );
    }
}
