package com.encens.khipus.action.warehouse;

import com.encens.khipus.exception.ConcurrencyException;
import com.encens.khipus.exception.ReferentialIntegrityException;
import com.encens.khipus.exception.finances.CompanyConfigurationNotFoundException;
import com.encens.khipus.exception.finances.FinancesCurrencyNotFoundException;
import com.encens.khipus.exception.finances.FinancesExchangeRateNotFoundException;
import com.encens.khipus.exception.warehouse.*;
import com.encens.khipus.framework.action.Outcome;
import com.encens.khipus.interceptor.BusinessUnitRestrict;
import com.encens.khipus.interceptor.BusinessUnitRestriction;
import com.encens.khipus.model.warehouse.*;
import com.encens.khipus.service.warehouse.ApprovalWarehouseVoucherService;
import com.encens.khipus.service.warehouse.MovementDetailService;
import com.encens.khipus.util.Constants;
import com.encens.khipus.util.DateUtils;
import com.encens.khipus.util.MessageUtils;
import com.encens.khipus.util.query.QueryUtils;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.*;
import org.jboss.seam.annotations.security.Restrict;
import org.jboss.seam.international.StatusMessage;

import java.math.BigDecimal;

/**
 * @author
 * @version 3.0
 */
@Name("warehouseVoucherUpdateAction")
@Scope(ScopeType.CONVERSATION)
@AutoCreate
@BusinessUnitRestrict
public class WarehouseVoucherUpdateAction extends WarehouseVoucherGeneralAction {

    public static String APPROVED_OUTCOME = "Approved";


    @In
    private ApprovalWarehouseVoucherService approvalWarehouseVoucherService;

    @In
    private MovementDetailService movementDetailService;

    @Override
    @BusinessUnitRestriction(value = "#{warehouseVoucherUpdateAction.warehouseVoucher}", postValidation = true)
    @Begin(ifOutcome = Outcome.SUCCESS, flushMode = FlushModeType.MANUAL)
    @Restrict("#{s:hasPermission('WAREHOUSEVOUCHER','VIEW')}")
    public String select(WarehouseVoucher instance) {
        setOp(OP_UPDATE);
        try {
            readWarehouseVoucher(instance.getId());
        } catch (WarehouseVoucherNotFoundException e) {
            addNotFoundMessage();
            return Outcome.FAIL;
        }

        return Outcome.SUCCESS;
    }

    public void putWarehouseVoucher(WarehouseVoucherPK pk) {
        setOp(OP_UPDATE);
        try {
            readWarehouseVoucher(pk);
        } catch (WarehouseVoucherNotFoundException e) {
            //this exception never happens because this method is executed immediately after of create operation.
        }
    }

    @Override
    @BusinessUnitRestriction(value = "#{warehouseVoucherUpdateAction.warehouseVoucher}")
    @End
    @Restrict("#{s:hasPermission('WAREHOUSEVOUCHER','UPDATE')}")
    public String update() {
        String validationOutcome = validateInputFields();
        if (!Outcome.SUCCESS.equals(validationOutcome)) {
            return validationOutcome;
        }
        resetValidateQuantityMappings();

        try {
            for (MovementDetail movementDetail : inventoryMovement.getMovementDetailList()) {
                buildValidateQuantityMappings(movementDetail);
            }
            warehouseService.updateWarehouseVoucher(warehouseVoucher, inventoryMovement,
                    movementDetailUnderMinimalStockMap,
                    movementDetailOverMaximumStockMap,
                    movementDetailWithoutWarnings);
        } catch (ConcurrencyException e) {
            try {
                readWarehouseVoucher(warehouseVoucher.getId());
            } catch (WarehouseVoucherNotFoundException e1) {
                addNotFoundMessage();
                return Outcome.FAIL;
            }

            addUpdateConcurrencyMessage();
            return Outcome.REDISPLAY;
        } catch (WarehouseVoucherApprovedException e) {
            addWarehouseVoucherApprovedMessage();
            return APPROVED_OUTCOME;
        } catch (WarehouseVoucherNotFoundException e) {
            addNotFoundMessage();
            return Outcome.FAIL;
        } catch (ProductItemNotFoundException e) {
            addProductItemNotFoundMessage(e.getProductItem().getFullName());
            return Outcome.FAIL;
        }

        addUpdatedMessage();
        showMovementDetailWarningMessages();
        return Outcome.SUCCESS;
    }

    @BusinessUnitRestriction(value = "#{warehouseVoucherUpdateAction.warehouseVoucher}")
    @End
    @Restrict("#{s:hasPermission('WAREHOUSEVOUCHERAPPROVAL','VIEW')}")
    public String approve() {
        resetValidateQuantityMappings();
        try {
            for (MovementDetail movementDetail : inventoryMovement.getMovementDetailList()) {
                buildValidateQuantityMappings(movementDetail);
            }
            approvalWarehouseVoucherService.approveWarehouseVoucher(warehouseVoucher.getId(), getGlossMessage(),
                    movementDetailUnderMinimalStockMap,
                    movementDetailOverMaximumStockMap,
                    movementDetailWithoutWarnings);
            addWarehouseVoucherApproveMessage();
            showMovementDetailWarningMessages();
        } catch (InventoryException e) {
            addInventoryMessages(e.getInventoryMessages());
            return Outcome.REDISPLAY;
        } catch (WarehouseVoucherApprovedException e) {
            addWarehouseVoucherApprovedMessage();
            return APPROVED_OUTCOME;
        } catch (WarehouseVoucherEmptyException e) {
            addWarehouseVoucherEmptyException();
            return Outcome.REDISPLAY;
        } catch (WarehouseVoucherNotFoundException e) {
            addNotFoundMessage();
            return Outcome.FAIL;
        } catch (ProductItemAmountException e) {
            addNotEnoughAmountMessage(e.getProductItem(), e.getAvailableAmount());
            return Outcome.FAIL;
        } catch (InventoryUnitaryBalanceException e) {
            addInventoryUnitaryBalanceErrorMessage(e.getAvailableUnitaryBalance(), e.getProductItem());
            return Outcome.FAIL;
        } catch (InventoryProductItemNotFoundException e) {
            addInventoryProductItemNotFoundErrorMessage(e.getExecutorUnitCode(),
                    e.getProductItem(), e.getWarehouse());
            return Outcome.FAIL;
        } catch (CompanyConfigurationNotFoundException e) {
            addCompanyConfigurationNotFoundErrorMessage();
            return Outcome.FAIL;
        } catch (FinancesExchangeRateNotFoundException e) {
            addFinancesExchangeRateNotFoundExceptionMessage();
            return Outcome.FAIL;
        } catch (FinancesCurrencyNotFoundException e) {
            addFinancesExchangeRateNotFoundExceptionMessage();
            return Outcome.FAIL;
        } catch (ConcurrencyException e) {
            addUpdateConcurrencyMessage();
            return Outcome.FAIL;
        } catch (ReferentialIntegrityException e) {
            addDeleteReferentialIntegrityMessage();
            return Outcome.FAIL;
        } catch (ProductItemNotFoundException e) {
            addProductItemNotFoundMessage(e.getProductItem().getFullName());
            return Outcome.FAIL;
        } catch (WarehouseAccountCashNotFoundException e) {
            addWarehouseAccountCashNotFoundMessage();
            return Outcome.REDISPLAY;
        }

        return Outcome.SUCCESS;
    }

    @BusinessUnitRestriction(value = "#{warehouseVoucherUpdateAction.warehouseVoucher}")
    @End
    @Restrict("#{s:hasPermission('WAREHOUSEVOUCHERAPPROVAL','VIEW')}")
    public String approveFromCollection() {
        resetValidateQuantityMappings();
        try {
            for (MovementDetail movementDetail : inventoryMovement.getMovementDetailList()) {
                buildValidateQuantityMappings(movementDetail);
            }
            approvalWarehouseVoucherService.approveWarehouseVoucherFromCollection(warehouseVoucher.getId(), getGlossMessage(),
                    movementDetailUnderMinimalStockMap,
                    movementDetailOverMaximumStockMap,
                    movementDetailWithoutWarnings);
            addWarehouseVoucherApproveMessage();
            showMovementDetailWarningMessages();
        } catch (InventoryException e) {
            addInventoryMessages(e.getInventoryMessages());
            return Outcome.REDISPLAY;
        } catch (WarehouseVoucherApprovedException e) {
            addWarehouseVoucherApprovedMessage();
            return APPROVED_OUTCOME;
        } catch (WarehouseVoucherEmptyException e) {
            addWarehouseVoucherEmptyException();
            return Outcome.REDISPLAY;
        } catch (WarehouseVoucherNotFoundException e) {
            addNotFoundMessage();
            return Outcome.FAIL;
        } catch (ProductItemAmountException e) {
            addNotEnoughAmountMessage(e.getProductItem(), e.getAvailableAmount());
            return Outcome.FAIL;
        } catch (InventoryUnitaryBalanceException e) {
            addInventoryUnitaryBalanceErrorMessage(e.getAvailableUnitaryBalance(), e.getProductItem());
            return Outcome.FAIL;
        } catch (InventoryProductItemNotFoundException e) {
            addInventoryProductItemNotFoundErrorMessage(e.getExecutorUnitCode(),
                    e.getProductItem(), e.getWarehouse());
            return Outcome.FAIL;
        } catch (CompanyConfigurationNotFoundException e) {
            addCompanyConfigurationNotFoundErrorMessage();
            return Outcome.FAIL;
        } catch (FinancesExchangeRateNotFoundException e) {
            addFinancesExchangeRateNotFoundExceptionMessage();
            return Outcome.FAIL;
        } catch (FinancesCurrencyNotFoundException e) {
            addFinancesExchangeRateNotFoundExceptionMessage();
            return Outcome.FAIL;
        } catch (ConcurrencyException e) {
            addUpdateConcurrencyMessage();
            return Outcome.FAIL;
        } catch (ReferentialIntegrityException e) {
            addDeleteReferentialIntegrityMessage();
            return Outcome.FAIL;
        } catch (ProductItemNotFoundException e) {
            addProductItemNotFoundMessage(e.getProductItem().getFullName());
            return Outcome.FAIL;
        } catch (WarehouseAccountCashNotFoundException e) {
            addWarehouseAccountCashNotFoundMessage();
            return Outcome.FAIL;
        }

        return Outcome.SUCCESS;
    }

    @BusinessUnitRestriction(value = "#{warehouseVoucherUpdateAction.warehouseVoucher}")
    @Override
    @End
    @Restrict("#{s:hasPermission('WAREHOUSEVOUCHER','DELETE')}")
    public String delete() {
        try {
            warehouseService.deleteWarehouseVoucher(warehouseVoucher.getId());
            addDeletedMessage();
        } catch (WarehouseVoucherNotFoundException e) {
            addDeleteConcurrencyMessage();
        } catch (ReferentialIntegrityException e) {
            addDeleteReferentialIntegrityMessage();
        } catch (WarehouseVoucherApprovedException e) {
            addWarehouseVoucherApprovedMessage();
            return APPROVED_OUTCOME;
        }

        return Outcome.CANCEL;
    }


    public void readWarehouseVoucher(WarehouseVoucherPK id) throws WarehouseVoucherNotFoundException {
        setWarehouseVoucher(warehouseService.findWarehouseVoucher(id));

        InventoryMovementPK inventoryMovementPK =
                new InventoryMovementPK(warehouseVoucher.getId().getCompanyNumber(),
                        warehouseVoucher.getId().getTransactionNumber(),
                        warehouseVoucher.getState().name());

        setInventoryMovement(warehouseService.findInventoryMovement(inventoryMovementPK));
    }

    private String[] getGlossMessage() {
        String gloss[] = new String[2];
        String dateString = DateUtils.format(warehouseVoucher.getDate(), MessageUtils.getMessage("patterns.date"));
        String productCodes = QueryUtils.toQueryParameter(movementDetailService.findDetailProductCodeByVoucher(warehouseVoucher));
        String documentName = warehouseVoucher.getDocumentType().getName();
        String sourceWarehouseName = warehouseVoucher.getWarehouse().getName();
        String movementDescription = getInventoryMovement().getDescription();

        if (warehouseVoucher.isExecutorUnitTransfer()) {
            String targetWarehouseName = warehouseVoucher.getWarehouse().getName();
            gloss[0] = MessageUtils.getMessage("WarehouseVoucher.message.outTransferenceGloss", documentName, sourceWarehouseName, targetWarehouseName, productCodes, dateString, Constants.WAREHOUSEVOUCHER_NUMBER_PARAM, movementDescription);
            gloss[1] = MessageUtils.getMessage("WarehouseVoucher.message.inTransferenceGloss", documentName, sourceWarehouseName, targetWarehouseName, productCodes, dateString, Constants.WAREHOUSEVOUCHER_NUMBER_PARAM, movementDescription);
        } else {
            String voucherTypeName = messages.get(warehouseVoucher.getDocumentType().getWarehouseVoucherType().getResourceKey());
            gloss[0] = MessageUtils.getMessage("WarehouseVoucher.message.gloss", voucherTypeName, documentName, sourceWarehouseName, productCodes, dateString, Constants.WAREHOUSEVOUCHER_NUMBER_PARAM, movementDescription);
        }

        return gloss;

    }

    @Override
    protected void addNotFoundMessage() {
        facesMessages.addFromResourceBundle(StatusMessage.Severity.WARN,
                "WarehouseVoucher.error.notFound");
    }

    @Override
    protected void addUpdatedMessage() {
        facesMessages.addFromResourceBundle(StatusMessage.Severity.INFO,
                "WarehouseVoucher.message.updated");
    }

    public boolean isApproved() {
        return WarehouseVoucherState.APR.equals(warehouseVoucher.getState());
    }

    public boolean isPending() {
        return WarehouseVoucherState.PEN.equals(warehouseVoucher.getState());
    }

    public boolean isPartial() {
        return WarehouseVoucherState.PAR.equals(warehouseVoucher.getState());
    }

    @Override
    protected void addDeletedMessage() {
        facesMessages.addFromResourceBundle(StatusMessage.Severity.INFO,
                "WarehouseVoucher.message.deleted");
    }

    @Override
    protected void addDeleteConcurrencyMessage() {
        facesMessages.addFromResourceBundle(StatusMessage.Severity.WARN,
                "WarehouseVoucher.concurrency.delete");
    }

    @Override
    protected void addDeleteReferentialIntegrityMessage() {
        facesMessages.addFromResourceBundle(StatusMessage.Severity.WARN,
                "WarehouseVoucher.referentialIntegrity.delete");
    }

    protected void addNotEnoughAmountMessage(ProductItem productItem,
                                             BigDecimal availableAmount) {
        facesMessages.addFromResourceBundle(StatusMessage.Severity.ERROR,
                "WarehouseVoucher.approve.error.notEnoughAmount",
                productItem.getName(),
                availableAmount);
    }

    public void addWarehouseVoucherApprovedMessage() {
        facesMessages.addFromResourceBundle(StatusMessage.Severity.ERROR,
                "WarehouseVoucher.error.approved");
    }


    protected void addWarehouseVoucherEmptyException() {
        facesMessages.addFromResourceBundle(StatusMessage.Severity.ERROR,
                "WarehouseVoucher.error.empty");
    }

    private void addWarehouseVoucherApproveMessage() {
        facesMessages.addFromResourceBundle(StatusMessage.Severity.INFO,
                "WarehouseVoucher.message.approved");
    }


    protected void addInventoryUnitaryBalanceErrorMessage(BigDecimal availableUnitaryBalance,
                                                          ProductItem productItem) {
        facesMessages.addFromResourceBundle(StatusMessage.Severity.ERROR,
                "warehouseVoucher.approve.error.notEnoughUnitaryBalance",
                productItem.getName(),
                availableUnitaryBalance);
    }

    protected void addInventoryProductItemNotFoundErrorMessage(String executorUnitCode,
                                                               ProductItem productItem,
                                                               Warehouse warehouse) {
        facesMessages.addFromResourceBundle(StatusMessage.Severity.WARN,
                "warehouseVoucher.approve.error.productItemNotFound",
                productItem.getName(),
                warehouse.getName(),
                executorUnitCode);
    }

    protected void addFinancesExchangeRateNotFoundExceptionMessage() {
        facesMessages.addFromResourceBundle(StatusMessage.Severity.ERROR,
                "PurchaseOrder.financesExchangeRateNotFound");
    }

    private String validateInputFields() {
        String validationOutcome = Outcome.SUCCESS;
        if (warehouseVoucher.isExecutorUnitTransfer()) {
            validationOutcome = validateExecutorUnitTransferenceVoucher();
        }

        if (warehouseVoucher.isTransfer()) {
            validationOutcome = validateTransferenceVoucher();
        }

        if (warehouseVoucher.isDevolution() || warehouseVoucher.isInput() || warehouseVoucher.isReception()) {
            validationOutcome = validateReceptionVoucher();
        }

        if (warehouseVoucher.isConsumption() || warehouseVoucher.isOutput()) {
            validationOutcome = validateConsumptionVoucher();
        }

        if (warehouseVoucher.isOutput()) {
            validationOutcome = validateOutputVoucher();
        }

        return validationOutcome;
    }

    public boolean isEmptyWarehouseVoucher() {
        return warehouseService.isEmptyWarehouseVoucher(warehouseVoucher.getId());
    }

    public boolean isEnableContractInfo() {
        return warehouseVoucher.getPetitionerJobContract() != null;
    }

    public boolean isShowAddPartialWarehouseVoucherButton() {
        return (isPending() || isPartial()) && isReception()
                && (warehouseVoucher.getWarehouseVoucherReceptionType() == null
                || warehouseVoucher.getWarehouseVoucherReceptionType().equals(WarehouseVoucherReceptionType.RP));
    }

    /*messages*/
    public void addWarehouseVoucherStateChangedErrorMessage(WarehouseVoucherStateException e) {
        facesMessages.addFromResourceBundle(StatusMessage.Severity.ERROR,
                "WarehouseVoucher.error.annulled");
    }
}
