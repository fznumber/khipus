package com.encens.khipus.service.warehouse;

import com.encens.khipus.action.production.ProductionPlanningAction;
import com.encens.khipus.exception.finances.CompanyConfigurationNotFoundException;
import com.encens.khipus.exception.finances.FinancesCurrencyNotFoundException;
import com.encens.khipus.exception.finances.FinancesExchangeRateNotFoundException;
import com.encens.khipus.framework.service.GenericServiceBean;
import com.encens.khipus.interceptor.FinancesUser;
import com.encens.khipus.model.admin.BusinessUnit;
import com.encens.khipus.model.admin.User;
import com.encens.khipus.model.finances.*;
import com.encens.khipus.model.purchases.PurchaseOrder;
import com.encens.khipus.model.purchases.PurchaseOrderPayment;
import com.encens.khipus.model.purchases.PurchaseOrderPaymentState;
import com.encens.khipus.model.purchases.PurchaseOrderPaymentType;
import com.encens.khipus.model.warehouse.MovementDetail;
import com.encens.khipus.model.warehouse.MovementDetailType;
import com.encens.khipus.model.warehouse.WarehouseVoucher;
import com.encens.khipus.service.common.SequenceGeneratorService;
import com.encens.khipus.service.finances.FinancesExchangeRateService;
import com.encens.khipus.service.finances.RotatoryFundService;
import com.encens.khipus.service.finances.VoucherService;
import com.encens.khipus.service.fixedassets.CompanyConfigurationService;
import com.encens.khipus.service.purchases.GlossGeneratorService;
import com.encens.khipus.util.*;
import com.encens.khipus.util.warehouse.WarehouseUtil;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;

import javax.ejb.Stateless;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * @author
 * @version 3.5.2.2
 */
@Stateless
@Name("warehouseAccountEntryService")
@FinancesUser
@AutoCreate
public class WarehouseAccountEntryServiceBean extends GenericServiceBean implements WarehouseAccountEntryService {

    @In
    private VoucherService voucherService;

    @In
    private SequenceGeneratorService sequenceGeneratorService;

    @In
    private AdvancePaymentService advancePaymentService;

    @In
    private CompanyConfigurationService companyConfigurationService;

    @In
    private GlossGeneratorService glossGeneratorService;

    @In
    private FinancesExchangeRateService financesExchangeRateService;

    @In
    private RotatoryFundService rotatoryFundService;

    @In
    private User currentUser;

    @In
    private MovementDetailService movementDetailService;

    /* For advance payments of warehouse and fixedAssets */

    public void createAdvancePaymentAccountEntry(PurchaseOrderPayment purchaseOrderPayment) throws CompanyConfigurationNotFoundException {
        Voucher voucher = null;
        CompanyConfiguration companyConfiguration = companyConfigurationService.findCompanyConfiguration();

        String executorUnitCode = purchaseOrderPayment.getPurchaseOrder().getExecutorUnit().getExecutorUnitCode();
        String costCenterCode = purchaseOrderPayment.getPurchaseOrder().getCostCenter().getCode();

        CashAccount payCashAccount = null;
        if (PurchaseOrderPaymentKind.ADVANCE_PAYMENT.equals(purchaseOrderPayment.getPurchaseOrderPaymentKind())) {
            payCashAccount = FinancesCurrencyType.D.equals(purchaseOrderPayment.getPayCurrency()) ?
                    companyConfiguration.getAdvancePaymentForeignCurrencyAccount() : companyConfiguration.getAdvancePaymentNationalCurrencyAccount();
        } else if (PurchaseOrderPaymentKind.LIQUIDATION_PAYMENT.equals(purchaseOrderPayment.getPurchaseOrderPaymentKind())) {
            payCashAccount = purchaseOrderPayment.getPurchaseOrder().getProvider().getPayableAccount();
        }

        if (payCashAccount == null) {
            throw new CompanyConfigurationNotFoundException("The system configuration (payCashAccountCode) for current company haven't been configured");
        }

        BigDecimal bankExchangeRate = FinancesCurrencyType.D.equals(purchaseOrderPayment.getSourceCurrency()) ?
                purchaseOrderPayment.getExchangeRate() : BigDecimal.ONE;
        BigDecimal payExchangeRate = FinancesCurrencyType.D.equals(payCashAccount.getCurrency()) ?
                purchaseOrderPayment.getExchangeRate() : BigDecimal.ONE;
        BigDecimal payAmount = FinancesCurrencyType.D.equals(payCashAccount.getCurrency()) ?
                BigDecimalUtil.multiply(purchaseOrderPayment.getPayAmount(), purchaseOrderPayment.getExchangeRate()) : purchaseOrderPayment.getPayAmount();

        BigDecimal voucherAmountNationalAmount = FinancesCurrencyType.D.equals(purchaseOrderPayment.getSourceCurrency()) ?
                purchaseOrderPayment.getSourceAmount().multiply(purchaseOrderPayment.getExchangeRate()) : purchaseOrderPayment.getSourceAmount();

        if (PurchaseOrderPaymentType.PAYMENT_BANK_ACCOUNT.equals(purchaseOrderPayment.getPaymentType())) {
            Long sequenceNumber = sequenceGeneratorService.nextValue(Constants.ADVANCEPAYMENT_DOCUMENT_SEQUENCE);
            voucher = VoucherBuilder.newBankAccountPaymentTypeVoucher(
                    Constants.BANKACCOUNT_VOUCHERTYPE_FORM,
                    Constants.BANKACCOUNT_VOUCHERTYPE_DEBITNOTE_DOCTYPE,
                    Constants.ADVANCEPAYMENT_DOCNUMBER_PREFFIX + sequenceNumber,
                    purchaseOrderPayment.getBankAccountNumber(),
                    purchaseOrderPayment.getSourceAmount(),
                    purchaseOrderPayment.getSourceCurrency(),
                    bankExchangeRate,
                    purchaseOrderPayment.getDescription());
            /* TODO may be the beneficiary should be included in the voucher*/
        } else if (PurchaseOrderPaymentType.PAYMENT_WITH_CHECK.equals(purchaseOrderPayment.getPaymentType())) {
            voucher = VoucherBuilder.newCheckPaymentTypeVoucher(
                    Constants.CHECK_VOUCHERTYPE_FORM,
                    Constants.CHECK_VOUCHERTYPE_DOCTYPE,
                    purchaseOrderPayment.getBankAccountNumber(),
                    purchaseOrderPayment.getBeneficiaryName(),
                    purchaseOrderPayment.getSourceAmount(),
                    purchaseOrderPayment.getSourceCurrency(),
                    bankExchangeRate,
                    purchaseOrderPayment.getCheckDestination(),
                    purchaseOrderPayment.getDescription());
        } else if (PurchaseOrderPaymentType.PAYMENT_CASHBOX.equals(purchaseOrderPayment.getPaymentType())) {
            voucher = VoucherBuilder.newGeneralVoucher(Constants.CASHBOX_PAYMENT_VOUCHER_FORM, purchaseOrderPayment.getDescription());
            voucher.addVoucherDetail(VoucherDetailBuilder.newCreditVoucherDetail(
                    executorUnitCode,
                    costCenterCode,
                    purchaseOrderPayment.getCashBoxCashAccount(),
                    voucherAmountNationalAmount,
                    purchaseOrderPayment.getCashBoxCashAccount().getCurrency(),
                    bankExchangeRate));
            voucher.setEmployeeName(purchaseOrderPayment.getBeneficiaryName());
        } else if (PurchaseOrderPaymentType.PAYMENT_ROTATORY_FUND.equals(purchaseOrderPayment.getPaymentType())) {
            voucher = VoucherBuilder.newGeneralVoucher(Constants.RECEIVABLES_VOUCHER_FORM, purchaseOrderPayment.getDescription());
            CashAccount rotatoryFundCashAccount = rotatoryFundService.matchCashAccount(purchaseOrderPayment.getRotatoryFund());
            voucher.addVoucherDetail(VoucherDetailBuilder.newCreditVoucherDetail(
                    executorUnitCode,
                    costCenterCode,
                    rotatoryFundCashAccount,
                    voucherAmountNationalAmount,
                    rotatoryFundCashAccount.getCurrency(),
                    bankExchangeRate));
        }

        if (voucher != null) {
            if (purchaseOrderPayment.getAccountingEntryDefaultDate() != null) {
                voucher.setDate(purchaseOrderPayment.getAccountingEntryDefaultDate());
                voucher.setExpirationDate(purchaseOrderPayment.getAccountingEntryDefaultDate());
            }
            if (!ValidatorUtil.isBigDecimal(purchaseOrderPayment.getAccountingEntryDefaultUserNumber())) {
                voucher.setUserNumber(purchaseOrderPayment.getAccountingEntryDefaultUserNumber());
            }

            voucher.addVoucherDetail(VoucherDetailBuilder.newDebitVoucherDetail(
                    executorUnitCode,
                    costCenterCode,
                    payCashAccount,
                    payAmount,
                    payCashAccount.getCurrency(),
                    payExchangeRate));

            BigDecimal balanceAmount = BigDecimalUtil.subtract(payAmount, voucherAmountNationalAmount);
            if (balanceAmount.doubleValue() > 0) {
                voucher.addVoucherDetail(VoucherDetailBuilder.newCreditVoucherDetail(
                        executorUnitCode,
                        companyConfiguration.getExchangeRateBalanceCostCenter().getCode(),
                        companyConfiguration.getBalanceExchangeRateAccount(),
                        balanceAmount,
                        FinancesCurrencyType.P,
                        BigDecimal.ONE));
            } else if (balanceAmount.doubleValue() < 0) {
                voucher.addVoucherDetail(VoucherDetailBuilder.newDebitVoucherDetail(
                        executorUnitCode,
                        companyConfiguration.getExchangeRateBalanceCostCenter().getCode(),
                        companyConfiguration.getBalanceExchangeRateAccount(),
                        balanceAmount.abs(),
                        FinancesCurrencyType.P,
                        BigDecimal.ONE));
            }

            voucherService.create(voucher);
            purchaseOrderPayment.setTransactionNumber(voucher.getTransactionNumber());
            if (!getEntityManager().contains(purchaseOrderPayment)) {
                getEntityManager().merge(purchaseOrderPayment);
            }
            getEntityManager().flush();
        }
    }

    /* when a warehouse purchase order is been liquidated
    *  warehouse vs (advance and liquidation payments)*/

    public void createEntryAccountForLiquidatedPurchaseOrder(PurchaseOrder purchaseOrder, BigDecimal defaultExchangeRate)
            throws CompanyConfigurationNotFoundException,
            FinancesCurrencyNotFoundException,
            FinancesExchangeRateNotFoundException {
        BigDecimal sumAdvancePaymentAmount = advancePaymentService.sumAllPaymentAmountsByKind(purchaseOrder, PurchaseOrderPaymentKind.ADVANCE_PAYMENT);
        BigDecimal sumLiquidationPaymentAmount = advancePaymentService.sumAllPaymentAmountsByKind(purchaseOrder, PurchaseOrderPaymentKind.LIQUIDATION_PAYMENT);

        if (BigDecimalUtil.isZeroOrNull(sumAdvancePaymentAmount)) {
            sumAdvancePaymentAmount = BigDecimal.ZERO;
        }
        if (BigDecimalUtil.isZeroOrNull(sumLiquidationPaymentAmount)) {
            sumLiquidationPaymentAmount = BigDecimal.ZERO;
        }


        CompanyConfiguration companyConfiguration = companyConfigurationService.findCompanyConfiguration();

        if (ValidatorUtil.isBlankOrNull(companyConfiguration.getWarehouseNationalCurrencyTransientAccountCode())) {
            throw new CompanyConfigurationNotFoundException("The system configuration (warehouseNationalCurrencyAccountCode) for current company haven't been configured");
        }

        String executorUnitCode = purchaseOrder.getExecutorUnit().getExecutorUnitCode();
        String costCenterCode = purchaseOrder.getCostCenter().getCode();

        String gloss = glossGeneratorService.generatePurchaseOrderGloss(purchaseOrder,
                MessageUtils.getMessage("WarehousePurchaseOrder.warehouses"), MessageUtils.getMessage("WarehousePurchaseOrder.orderNumberAcronym"));

        Voucher voucher = VoucherBuilder.newGeneralVoucher(Constants.WAREHOUSE_VOUCHER_FORM, gloss);
        voucher.setUserNumber(companyConfiguration.getDefaultAccountancyUser().getId());
        if (CollectionDocumentType.INVOICE.equals(purchaseOrder.getDocumentType())) {
            voucher.addVoucherDetail(VoucherDetailBuilder.newDebitVoucherDetail(
                    executorUnitCode,
                    costCenterCode,
                    companyConfiguration.getWarehouseNationalCurrencyTransientAccount(),
                    BigDecimalUtil.multiply(purchaseOrder.getTotalAmount(), Constants.VAT_COMPLEMENT),
                    FinancesCurrencyType.P,
                    BigDecimal.ONE));
            voucher.addVoucherDetail(VoucherDetailBuilder.newDebitVoucherDetail(
                    executorUnitCode,
                    costCenterCode,
                    companyConfiguration.getNationalCurrencyVATFiscalCreditTransientAccount(),
                    BigDecimalUtil.multiply(purchaseOrder.getTotalAmount(), Constants.VAT),
                    FinancesCurrencyType.P,
                    BigDecimal.ONE));
        } else {
            voucher.addVoucherDetail(VoucherDetailBuilder.newDebitVoucherDetail(
                    executorUnitCode,
                    costCenterCode,
                    companyConfiguration.getWarehouseNationalCurrencyTransientAccount(),
                    purchaseOrder.getTotalAmount(),
                    FinancesCurrencyType.P,
                    BigDecimal.ONE));
        }

        BigDecimal totalCreditAmount = BigDecimal.ZERO;

        if (BigDecimalUtil.isPositive(sumAdvancePaymentAmount)) {
            voucher.addVoucherDetail(VoucherDetailBuilder.newCreditVoucherDetail(
                    executorUnitCode,
                    costCenterCode,
                    companyConfiguration.getAdvancePaymentNationalCurrencyAccount(),
                    sumAdvancePaymentAmount,
                    FinancesCurrencyType.P,
                    BigDecimal.ONE));
            totalCreditAmount = BigDecimalUtil.sum(totalCreditAmount, sumAdvancePaymentAmount);
        }

        if (BigDecimalUtil.isPositive(sumLiquidationPaymentAmount)) {
            voucher.addVoucherDetail(VoucherDetailBuilder.newCreditVoucherDetail(
                    executorUnitCode,
                    costCenterCode,
                    purchaseOrder.getProvider().getPayableAccount(),
                    sumLiquidationPaymentAmount,
                    purchaseOrder.getProvider().getPayableAccount().getCurrency(),
                    financesExchangeRateService.getExchangeRateByCurrencyType(purchaseOrder.getProvider().getPayableAccount().getCurrency(), defaultExchangeRate)));
            totalCreditAmount = BigDecimalUtil.sum(totalCreditAmount, sumLiquidationPaymentAmount);
        }

        BigDecimal balanceAmount = BigDecimalUtil.subtract(purchaseOrder.getTotalAmount(), totalCreditAmount);

        if (balanceAmount.doubleValue() > 0) {
            voucher.addVoucherDetail(VoucherDetailBuilder.newCreditVoucherDetail(
                    executorUnitCode,
                    companyConfiguration.getExchangeRateBalanceCostCenter().getCode(),
                    companyConfiguration.getBalanceExchangeRateAccount(),
                    balanceAmount,
                    FinancesCurrencyType.P,
                    BigDecimal.ONE));
        } else if (balanceAmount.doubleValue() < 0) {
            voucher.addVoucherDetail(VoucherDetailBuilder.newDebitVoucherDetail(
                    executorUnitCode,
                    companyConfiguration.getExchangeRateBalanceCostCenter().getCode(),
                    companyConfiguration.getBalanceExchangeRateAccount(),
                    balanceAmount.abs(),
                    FinancesCurrencyType.P,
                    BigDecimal.ONE));
        }
        voucherService.create(voucher);
    }
    /* when a fixedAsset or warehouse purchase order has been liquidated
    *  (bank or cashbox) vs provider */

    public void createEntryAccountForPurchaseOrderPayment(PurchaseOrder purchaseOrder, PurchaseOrderPayment purchaseOrderPayment)
            throws CompanyConfigurationNotFoundException,
            FinancesCurrencyNotFoundException,
            FinancesExchangeRateNotFoundException {
        if (purchaseOrderPayment != null && !BigDecimalUtil.isZeroOrNull(purchaseOrderPayment.getPayAmount())
                && !BigDecimalUtil.isZeroOrNull(purchaseOrderPayment.getSourceAmount())) {
            Voucher voucher = null;
            CompanyConfiguration companyConfiguration = companyConfigurationService.findCompanyConfiguration();

            purchaseOrderPayment.setPayCurrency(FinancesCurrencyType.P);
            purchaseOrderPayment.setState(PurchaseOrderPaymentState.APPROVED);
            purchaseOrderPayment.setCreationDate(new Date());
            purchaseOrderPayment.setRegisterEmployee(currentUser);
            purchaseOrderPayment.setApprovalDate(new Date());
            purchaseOrderPayment.setApprovedByEmployee(currentUser);
            purchaseOrderPayment.setPurchaseOrder(purchaseOrder);
            if (BigDecimalUtil.isZeroOrNull(purchaseOrderPayment.getExchangeRate())) {
                purchaseOrderPayment.setExchangeRate(BigDecimal.ONE);
            }

            String executorUnitCode = purchaseOrder.getExecutorUnit().getExecutorUnitCode();
            String costCenterCode = purchaseOrder.getCostCenter().getCode();
            BigDecimal bankExchangeRate = purchaseOrderPayment.getExchangeRate();
            BigDecimal payExchangeRate = purchaseOrderPayment.getExchangeRate();

            BigDecimal voucherAmountNationalAmount = BigDecimalUtil.multiply(purchaseOrderPayment.getSourceAmount(), payExchangeRate);

            if (PurchaseOrderPaymentType.PAYMENT_BANK_ACCOUNT.equals(purchaseOrderPayment.getPaymentType())) {
                Long sequenceNumber = sequenceGeneratorService.nextValue(Constants.FIXEDASSET_PAYMENT_DOCUMENT_SEQUENCE);
                voucher = VoucherBuilder.newBankAccountPaymentTypeVoucher(
                        Constants.BANKACCOUNT_VOUCHERTYPE_FORM,
                        Constants.BANKACCOUNT_VOUCHERTYPE_DEBITNOTE_DOCTYPE,
                        Constants.FIXEDASSET_PAYMENT_DOCNUMBER_PREFFIX + sequenceNumber,
                        purchaseOrderPayment.getBankAccountNumber(),
                        purchaseOrderPayment.getSourceAmount(),
                        purchaseOrderPayment.getSourceCurrency(),
                        bankExchangeRate,
                        purchaseOrderPayment.getDescription());
            } else if (PurchaseOrderPaymentType.PAYMENT_WITH_CHECK.equals(purchaseOrderPayment.getPaymentType())) {
                voucher = VoucherBuilder.newCheckPaymentTypeVoucher(
                        Constants.CHECK_VOUCHERTYPE_FORM,
                        Constants.CHECK_VOUCHERTYPE_DOCTYPE,
                        purchaseOrderPayment.getBankAccountNumber(),
                        purchaseOrderPayment.getBeneficiaryName(),
                        purchaseOrderPayment.getSourceAmount(),
                        purchaseOrderPayment.getSourceCurrency(),
                        bankExchangeRate,
                        purchaseOrderPayment.getCheckDestination(),
                        purchaseOrderPayment.getDescription());
            } else if (PurchaseOrderPaymentType.PAYMENT_CASHBOX.equals(purchaseOrderPayment.getPaymentType())) {
                voucher = VoucherBuilder.newGeneralVoucher(Constants.CASHBOX_PAYMENT_VOUCHER_FORM, purchaseOrderPayment.getDescription());
                voucher.addVoucherDetail(VoucherDetailBuilder.newCreditVoucherDetail(
                        executorUnitCode,
                        costCenterCode,
                        purchaseOrderPayment.getCashBoxCashAccount(),
                        voucherAmountNationalAmount,
                        purchaseOrderPayment.getCashBoxCashAccount().getCurrency(),
                        bankExchangeRate));
                voucher.setEmployeeName(purchaseOrderPayment.getBeneficiaryName());
            } else if (PurchaseOrderPaymentType.PAYMENT_ROTATORY_FUND.equals(purchaseOrderPayment.getPaymentType())) {
                voucher = VoucherBuilder.newGeneralVoucher(Constants.RECEIVABLES_VOUCHER_FORM, purchaseOrderPayment.getDescription());
                CashAccount rotatoryFundCashAccount = rotatoryFundService.matchCashAccount(purchaseOrderPayment.getRotatoryFund());
                voucher.addVoucherDetail(VoucherDetailBuilder.newCreditVoucherDetail(
                        executorUnitCode,
                        costCenterCode,
                        rotatoryFundCashAccount,
                        voucherAmountNationalAmount,
                        rotatoryFundCashAccount.getCurrency(),
                        bankExchangeRate));
            }
            if (voucher != null) {
                voucher.setUserNumber(companyConfiguration.getDefaultTreasuryUser().getId());
                voucher.addVoucherDetail(VoucherDetailBuilder.newDebitVoucherDetail(
                        executorUnitCode,
                        costCenterCode,
                        purchaseOrder.getProvider().getPayableAccount(),
                        purchaseOrderPayment.getPayAmount(),
                        purchaseOrder.getProvider().getPayableAccount().getCurrency(),
                        financesExchangeRateService.getExchangeRateByCurrencyType(purchaseOrder.getProvider().getPayableAccount().getCurrency(), payExchangeRate)));
                BigDecimal balanceAmount = BigDecimalUtil.subtract(purchaseOrderPayment.getPayAmount(), voucherAmountNationalAmount);
                if (balanceAmount.doubleValue() > 0) {
                    voucher.addVoucherDetail(VoucherDetailBuilder.newCreditVoucherDetail(
                            executorUnitCode,
                            companyConfiguration.getExchangeRateBalanceCostCenter().getCode(),
                            companyConfiguration.getBalanceExchangeRateAccount(),
                            balanceAmount,
                            FinancesCurrencyType.P,
                            BigDecimal.ONE));
                } else if (balanceAmount.doubleValue() < 0) {
                    voucher.addVoucherDetail(VoucherDetailBuilder.newDebitVoucherDetail(
                            executorUnitCode,
                            companyConfiguration.getExchangeRateBalanceCostCenter().getCode(),
                            companyConfiguration.getBalanceExchangeRateAccount(),
                            balanceAmount.abs(),
                            FinancesCurrencyType.P,
                            BigDecimal.ONE));
                }
                voucherService.create(voucher);

                purchaseOrderPayment.setTransactionNumber(voucher.getTransactionNumber());
                getEntityManager().persist(purchaseOrderPayment);
                getEntityManager().flush();
            }
        }
    }

    @SuppressWarnings(value = "unchecked")
    public void createAccountEntry(WarehouseVoucher warehouseVoucher, String[] gloss)
            throws CompanyConfigurationNotFoundException,
            FinancesCurrencyNotFoundException,
            FinancesExchangeRateNotFoundException {
        if (warehouseVoucher.isTransfer()) {
            log.debug("The account entry should not be generated for transference vouchers.");
            return;
        }

        if (!existsControlValuedProductsItems(warehouseVoucher)) {
            log.debug("Unable to generate the account entry because the all productItems " +
                    "related with movement details are not enabled controlValued property.");
            return;
        }

        log.debug("Generating the account entry for warehouse voucher Nro: " + warehouseVoucher.getNumber());

        if (warehouseVoucher.isReception()) {
            createAccountEntryForReception(warehouseVoucher,
                    warehouseVoucher.getExecutorUnit(),
                    warehouseVoucher.getCostCenterCode(),
                    gloss[0]);
        } else if (warehouseVoucher.isExecutorUnitTransfer()) {
            createAccountEntryForExecutorUnitTransfer(warehouseVoucher, gloss);
        } else if ((warehouseVoucher.isInput() || warehouseVoucher.isOutput()) &&
                (warehouseVoucher.getDocumentType().isContraAccountDefinedByUser() ||
                        warehouseVoucher.getDocumentType().isContraAccountDefinedByDefault())) {
            CashAccount contraAccount = warehouseVoucher.getDocumentType().isContraAccountDefinedByUser() ?
                    warehouseVoucher.getContraAccount() : warehouseVoucher.getDocumentType().getContraAccount();
            if (warehouseVoucher.isInput()) {
                createAccountEntryForInputsAndContraAccount(warehouseVoucher,
                        contraAccount,
                        warehouseVoucher.getExecutorUnit(),
                        warehouseVoucher.getCostCenterCode(),
                        gloss[0]);
            } else {
                createAccountEntryForOutputsAndContraAccount(warehouseVoucher,
                        contraAccount,
                        warehouseVoucher.getExecutorUnit(),
                        warehouseVoucher.getCostCenterCode(),
                        gloss[0]);
            }
        } else {
            MovementDetailType movementDetailType =
                    WarehouseUtil.getMovementTye(warehouseVoucher.getDocumentType());

            if (MovementDetailType.E.equals(movementDetailType)) {
                createAccountEntryForInputs(warehouseVoucher,
                        warehouseVoucher.getExecutorUnit(),
                        warehouseVoucher.getCostCenterCode(),
                        gloss[0]);
            }

            if (MovementDetailType.S.equals(movementDetailType)) {
                createAccountEntryForOutputs(warehouseVoucher,
                        warehouseVoucher.getExecutorUnit(),
                        warehouseVoucher.getCostCenterCode(),
                        gloss[0]);
            }
        }
    }

    @SuppressWarnings(value = "unchecked")
    public void createAccountEntryFromProductDelivery(WarehouseVoucher warehouseVoucher, String[] gloss)
            throws CompanyConfigurationNotFoundException,
            FinancesCurrencyNotFoundException,
            FinancesExchangeRateNotFoundException {
        if (warehouseVoucher.isTransfer()) {
            log.debug("The account entry should not be generated for transference vouchers.");
            return;
        }

        if (!existsControlValuedProductsItems(warehouseVoucher)) {
            log.debug("Unable to generate the account entry because the all productItems " +
                    "related with movement details are not enabled controlValued property.");
            return;
        }

        log.debug("Generating the account entry for warehouse voucher Nro: " + warehouseVoucher.getNumber());

        if (warehouseVoucher.isReception()) {
            createAccountEntryForReception(warehouseVoucher,
                    warehouseVoucher.getExecutorUnit(),
                    warehouseVoucher.getCostCenterCode(),
                    gloss[0]);
        } else if (warehouseVoucher.isExecutorUnitTransfer()) {
            createAccountEntryForExecutorUnitTransfer(warehouseVoucher, gloss);
        } else if ((warehouseVoucher.isInput() || warehouseVoucher.isOutput()) &&
                (warehouseVoucher.getDocumentType().isContraAccountDefinedByUser() ||
                        warehouseVoucher.getDocumentType().isContraAccountDefinedByDefault())) {
            CashAccount contraAccount = warehouseVoucher.getDocumentType().isContraAccountDefinedByUser() ?
                    warehouseVoucher.getContraAccount() : warehouseVoucher.getDocumentType().getContraAccount();
            if (warehouseVoucher.isInput()) {
                createAccountEntryForInputsAndContraAccount(warehouseVoucher,
                        contraAccount,
                        warehouseVoucher.getExecutorUnit(),
                        warehouseVoucher.getCostCenterCode(),
                        gloss[0]);
            } else {
                createAccountEntryForOutputsAndContraAccount(warehouseVoucher,
                        contraAccount,
                        warehouseVoucher.getExecutorUnit(),
                        warehouseVoucher.getCostCenterCode(),
                        gloss[0]);
            }
        } else {
            MovementDetailType movementDetailType =
                    WarehouseUtil.getMovementTye(warehouseVoucher.getDocumentType());

            if (MovementDetailType.E.equals(movementDetailType)) {
                createAccountEntryForInputs(warehouseVoucher,
                        warehouseVoucher.getExecutorUnit(),
                        warehouseVoucher.getCostCenterCode(),
                        gloss[0]);
            }

            if (MovementDetailType.S.equals(movementDetailType)) {
                createAccountEntryForOutputsFromProductDelivery(warehouseVoucher,
                        warehouseVoucher.getExecutorUnit(),
                        warehouseVoucher.getCostCenterCode(),
                        gloss[0]);
            }
        }
    }

    @SuppressWarnings(value = "unchecked")
    public void createAccountEntryFromCollection(WarehouseVoucher warehouseVoucher, String[] gloss)
            throws CompanyConfigurationNotFoundException,
            FinancesCurrencyNotFoundException,
            FinancesExchangeRateNotFoundException {
        if (warehouseVoucher.isTransfer()) {
            log.debug("The account entry should not be generated for transference vouchers.");
            return;
        }

        if (!existsControlValuedProductsItems(warehouseVoucher)) {
            log.debug("Unable to generate the account entry because the all productItems " +
                    "related with movement details are not enabled controlValued property.");
            return;
        }

        log.debug("Generating the account entry for warehouse voucher Nro: " + warehouseVoucher.getNumber());

        if (warehouseVoucher.isReception()) {
            createAccountEntryForReceptionFromCollection(warehouseVoucher,
                    warehouseVoucher.getExecutorUnit(),
                    warehouseVoucher.getCostCenterCode(),
                    gloss[0]);
        } else if (warehouseVoucher.isExecutorUnitTransfer()) {
            createAccountEntryForExecutorUnitTransfer(warehouseVoucher, gloss);
        } else if ((warehouseVoucher.isInput() || warehouseVoucher.isOutput()) &&
                (warehouseVoucher.getDocumentType().isContraAccountDefinedByUser() ||
                        warehouseVoucher.getDocumentType().isContraAccountDefinedByDefault())) {
            CashAccount contraAccount = warehouseVoucher.getDocumentType().isContraAccountDefinedByUser() ?
                    warehouseVoucher.getContraAccount() : warehouseVoucher.getDocumentType().getContraAccount();
            if (warehouseVoucher.isInput()) {
                createAccountEntryForInputsAndContraAccount(warehouseVoucher,
                        contraAccount,
                        warehouseVoucher.getExecutorUnit(),
                        warehouseVoucher.getCostCenterCode(),
                        gloss[0]);
            } else {
                createAccountEntryForOutputsAndContraAccount(warehouseVoucher,
                        contraAccount,
                        warehouseVoucher.getExecutorUnit(),
                        warehouseVoucher.getCostCenterCode(),
                        gloss[0]);
            }
        } else {
            MovementDetailType movementDetailType =
                    WarehouseUtil.getMovementTye(warehouseVoucher.getDocumentType());

            if (MovementDetailType.E.equals(movementDetailType)) {
                createAccountEntryForInputs(warehouseVoucher,
                        warehouseVoucher.getExecutorUnit(),
                        warehouseVoucher.getCostCenterCode(),
                        gloss[0]);
            }

            if (MovementDetailType.S.equals(movementDetailType)) {
                createAccountEntryForOutputs(warehouseVoucher,
                        warehouseVoucher.getExecutorUnit(),
                        warehouseVoucher.getCostCenterCode(),
                        gloss[0]);
            }
        }
    }

    @Override
    public String createAccountEntryForReceptionProductionOrder(WarehouseVoucher warehouseVoucher,
                                                BusinessUnit executorUnit,
                                                String costCenterCode,
                                                String gloss,
                                                List<ProductionPlanningAction.AccountOrderProduction> accountOrderProductions)
            throws CompanyConfigurationNotFoundException {

        CompanyConfiguration companyConfiguration = companyConfigurationService.findCompanyConfiguration();
        BigDecimal voucherAmount = movementDetailService.sumWarehouseVoucherMovementDetailAmount(warehouseVoucher.getId().getCompanyNumber(), warehouseVoucher.getState(), warehouseVoucher.getId().getTransactionNumber());

        Voucher voucherForGeneration = VoucherBuilder.newGeneralVoucher(Constants.INPUT_PROD_WAREHOUSE, gloss);
        voucherForGeneration.setUserNumber(companyConfiguration.getDefaultAccountancyUserProduction().getId());
        voucherForGeneration.addVoucherDetail(VoucherDetailBuilder.newDebitVoucherDetail(
                executorUnit.getExecutorUnitCode(),
                costCenterCode,
                companyConfiguration.getWarehouseNationalCurrencyTransientAccount2(),
                voucherAmount,
                FinancesCurrencyType.P,
                BigDecimal.ONE));

        for(ProductionPlanningAction.AccountOrderProduction accountOrderProduction :accountOrderProductions)
        {
            voucherForGeneration.addVoucherDetail(VoucherDetailBuilder.newCreditVoucherDetail(
                    accountOrderProduction.getExecutorUnit().getExecutorUnitCode(),
                    accountOrderProduction.getCostCenterCode(),
                    accountOrderProduction.getCashAccount(),
                    accountOrderProduction.getVoucherAmount(),
                    FinancesCurrencyType.P,
                    BigDecimal.ONE));
        }
        voucherForGeneration.setDate(warehouseVoucher.getDate());
        voucherService.create(voucherForGeneration);
        return voucherForGeneration.getTransactionNumber();
    }

    private void createAccountEntryForReception(WarehouseVoucher warehouseVoucher,
                                                BusinessUnit executorUnit,
                                                String costCenterCode,
                                                String gloss)
            throws CompanyConfigurationNotFoundException {

        CompanyConfiguration companyConfiguration = companyConfigurationService.findCompanyConfiguration();
        BigDecimal voucherAmount = movementDetailService.sumWarehouseVoucherMovementDetailAmount(warehouseVoucher.getId().getCompanyNumber(), warehouseVoucher.getState(), warehouseVoucher.getId().getTransactionNumber());

        Voucher voucherForGeneration = VoucherBuilder.newGeneralVoucher(Constants.WAREHOUSE_VOUCHER_FORM, gloss);
        voucherForGeneration.setUserNumber(companyConfiguration.getDefaultAccountancyUser().getId());

        voucherForGeneration.addVoucherDetail(VoucherDetailBuilder.newDebitVoucherDetail(
                executorUnit.getExecutorUnitCode(),
                costCenterCode,
                companyConfiguration.getWarehouseNationalCurrencyAccount(),
                voucherAmount,
                FinancesCurrencyType.P,
                BigDecimal.ONE));

        voucherForGeneration.addVoucherDetail(VoucherDetailBuilder.newCreditVoucherDetail(
                executorUnit.getExecutorUnitCode(),
                costCenterCode,
                companyConfiguration.getWarehouseNationalCurrencyTransientAccount(),
                voucherAmount,
                FinancesCurrencyType.P,
                BigDecimal.ONE));

        voucherService.create(voucherForGeneration);

    }

    private void createAccountEntryForReceptionFromCollection(WarehouseVoucher warehouseVoucher,
                                                              BusinessUnit executorUnit,
                                                              String costCenterCode,
                                                              String gloss)
            throws CompanyConfigurationNotFoundException {

        CompanyConfiguration companyConfiguration = companyConfigurationService.findCompanyConfiguration();
        BigDecimal voucherAmount = movementDetailService.sumWarehouseVoucherMovementDetailAmount(warehouseVoucher.getId().getCompanyNumber(), warehouseVoucher.getState(), warehouseVoucher.getId().getTransactionNumber());

        Voucher voucherForGeneration = VoucherBuilder.newGeneralVoucher(Constants.WAREHOUSE_VOUCHER_FORM, gloss);
        voucherForGeneration.setUserNumber(companyConfiguration.getDefaultAccountancyUser().getId());

        voucherForGeneration.addVoucherDetail(VoucherDetailBuilder.newDebitVoucherDetail(
                executorUnit.getExecutorUnitCode(),
                costCenterCode,
                companyConfiguration.getWarehouseNationalCurrencyAccount(),
                voucherAmount,
                FinancesCurrencyType.P,
                BigDecimal.ONE));

        voucherForGeneration.addVoucherDetail(VoucherDetailBuilder.newCreditVoucherDetail(
                executorUnit.getExecutorUnitCode(),
                costCenterCode,
                companyConfiguration.getWarehouseNationalCurrencyTransientAccount1(),
                voucherAmount,
                FinancesCurrencyType.P,
                BigDecimal.ONE));

        voucherService.create(voucherForGeneration);

    }

    private void createAccountEntryForExecutorUnitTransfer(WarehouseVoucher warehouseVoucher,
                                                           String[] gloss)
            throws CompanyConfigurationNotFoundException {

        CompanyConfiguration companyConfiguration = companyConfigurationService.findCompanyConfiguration();
        BigDecimal voucherAmount = movementDetailService.sumWarehouseVoucherMovementDetailAmount(warehouseVoucher.getId().getCompanyNumber(), warehouseVoucher.getState(), warehouseVoucher.getId().getTransactionNumber());

        // Source
        Voucher sourceVoucherForGeneration = VoucherBuilder.newGeneralVoucher(Constants.WAREHOUSE_VOUCHER_FORM, gloss[0]);
        sourceVoucherForGeneration.setUserNumber(companyConfiguration.getDefaultAccountancyUser().getId());
        sourceVoucherForGeneration.addVoucherDetail(VoucherDetailBuilder.newDebitVoucherDetail(
                warehouseVoucher.getExecutorUnit().getExecutorUnitCode(),
                warehouseVoucher.getCostCenterCode(),
                companyConfiguration.getWarehouseNationalCurrencyTransientAccount(),
                voucherAmount,
                FinancesCurrencyType.P,
                BigDecimal.ONE));
        sourceVoucherForGeneration.addVoucherDetail(VoucherDetailBuilder.newCreditVoucherDetail(
                warehouseVoucher.getExecutorUnit().getExecutorUnitCode(),
                warehouseVoucher.getCostCenterCode(),
                companyConfiguration.getWarehouseNationalCurrencyAccount(),
                voucherAmount,
                FinancesCurrencyType.P,
                BigDecimal.ONE));
        voucherService.create(sourceVoucherForGeneration);

        // Target
        Voucher targetVoucherForGeneration = VoucherBuilder.newGeneralVoucher(Constants.WAREHOUSE_VOUCHER_FORM, gloss[1]);
        targetVoucherForGeneration.setUserNumber(companyConfiguration.getDefaultAccountancyUser().getId());
        targetVoucherForGeneration.addVoucherDetail(VoucherDetailBuilder.newCreditVoucherDetail(
                warehouseVoucher.getTargetExecutorUnit().getExecutorUnitCode(),
                warehouseVoucher.getTargetCostCenterCode(),
                companyConfiguration.getWarehouseNationalCurrencyTransientAccount(),
                voucherAmount,
                FinancesCurrencyType.P,
                BigDecimal.ONE));
        targetVoucherForGeneration.addVoucherDetail(VoucherDetailBuilder.newDebitVoucherDetail(
                warehouseVoucher.getTargetExecutorUnit().getExecutorUnitCode(),
                warehouseVoucher.getTargetCostCenterCode(),
                companyConfiguration.getWarehouseNationalCurrencyAccount(),
                voucherAmount,
                FinancesCurrencyType.P,
                BigDecimal.ONE));

        voucherService.create(targetVoucherForGeneration);

    }

    private void createAccountEntryForInputs(WarehouseVoucher warehouseVoucher,
                                             BusinessUnit executorUnit,
                                             String costCenterCode,
                                             String gloss)
            throws CompanyConfigurationNotFoundException,
            FinancesCurrencyNotFoundException,
            FinancesExchangeRateNotFoundException {
        Voucher voucherForGeneration = VoucherBuilder.newGeneralVoucher(Constants.WAREHOUSE_VOUCHER_FORM, gloss);
        CompanyConfiguration companyConfiguration = companyConfigurationService.findCompanyConfiguration();
        List<MovementDetail> movementDetails = movementDetailService.findDetailByVoucherAndType(warehouseVoucher, MovementDetailType.E);

        BigDecimal total = BigDecimal.ZERO;
        for (MovementDetail movementDetail : movementDetails) {
            if (!movementDetail.getProductItem().getControlValued()) {
                log.debug("The movement detail with id=" +
                        movementDetail.getId() +
                        " was skipped because his product item does not have the controlValue field enabled.");
                continue;
            }

            if (BigDecimalUtil.isZeroOrNull(movementDetail.getAmount())) {
                log.debug("The movement detail with id=" +
                        movementDetail.getId() +
                        " was skipped because his amount its equal to zero.");
                continue;
            }

            BigDecimal detailAmount = BigDecimalUtil.roundBigDecimal(movementDetail.getAmount());
            total = BigDecimalUtil.sum(total, detailAmount);
            voucherForGeneration.addVoucherDetail(VoucherDetailBuilder.newCreditVoucherDetail(
                    executorUnit.getExecutorUnitCode(),
                    costCenterCode,
                    movementDetail.getProductItemCashAccount(),
                    detailAmount,
                    movementDetail.getProductItemCashAccount().getCurrency(),
                    financesExchangeRateService.getExchangeRateByCurrencyType(movementDetail.getProductItemCashAccount().getCurrency(), BigDecimal.ONE)));
        }

        if (BigDecimalUtil.isPositive(total)) {
            voucherForGeneration.setUserNumber(companyConfigurationService.findDefaultAccountancyUserNumber());
            voucherForGeneration.addVoucherDetail(VoucherDetailBuilder.newDebitVoucherDetail(
                    executorUnit.getExecutorUnitCode(),
                    costCenterCode,
                    companyConfiguration.getWarehouseNationalCurrencyAccount(),
                    total,
                    FinancesCurrencyType.P,
                    BigDecimal.ONE));

            voucherService.create(voucherForGeneration);
        }
    }

    private void createAccountEntryForOutputs(WarehouseVoucher warehouseVoucher,
                                              BusinessUnit executorUnit,
                                              String costCenterCode,
                                              String gloss)
            throws CompanyConfigurationNotFoundException,
            FinancesCurrencyNotFoundException,
            FinancesExchangeRateNotFoundException {
        Voucher voucherForGeneration = VoucherBuilder.newGeneralVoucher(Constants.WAREHOUSE_VOUCHER_FORM, gloss);
        CompanyConfiguration companyConfiguration = companyConfigurationService.findCompanyConfiguration();
        List<MovementDetail> movementDetails = movementDetailService.findDetailByVoucherAndType(warehouseVoucher, MovementDetailType.S);

        BigDecimal total = BigDecimal.ZERO;
        for (MovementDetail movementDetail : movementDetails) {
            if (!movementDetail.getProductItem().getControlValued()) {
                log.debug("The movement detail with id=" +
                        movementDetail.getId() +
                        " was skipped because his product item does not have the controlValue field enabled.");
                continue;
            }

            if (BigDecimalUtil.isZeroOrNull(movementDetail.getAmount())) {
                log.debug("The movement detail with id=" +
                        movementDetail.getId() +
                        " was skipped because his amount its equal to zero.");
                continue;
            }

            BigDecimal detailAmount = BigDecimalUtil.roundBigDecimal(movementDetail.getAmount());
            total = BigDecimalUtil.sum(total, detailAmount);
            voucherForGeneration.addVoucherDetail(VoucherDetailBuilder.newDebitVoucherDetail(
                    executorUnit.getExecutorUnitCode(),
                    costCenterCode,
                    movementDetail.getProductItemCashAccount(),
                    detailAmount,
                    movementDetail.getProductItemCashAccount().getCurrency(),
                    financesExchangeRateService.getExchangeRateByCurrencyType(movementDetail.getProductItemCashAccount().getCurrency(), BigDecimal.ONE)));
        }

        if (BigDecimalUtil.isPositive(total)) {
            voucherForGeneration.setUserNumber(companyConfigurationService.findDefaultAccountancyUserNumber());
            voucherForGeneration.addVoucherDetail(VoucherDetailBuilder.newCreditVoucherDetail(
                    executorUnit.getExecutorUnitCode(),
                    costCenterCode,
                    companyConfiguration.getWarehouseNationalCurrencyAccount(),
                    total,
                    FinancesCurrencyType.P,
                    BigDecimal.ONE));

            voucherService.create(voucherForGeneration);
        }
    }

    private void createAccountEntryForOutputsFromProductDelivery(WarehouseVoucher warehouseVoucher,
                                                                 BusinessUnit executorUnit,
                                                                 String costCenterCode,
                                                                 String gloss)
            throws CompanyConfigurationNotFoundException,
            FinancesCurrencyNotFoundException,
            FinancesExchangeRateNotFoundException {
        //todo:muy importante restablecer el cambio luego
        //Voucher voucherForGeneration = VoucherBuilder.newGeneralVoucher(Constants.ORDER_VOUCHER_FORM, gloss);
        Voucher voucherForGeneration = VoucherBuilder.newGeneralVoucher(Constants.ORDER_VOUCHER_FORM, gloss);
        CompanyConfiguration companyConfiguration = companyConfigurationService.findCompanyConfiguration();
        List<MovementDetail> movementDetails = movementDetailService.findDetailByVoucherAndType(warehouseVoucher, MovementDetailType.S);

        BigDecimal total = BigDecimal.ZERO;
        for (MovementDetail movementDetail : movementDetails) {
            if (!movementDetail.getProductItem().getControlValued()) {
                log.debug("The movement detail with id=" +
                        movementDetail.getId() +
                        " was skipped because his product item does not have the controlValue field enabled.");
                continue;
            }

            if (BigDecimalUtil.isZeroOrNull(movementDetail.getAmount())) {
                log.debug("The movement detail with id=" +
                        movementDetail.getId() +
                        " was skipped because his amount its equal to zero.");
                continue;
            }

            BigDecimal detailAmount = BigDecimalUtil.roundBigDecimal(movementDetail.getAmount());
            total = BigDecimalUtil.sum(total, detailAmount);
            voucherForGeneration.addVoucherDetail(VoucherDetailBuilder.newDebitVoucherDetail(
                    executorUnit.getExecutorUnitCode(),
                    costCenterCode,
                    movementDetail.getProductItemCashAccount(),
                    detailAmount,
                    movementDetail.getProductItemCashAccount().getCurrency(),
                    financesExchangeRateService.getExchangeRateByCurrencyType(movementDetail.getProductItemCashAccount().getCurrency(), BigDecimal.ONE)));
        }

        if (BigDecimalUtil.isPositive(total)) {

            System.out.println(">>>>>>>>>>>>>>>>>>>>> CUENTA CONTABLE: " + companyConfiguration.getWarehouseNationalCurrencyTransientAccount2().getFullName());
            voucherForGeneration.setUserNumber(companyConfigurationService.findDefaultAccountancyUserNumber());
            voucherForGeneration.addVoucherDetail(VoucherDetailBuilder.newCreditVoucherDetail(
                    executorUnit.getExecutorUnitCode(),
                    costCenterCode,
                    companyConfiguration.getWarehouseNationalCurrencyTransientAccount2(),
                    total,
                    FinancesCurrencyType.P,
                    BigDecimal.ONE));
            voucherForGeneration.setDate(warehouseVoucher.getDate());
            voucherService.create(voucherForGeneration);
        }
    }

    private void createAccountEntryForInputsAndContraAccount(WarehouseVoucher warehouseVoucher,
                                                             CashAccount contraAccount,
                                                             BusinessUnit executorUnit,
                                                             String costCenterCode,
                                                             String gloss)
            throws CompanyConfigurationNotFoundException,
            FinancesCurrencyNotFoundException,
            FinancesExchangeRateNotFoundException {

        CompanyConfiguration companyConfiguration = companyConfigurationService.findCompanyConfiguration();
        BigDecimal voucherAmount = movementDetailService.sumWarehouseVoucherMovementDetailAmount(warehouseVoucher.getId().getCompanyNumber(), warehouseVoucher.getState(), warehouseVoucher.getId().getTransactionNumber());

        Voucher voucherForGeneration = VoucherBuilder.newGeneralVoucher(Constants.WAREHOUSE_VOUCHER_FORM, gloss);
        voucherForGeneration.setUserNumber(companyConfigurationService.findDefaultAccountancyUserNumber());

        voucherForGeneration.addVoucherDetail(VoucherDetailBuilder.newDebitVoucherDetail(
                executorUnit.getExecutorUnitCode(),
                costCenterCode,
                companyConfiguration.getWarehouseNationalCurrencyAccount(),
                voucherAmount,
                FinancesCurrencyType.P,
                BigDecimal.ONE));

        voucherForGeneration.addVoucherDetail(VoucherDetailBuilder.newCreditVoucherDetail(
                executorUnit.getExecutorUnitCode(),
                costCenterCode,
                contraAccount,
                voucherAmount,
                contraAccount.getCurrency(),
                financesExchangeRateService.getExchangeRateByCurrencyType(contraAccount.getCurrency(), BigDecimal.ONE)));

        voucherService.create(voucherForGeneration);
    }

    private void createAccountEntryForOutputsAndContraAccount(WarehouseVoucher warehouseVoucher,
                                                              CashAccount contraAccount,
                                                              BusinessUnit executorUnit,
                                                              String costCenterCode,
                                                              String gloss)
            throws CompanyConfigurationNotFoundException,
            FinancesCurrencyNotFoundException,
            FinancesExchangeRateNotFoundException {

        CompanyConfiguration companyConfiguration = companyConfigurationService.findCompanyConfiguration();
        BigDecimal voucherAmount = movementDetailService.sumWarehouseVoucherMovementDetailAmount(warehouseVoucher.getId().getCompanyNumber(), warehouseVoucher.getState(), warehouseVoucher.getId().getTransactionNumber());

        Voucher voucherForGeneration = VoucherBuilder.newGeneralVoucher(Constants.WAREHOUSE_VOUCHER_FORM, gloss);
        voucherForGeneration.setUserNumber(companyConfigurationService.findDefaultAccountancyUserNumber());

        voucherForGeneration.addVoucherDetail(VoucherDetailBuilder.newDebitVoucherDetail(
                executorUnit.getExecutorUnitCode(),
                costCenterCode,
                contraAccount,
                voucherAmount,
                contraAccount.getCurrency(),
                financesExchangeRateService.getExchangeRateByCurrencyType(contraAccount.getCurrency(), BigDecimal.ONE)));

        voucherForGeneration.addVoucherDetail(VoucherDetailBuilder.newCreditVoucherDetail(
                executorUnit.getExecutorUnitCode(),
                costCenterCode,
                companyConfiguration.getWarehouseNationalCurrencyAccount(),
                voucherAmount,
                FinancesCurrencyType.P,
                BigDecimal.ONE));

        voucherService.create(voucherForGeneration);

    }

    @SuppressWarnings(value = "unchecked")
    private boolean existsControlValuedProductsItems(WarehouseVoucher warehouseVoucher) {
        List<MovementDetail> movementDetails = getEntityManager().createNamedQuery("MovementDetail.findByTransactionNumber").
                setParameter("companyNumber", warehouseVoucher.getId().getCompanyNumber()).
                setParameter("transactionNumber", warehouseVoucher.getId().getTransactionNumber()).getResultList();
        for (MovementDetail movementDetail : movementDetails) {
            if (movementDetail.getProductItem().getControlValued()) {
                return true;
            }
        }

        return false;
    }
}
