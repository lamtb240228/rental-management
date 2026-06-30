package com.example.rental.payment.repository;

import com.example.rental.payment.entity.Payment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByInvoiceIdAndDeletedAtIsNullOrderByPaidAtDesc(Long invoiceId);

    List<Payment> findDistinctByInvoiceContractTenantsTenantUserAccountIdAndDeletedAtIsNullOrderByPaidAtDesc(Long userAccountId);

    boolean existsByTransactionReferenceAndDeletedAtIsNull(String transactionReference);
}
