package com.encens.khipus.model.finances;

import com.encens.khipus.model.BaseModel;
import com.encens.khipus.model.UpperCaseStringListener;
import com.encens.khipus.util.Constants;
import org.hibernate.validator.Length;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Entity for VoucherDetail
 *
 * @author
 * @version 1.4
 */
@Entity
@EntityListeners(UpperCaseStringListener.class)
@Table(name = "SF_TMPDET", schema = Constants.FINANCES_SCHEMA)
public class VoucherDetail implements BaseModel {

    //Fake ID, just to avoid duplicated in the EntityManager
    @Id
    @Column(name = "TIMEMILLIS", insertable = false, updatable = false)
    private String id;

    @Column(name = "NO_TRANS", nullable = false, insertable = true, updatable = false, length = 10)
    @Length(max = 10)
    private String transactionNumber;

    @Column(name = "NO_CIA", updatable = false, length = 2)
    @Length(max = 2)
    private String companyNumber = "01";

    @Column(name = "COD_UNI", updatable = false)
    private String businessUnitCode;

    @Column(name = "COD_CC", updatable = false)
    private String costCenterCode;

    @Column(name = "CUENTA", updatable = false)
    @Length(max = 31)
    private String account;

    @Column(name = "DEBE", precision = 16, scale = 2, updatable = false)
    private BigDecimal debit;

    @Column(name = "HABER", precision = 16, scale = 2, updatable = false)
    private BigDecimal credit;

    @Column(name = "MONEDA", updatable = false)
    @Enumerated(EnumType.STRING)
    private FinancesCurrencyType currency = FinancesCurrencyType.P;

    @Column(name = "TC", precision = 10, scale = 6, updatable = false)
    private BigDecimal exchangeAmount;


    public VoucherDetail(String businessUnitCode, String costCenterCode, String account,
                         BigDecimal debit, BigDecimal credit, FinancesCurrencyType currency, BigDecimal exchangeAmount) {
        this.businessUnitCode = businessUnitCode;
        this.costCenterCode = costCenterCode;
        this.account = account;
        this.debit = debit;
        this.credit = credit;
        this.currency = currency;
        this.exchangeAmount = exchangeAmount;
    }

    @PrePersist
    private void defineFakeIdentifier() {
        id = UUID.randomUUID().toString();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTransactionNumber() {
        return transactionNumber;
    }

    public void setTransactionNumber(String transactionNumber) {
        this.transactionNumber = transactionNumber;
    }

    public String getCompanyNumber() {
        return companyNumber;
    }

    public void setCompanyNumber(String companyNumber) {
        this.companyNumber = companyNumber;
    }

    public String getBusinessUnitCode() {
        return businessUnitCode;
    }

    public void setBusinessUnitCode(String businessUnitCode) {
        this.businessUnitCode = businessUnitCode;
    }

    public String getCostCenterCode() {
        return costCenterCode;
    }

    public void setCostCenterCode(String costCenterCode) {
        this.costCenterCode = costCenterCode;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public BigDecimal getDebit() {
        return debit;
    }

    public void setDebit(BigDecimal debit) {
        this.debit = debit;
    }

    public BigDecimal getCredit() {
        return credit;
    }

    public void setCredit(BigDecimal credit) {
        this.credit = credit;
    }

    public FinancesCurrencyType getCurrency() {
        return currency;
    }

    public void setCurrency(FinancesCurrencyType currency) {
        this.currency = currency;
    }

    public BigDecimal getExchangeAmount() {
        return exchangeAmount;
    }

    public void setExchangeAmount(BigDecimal exchangeAmount) {
        this.exchangeAmount = exchangeAmount;
    }
}
