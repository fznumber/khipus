package com.encens.khipus.service.warehouse;

import com.encens.khipus.action.production.ProductionPlanningAction;
import com.encens.khipus.action.warehouse.WarehouseVoucherCreateAction;
import com.encens.khipus.action.warehouse.WarehouseVoucherGeneralAction;
import com.encens.khipus.action.warehouse.WarehouseVoucherUpdateAction;
import com.encens.khipus.exception.ConcurrencyException;
import com.encens.khipus.exception.EntryDuplicatedException;
import com.encens.khipus.exception.ReferentialIntegrityException;
import com.encens.khipus.exception.finances.CompanyConfigurationNotFoundException;
import com.encens.khipus.exception.finances.FinancesCurrencyNotFoundException;
import com.encens.khipus.exception.finances.FinancesExchangeRateNotFoundException;
import com.encens.khipus.exception.warehouse.*;
import com.encens.khipus.framework.service.GenericServiceBean;
import com.encens.khipus.interceptor.FinancesUser;
import com.encens.khipus.model.admin.BusinessUnit;
import com.encens.khipus.model.finances.CostCenter;
import com.encens.khipus.model.finances.CostCenterPk;
import com.encens.khipus.model.warehouse.*;
import com.encens.khipus.service.finances.FinancesUserService;
import com.encens.khipus.util.BigDecimalUtil;
import com.encens.khipus.util.Constants;
import com.encens.khipus.util.MessageUtils;
import com.encens.khipus.util.ValidatorUtil;
import com.encens.khipus.util.warehouse.InventoryMessage;
import com.encens.khipus.util.warehouse.WarehouseUtil;
import org.jboss.seam.Component;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.*;

/**
 * @author
 * @version 3.0
 */
@Stateless
@Name("approvalWarehouseVoucherService")
@FinancesUser
@AutoCreate
public class ApprovalWarehouseVoucherServiceBean extends GenericServiceBean implements ApprovalWarehouseVoucherService {

    @In(value = "#{listEntityManager}")
    private EntityManager listEm;

    @In
    private MonthProcessService monthProcessService;

    @In
    private WarehouseService warehouseService;

    @In
    private InventoryHistoryService inventoryHistoryService;

    @In
    private WarehouseAccountEntryService warehouseAccountEntryService;

    @In
    private FinancesUserService financesUserService;

    @In
    private MovementDetailService movementDetailService;

    @In
    private ProductItemService productItemService;

    @In
    protected Map<String, String> messages;

    public void approveWarehouseVoucher(WarehouseVoucherPK id, String[] gloss,
                                        Map<MovementDetail, BigDecimal> movementDetailUnderMinimalStockMap,
                                        Map<MovementDetail, BigDecimal> movementDetailOverMaximumStockMap,
                                        List<MovementDetail> movementDetailWithoutWarnings)
            throws InventoryException,
            WarehouseVoucherApprovedException,
            WarehouseVoucherNotFoundException,
            WarehouseVoucherEmptyException,
            ProductItemAmountException,
            InventoryUnitaryBalanceException,
            InventoryProductItemNotFoundException,
            CompanyConfigurationNotFoundException,
            FinancesCurrencyNotFoundException,
            FinancesExchangeRateNotFoundException,
            ConcurrencyException,
            ReferentialIntegrityException, ProductItemNotFoundException {

        String financeUserCode = financesUserService.getFinancesUserCode();

        if (!warehouseService.existsWarehouseVoucherInDataBase(id)) {
            throw new WarehouseVoucherNotFoundException("Cannot approve the warehouse voucher because" +
                    " it was deleted by other user.");
        }

        if (warehouseService.isEmptyWarehouseVoucher(id)) {
            throw new WarehouseVoucherEmptyException("Cannot approve the warehouse voucher because" +
                    " it not contains movement details.");
        }

        if (warehouseService.isWarehouseVoucherApproved(id)) {
            throw new WarehouseVoucherApprovedException("The WarehouseVoucher cannot be approved twice.");
        }

        WarehouseVoucher warehouseVoucher = getEntityManager().find(WarehouseVoucher.class, id);
        getEntityManager().refresh(warehouseVoucher);

        InventoryMovement pendantInventoryMovement = getPendantMovement(warehouseVoucher);
        getEntityManager().refresh(pendantInventoryMovement);

        if (warehouseVoucher.isTransfer() || warehouseVoucher.isExecutorUnitTransfer()) {
            validateOutputDetails(warehouseVoucher, warehouseVoucher.getWarehouse());
            updateWarehouseVoucher(warehouseVoucher);
            // creates a new approved inventory movement
            InventoryMovement approvedMovement = createApprovedInventoryMovement(pendantInventoryMovement, financeUserCode);

            approveOutputDetails(warehouseVoucher,
                    warehouseVoucher.getWarehouse(),
                    pendantInventoryMovement,
                    approvedMovement,
                    MovementDetailType.S);
            approveInputDetails(warehouseVoucher,
                    warehouseVoucher.getTargetWarehouse(),
                    pendantInventoryMovement,
                    approvedMovement,
                    MovementDetailType.E);
        } else {
            MovementDetailType movementDetailType = WarehouseUtil.getMovementTye(warehouseVoucher.getDocumentType());

            if (MovementDetailType.E.equals(movementDetailType)) {
                validateInputDetails(warehouseVoucher, warehouseVoucher.getWarehouse());
                updateWarehouseVoucher(warehouseVoucher);

                InventoryMovement approvedMovement = createApprovedInventoryMovement(pendantInventoryMovement, financeUserCode);

                approveInputDetails(warehouseVoucher,
                        warehouseVoucher.getWarehouse(),
                        pendantInventoryMovement, approvedMovement,
                        movementDetailType);
            }

            if (MovementDetailType.S.equals(movementDetailType)) {
                validateOutputDetails(warehouseVoucher, warehouseVoucher.getWarehouse());
                updateWarehouseVoucher(warehouseVoucher);

                InventoryMovement approvedMovement = createApprovedInventoryMovement(pendantInventoryMovement, financeUserCode);

                approveOutputDetails(warehouseVoucher,
                        warehouseVoucher.getWarehouse(),
                        pendantInventoryMovement,
                        approvedMovement,
                        movementDetailType);
            }
        }

        delete(pendantInventoryMovement);

        WarehouseVoucherCreateAction warehouseVoucherCreateAction = (WarehouseVoucherCreateAction) Component.getInstance("warehouseVoucherCreateAction");
        WarehouseVoucherUpdateAction warehouseVoucherUpdateAction = (WarehouseVoucherUpdateAction) Component.getInstance("warehouseVoucherUpdateAction");
        WarehouseVoucherGeneralAction warehouseVoucherGeneralAction = warehouseVoucherCreateAction != null ? warehouseVoucherCreateAction : warehouseVoucherUpdateAction;

        warehouseVoucherGeneralAction.resetValidateQuantityMappings();
        List<MovementDetail> inputMovementDetailList = movementDetailService.findDetailByVoucherAndType(warehouseVoucher, MovementDetailType.E);
        List<MovementDetail> outputMovementDetailList = movementDetailService.findDetailByVoucherAndType(warehouseVoucher, MovementDetailType.S);
        List<MovementDetail> approvedMovementDetailList = new ArrayList<MovementDetail>();
        approvedMovementDetailList.addAll(inputMovementDetailList);
        approvedMovementDetailList.addAll(outputMovementDetailList);
        for (MovementDetail movementDetail : approvedMovementDetailList) {
            warehouseVoucherGeneralAction.buildValidateQuantityMappings(movementDetail);
        }

        for (MovementDetail movementDetail : approvedMovementDetailList) {
            // update detail warnings
            warehouseService.fillMovementDetail(movementDetail, movementDetailUnderMinimalStockMap,
                    movementDetailOverMaximumStockMap,
                    movementDetailWithoutWarnings);
            try {
                update(movementDetail);
            } catch (EntryDuplicatedException e) {
                log.debug(e, "this won't happen because the attribute to update haven't a constraint");
            }
        }

        // Replace warehouse voucher number for current value
        gloss[0] = gloss[0].replaceAll(Constants.WAREHOUSEVOUCHER_NUMBER_PARAM, warehouseVoucher.getNumber());
        if (gloss[1] != null) {
            gloss[1] = gloss[1].replaceAll(Constants.WAREHOUSEVOUCHER_NUMBER_PARAM, warehouseVoucher.getNumber());
        }

        warehouseAccountEntryService.createAccountEntry(warehouseVoucher, gloss);

        updatePendantVoucherWarningContent(productItemService.findByWarehouseVoucher(warehouseVoucher));
    }

    public void crateAccountEntry(WarehouseVoucher warehouseVoucher,String[] gloss,List<ProductionPlanningAction.AccountOrderProduction> accountOrderProductions) throws CompanyConfigurationNotFoundException, FinancesCurrencyNotFoundException, FinancesExchangeRateNotFoundException
    {
        // Replace warehouse voucher number for current value
        gloss[0] = gloss[0].replaceAll(Constants.WAREHOUSEVOUCHER_NUMBER_PARAM, warehouseVoucher.getNumber());
        if (gloss[1] != null) {
            gloss[1] = gloss[1].replaceAll(Constants.WAREHOUSEVOUCHER_NUMBER_PARAM, warehouseVoucher.getNumber());
        }

        //todo: este servicio crea el asiento contable
        warehouseAccountEntryService.createAccountEntryForReceptionProductionOrder(warehouseVoucher, warehouseVoucher.getExecutorUnit(), warehouseVoucher.getCostCenterCode(), gloss[0], accountOrderProductions);

        updatePendantVoucherWarningContent(productItemService.findByWarehouseVoucher(warehouseVoucher));
    }

    public void approveWarehouseVoucherOrderProduction(WarehouseVoucherPK id,
                                                       Map<MovementDetail, BigDecimal> movementDetailUnderMinimalStockMap,
                                                       Map<MovementDetail, BigDecimal> movementDetailOverMaximumStockMap,
                                                       List<MovementDetail> movementDetailWithoutWarnings
                                                       )
            throws InventoryException,
            WarehouseVoucherApprovedException,
            WarehouseVoucherNotFoundException,
            WarehouseVoucherEmptyException,
            ProductItemAmountException,
            InventoryUnitaryBalanceException,
            InventoryProductItemNotFoundException,
            CompanyConfigurationNotFoundException,
            FinancesCurrencyNotFoundException,
            FinancesExchangeRateNotFoundException,
            ConcurrencyException,
            ReferentialIntegrityException, ProductItemNotFoundException {

        String financeUserCode = financesUserService.getFinancesUserCode();

        if (!warehouseService.existsWarehouseVoucherInDataBase(id)) {
            throw new WarehouseVoucherNotFoundException("Cannot approve the warehouse voucher because" +
                    " it was deleted by other user.");
        }

        if (warehouseService.isEmptyWarehouseVoucher(id)) {
            throw new WarehouseVoucherEmptyException("Cannot approve the warehouse voucher because" +
                    " it not contains movement details.");
        }

        if (warehouseService.isWarehouseVoucherApproved(id)) {
            throw new WarehouseVoucherApprovedException("The WarehouseVoucher cannot be approved twice.");
        }

        WarehouseVoucher warehouseVoucher = getEntityManager().find(WarehouseVoucher.class, id);
        getEntityManager().refresh(warehouseVoucher);

        InventoryMovement pendantInventoryMovement = getPendantMovement(warehouseVoucher);
        getEntityManager().refresh(pendantInventoryMovement);

        if (warehouseVoucher.isTransfer() || warehouseVoucher.isExecutorUnitTransfer()) {
            validateOutputDetails(warehouseVoucher, warehouseVoucher.getWarehouse());
            updateWarehouseVoucher(warehouseVoucher);
            // creates a new approved inventory movement
            InventoryMovement approvedMovement = createApprovedInventoryMovement(pendantInventoryMovement, financeUserCode);

            approveOutputDetails(warehouseVoucher,
                    warehouseVoucher.getWarehouse(),
                    pendantInventoryMovement,
                    approvedMovement,
                    MovementDetailType.S);
            approveInputDetails(warehouseVoucher,
                    warehouseVoucher.getTargetWarehouse(),
                    pendantInventoryMovement,
                    approvedMovement,
                    MovementDetailType.E);
        } else {
            MovementDetailType movementDetailType = WarehouseUtil.getMovementTye(warehouseVoucher.getDocumentType());

            if (MovementDetailType.E.equals(movementDetailType)) {
                validateInputDetails(warehouseVoucher, warehouseVoucher.getWarehouse());
                updateWarehouseVoucher(warehouseVoucher);

                InventoryMovement approvedMovement = createApprovedInventoryMovement(pendantInventoryMovement, financeUserCode);

                approveInputDetails(warehouseVoucher,
                        warehouseVoucher.getWarehouse(),
                        pendantInventoryMovement, approvedMovement,
                        movementDetailType);
            }

            if (MovementDetailType.S.equals(movementDetailType)) {
                validateOutputDetails(warehouseVoucher, warehouseVoucher.getWarehouse());
                updateWarehouseVoucher(warehouseVoucher);

                InventoryMovement approvedMovement = createApprovedInventoryMovement(pendantInventoryMovement, financeUserCode);

                approveOutputDetails(warehouseVoucher,
                        warehouseVoucher.getWarehouse(),
                        pendantInventoryMovement,
                        approvedMovement,
                        movementDetailType);
            }
        }

        delete(pendantInventoryMovement);

        WarehouseVoucherCreateAction warehouseVoucherCreateAction = (WarehouseVoucherCreateAction) Component.getInstance("warehouseVoucherCreateAction");
        WarehouseVoucherUpdateAction warehouseVoucherUpdateAction = (WarehouseVoucherUpdateAction) Component.getInstance("warehouseVoucherUpdateAction");
        WarehouseVoucherGeneralAction warehouseVoucherGeneralAction = warehouseVoucherCreateAction != null ? warehouseVoucherCreateAction : warehouseVoucherUpdateAction;

        warehouseVoucherGeneralAction.resetValidateQuantityMappings();
        List<MovementDetail> inputMovementDetailList = movementDetailService.findDetailByVoucherAndType(warehouseVoucher, MovementDetailType.E);
        List<MovementDetail> outputMovementDetailList = movementDetailService.findDetailByVoucherAndType(warehouseVoucher, MovementDetailType.S);
        List<MovementDetail> approvedMovementDetailList = new ArrayList<MovementDetail>();
        approvedMovementDetailList.addAll(inputMovementDetailList);
        approvedMovementDetailList.addAll(outputMovementDetailList);
        for (MovementDetail movementDetail : approvedMovementDetailList) {
            warehouseVoucherGeneralAction.buildValidateQuantityMappings(movementDetail);
        }

        for (MovementDetail movementDetail : approvedMovementDetailList) {
            // update detail warnings
            warehouseService.fillMovementDetail(movementDetail, movementDetailUnderMinimalStockMap,
                    movementDetailOverMaximumStockMap,
                    movementDetailWithoutWarnings);
            try {
                update(movementDetail);
            } catch (EntryDuplicatedException e) {
                log.debug(e, "this won't happen because the attribute to update haven't a constraint");
            }
        }

        updatePendantVoucherWarningContent(productItemService.findByWarehouseVoucher(warehouseVoucher));
    }

    @Override
    public void approveWarehouseVoucherOrderProduction(WarehouseVoucherPK id, String[] gloss,
                                        Map<MovementDetail, BigDecimal> movementDetailUnderMinimalStockMap,
                                        Map<MovementDetail, BigDecimal> movementDetailOverMaximumStockMap,
                                        List<MovementDetail> movementDetailWithoutWarnings,
                                        List<ProductionPlanningAction.AccountOrderProduction> accountOrderProductions)
            throws InventoryException,
            WarehouseVoucherApprovedException,
            WarehouseVoucherNotFoundException,
            WarehouseVoucherEmptyException,
            ProductItemAmountException,
            InventoryUnitaryBalanceException,
            InventoryProductItemNotFoundException,
            CompanyConfigurationNotFoundException,
            FinancesCurrencyNotFoundException,
            FinancesExchangeRateNotFoundException,
            ConcurrencyException,
            ReferentialIntegrityException, ProductItemNotFoundException {

        String financeUserCode = financesUserService.getFinancesUserCode();

        if (!warehouseService.existsWarehouseVoucherInDataBase(id)) {
            throw new WarehouseVoucherNotFoundException("Cannot approve the warehouse voucher because" +
                    " it was deleted by other user.");
        }

        if (warehouseService.isEmptyWarehouseVoucher(id)) {
            throw new WarehouseVoucherEmptyException("Cannot approve the warehouse voucher because" +
                    " it not contains movement details.");
        }

        if (warehouseService.isWarehouseVoucherApproved(id)) {
            throw new WarehouseVoucherApprovedException("The WarehouseVoucher cannot be approved twice.");
        }

        WarehouseVoucher warehouseVoucher = getEntityManager().find(WarehouseVoucher.class, id);
        getEntityManager().refresh(warehouseVoucher);

        InventoryMovement pendantInventoryMovement = getPendantMovement(warehouseVoucher);
        getEntityManager().refresh(pendantInventoryMovement);

        if (warehouseVoucher.isTransfer() || warehouseVoucher.isExecutorUnitTransfer()) {
            validateOutputDetails(warehouseVoucher, warehouseVoucher.getWarehouse());
            updateWarehouseVoucher(warehouseVoucher);
            // creates a new approved inventory movement
            InventoryMovement approvedMovement = createApprovedInventoryMovement(pendantInventoryMovement, financeUserCode);

            approveOutputDetails(warehouseVoucher,
                    warehouseVoucher.getWarehouse(),
                    pendantInventoryMovement,
                    approvedMovement,
                    MovementDetailType.S);
            approveInputDetails(warehouseVoucher,
                    warehouseVoucher.getTargetWarehouse(),
                    pendantInventoryMovement,
                    approvedMovement,
                    MovementDetailType.E);
        } else {
            MovementDetailType movementDetailType = WarehouseUtil.getMovementTye(warehouseVoucher.getDocumentType());

            if (MovementDetailType.E.equals(movementDetailType)) {
                validateInputDetails(warehouseVoucher, warehouseVoucher.getWarehouse());
                updateWarehouseVoucher(warehouseVoucher);

                InventoryMovement approvedMovement = createApprovedInventoryMovement(pendantInventoryMovement, financeUserCode);

                approveInputDetails(warehouseVoucher,
                        warehouseVoucher.getWarehouse(),
                        pendantInventoryMovement, approvedMovement,
                        movementDetailType);
            }

            if (MovementDetailType.S.equals(movementDetailType)) {
                validateOutputDetails(warehouseVoucher, warehouseVoucher.getWarehouse());
                updateWarehouseVoucher(warehouseVoucher);

                InventoryMovement approvedMovement = createApprovedInventoryMovement(pendantInventoryMovement, financeUserCode);

                approveOutputDetails(warehouseVoucher,
                        warehouseVoucher.getWarehouse(),
                        pendantInventoryMovement,
                        approvedMovement,
                        movementDetailType);
            }
        }

        delete(pendantInventoryMovement);

        WarehouseVoucherCreateAction warehouseVoucherCreateAction = (WarehouseVoucherCreateAction) Component.getInstance("warehouseVoucherCreateAction");
        WarehouseVoucherUpdateAction warehouseVoucherUpdateAction = (WarehouseVoucherUpdateAction) Component.getInstance("warehouseVoucherUpdateAction");
        WarehouseVoucherGeneralAction warehouseVoucherGeneralAction = warehouseVoucherCreateAction != null ? warehouseVoucherCreateAction : warehouseVoucherUpdateAction;

        warehouseVoucherGeneralAction.resetValidateQuantityMappings();
        List<MovementDetail> inputMovementDetailList = movementDetailService.findDetailByVoucherAndType(warehouseVoucher, MovementDetailType.E);
        List<MovementDetail> outputMovementDetailList = movementDetailService.findDetailByVoucherAndType(warehouseVoucher, MovementDetailType.S);
        List<MovementDetail> approvedMovementDetailList = new ArrayList<MovementDetail>();
        approvedMovementDetailList.addAll(inputMovementDetailList);
        approvedMovementDetailList.addAll(outputMovementDetailList);
        for (MovementDetail movementDetail : approvedMovementDetailList) {
            warehouseVoucherGeneralAction.buildValidateQuantityMappings(movementDetail);
        }

        for (MovementDetail movementDetail : approvedMovementDetailList) {
            // update detail warnings
            warehouseService.fillMovementDetail(movementDetail, movementDetailUnderMinimalStockMap,
                    movementDetailOverMaximumStockMap,
                    movementDetailWithoutWarnings);
            try {
                update(movementDetail);
            } catch (EntryDuplicatedException e) {
                log.debug(e, "this won't happen because the attribute to update haven't a constraint");
            }
        }

        // Replace warehouse voucher number for current value
        gloss[0] = gloss[0].replaceAll(Constants.WAREHOUSEVOUCHER_NUMBER_PARAM, warehouseVoucher.getNumber());
        if (gloss[1] != null) {
            gloss[1] = gloss[1].replaceAll(Constants.WAREHOUSEVOUCHER_NUMBER_PARAM, warehouseVoucher.getNumber());
        }

       //todo: este servicio crea el asiento contable
        warehouseAccountEntryService.createAccountEntryForReceptionProductionOrder(warehouseVoucher,warehouseVoucher.getExecutorUnit(),warehouseVoucher.getCostCenterCode(),gloss[0],accountOrderProductions);

        updatePendantVoucherWarningContent(productItemService.findByWarehouseVoucher(warehouseVoucher));
    }

    public void approveWarehouseVoucherFromDeliveryProduct(WarehouseVoucherPK id, String[] gloss,
                                                           Map<MovementDetail, BigDecimal> movementDetailUnderMinimalStockMap,
                                                           Map<MovementDetail, BigDecimal> movementDetailOverMaximumStockMap,
                                                           List<MovementDetail> movementDetailWithoutWarnings)
            throws InventoryException,
            WarehouseVoucherApprovedException,
            WarehouseVoucherNotFoundException,
            WarehouseVoucherEmptyException,
            ProductItemAmountException,
            InventoryUnitaryBalanceException,
            InventoryProductItemNotFoundException,
            CompanyConfigurationNotFoundException,
            FinancesCurrencyNotFoundException,
            FinancesExchangeRateNotFoundException,
            ConcurrencyException,
            ReferentialIntegrityException, ProductItemNotFoundException {

        String financeUserCode = financesUserService.getFinancesUserCode();

        if (!warehouseService.existsWarehouseVoucherInDataBase(id)) {
            throw new WarehouseVoucherNotFoundException("Cannot approve the warehouse voucher because it was deleted by other user.");
        }

        if (warehouseService.isEmptyWarehouseVoucher(id)) {
            throw new WarehouseVoucherEmptyException("Cannot approve the warehouse voucher because it not contains movement details.");
        }

        if (warehouseService.isWarehouseVoucherApproved(id)) {
            throw new WarehouseVoucherApprovedException("The WarehouseVoucher cannot be approved twice.");
        }

        WarehouseVoucher warehouseVoucher = getEntityManager().find(WarehouseVoucher.class, id);
        getEntityManager().refresh(warehouseVoucher);

        InventoryMovement pendantInventoryMovement = getPendantMovement(warehouseVoucher);
        getEntityManager().refresh(pendantInventoryMovement);

        if (warehouseVoucher.isTransfer() || warehouseVoucher.isExecutorUnitTransfer()) {
            validateOutputDetails(warehouseVoucher, warehouseVoucher.getWarehouse());
            updateWarehouseVoucher(warehouseVoucher);
            // creates a new approved inventory movement
            InventoryMovement approvedMovement = createApprovedInventoryMovement(pendantInventoryMovement, financeUserCode);

            approveOutputDetails(warehouseVoucher,
                    warehouseVoucher.getWarehouse(),
                    pendantInventoryMovement,
                    approvedMovement,
                    MovementDetailType.S);
            approveInputDetails(warehouseVoucher,
                    warehouseVoucher.getTargetWarehouse(),
                    pendantInventoryMovement,
                    approvedMovement,
                    MovementDetailType.E);
        } else {
            MovementDetailType movementDetailType = WarehouseUtil.getMovementTye(warehouseVoucher.getDocumentType());

            if (MovementDetailType.E.equals(movementDetailType)) {
                validateInputDetails(warehouseVoucher, warehouseVoucher.getWarehouse());
                updateWarehouseVoucher(warehouseVoucher);

                InventoryMovement approvedMovement = createApprovedInventoryMovement(pendantInventoryMovement, financeUserCode);

                approveInputDetails(warehouseVoucher,
                        warehouseVoucher.getWarehouse(),
                        pendantInventoryMovement, approvedMovement,
                        movementDetailType);
            }

            if (MovementDetailType.S.equals(movementDetailType)) {
                validateOutputDetails(warehouseVoucher, warehouseVoucher.getWarehouse());
                updateWarehouseVoucher(warehouseVoucher);

                InventoryMovement approvedMovement = createApprovedInventoryMovement(pendantInventoryMovement, financeUserCode);

                approveOutputDetails(warehouseVoucher,
                        warehouseVoucher.getWarehouse(),
                        pendantInventoryMovement,
                        approvedMovement,
                        movementDetailType);
            }
        }

        delete(pendantInventoryMovement);

        WarehouseVoucherCreateAction warehouseVoucherCreateAction = (WarehouseVoucherCreateAction) Component.getInstance("warehouseVoucherCreateAction");
        WarehouseVoucherUpdateAction warehouseVoucherUpdateAction = (WarehouseVoucherUpdateAction) Component.getInstance("warehouseVoucherUpdateAction");
        WarehouseVoucherGeneralAction warehouseVoucherGeneralAction = warehouseVoucherCreateAction != null ? warehouseVoucherCreateAction : warehouseVoucherUpdateAction;

        warehouseVoucherGeneralAction.resetValidateQuantityMappings();
        List<MovementDetail> inputMovementDetailList = movementDetailService.findDetailByVoucherAndType(warehouseVoucher, MovementDetailType.E);
        List<MovementDetail> outputMovementDetailList = movementDetailService.findDetailByVoucherAndType(warehouseVoucher, MovementDetailType.S);
        List<MovementDetail> approvedMovementDetailList = new ArrayList<MovementDetail>();
        approvedMovementDetailList.addAll(inputMovementDetailList);
        approvedMovementDetailList.addAll(outputMovementDetailList);
        for (MovementDetail movementDetail : approvedMovementDetailList) {
            warehouseVoucherGeneralAction.buildValidateQuantityMappings(movementDetail);
        }

        for (MovementDetail movementDetail : approvedMovementDetailList) {
            // update detail warnings
            warehouseService.fillMovementDetail(movementDetail, movementDetailUnderMinimalStockMap,
                    movementDetailOverMaximumStockMap,
                    movementDetailWithoutWarnings);
            try {
                update(movementDetail);
            } catch (EntryDuplicatedException e) {
                log.debug(e, "this won't happen because the attribute to update haven't a constraint");
            }
        }

        // Replace warehouse voucher number for current value
        gloss[0] = gloss[0].replaceAll(Constants.WAREHOUSEVOUCHER_NUMBER_PARAM, warehouseVoucher.getNumber());
        if (gloss[1] != null) {
            gloss[1] = gloss[1].replaceAll(Constants.WAREHOUSEVOUCHER_NUMBER_PARAM, warehouseVoucher.getNumber());
        }

        warehouseAccountEntryService.createAccountEntryFromProductDelivery(warehouseVoucher, gloss); //Change by createAccountEntryFromProductDelivery

        updatePendantVoucherWarningContent(productItemService.findByWarehouseVoucher(warehouseVoucher));
    }

    public void approveWarehouseVoucherFromCollection(WarehouseVoucherPK id, String[] gloss,
                                                      Map<MovementDetail, BigDecimal> movementDetailUnderMinimalStockMap,
                                                      Map<MovementDetail, BigDecimal> movementDetailOverMaximumStockMap,
                                                      List<MovementDetail> movementDetailWithoutWarnings)
            throws InventoryException,
            WarehouseVoucherApprovedException,
            WarehouseVoucherNotFoundException,
            WarehouseVoucherEmptyException,
            ProductItemAmountException,
            InventoryUnitaryBalanceException,
            InventoryProductItemNotFoundException,
            CompanyConfigurationNotFoundException,
            FinancesCurrencyNotFoundException,
            FinancesExchangeRateNotFoundException,
            ConcurrencyException,
            ReferentialIntegrityException, ProductItemNotFoundException {

        String financeUserCode = financesUserService.getFinancesUserCode();

        if (!warehouseService.existsWarehouseVoucherInDataBase(id)) {
            throw new WarehouseVoucherNotFoundException("Cannot approve the warehouse voucher because" +
                    " it was deleted by other user.");
        }

        if (warehouseService.isEmptyWarehouseVoucher(id)) {
            throw new WarehouseVoucherEmptyException("Cannot approve the warehouse voucher because" +
                    " it not contains movement details.");
        }

        if (warehouseService.isWarehouseVoucherApproved(id)) {
            throw new WarehouseVoucherApprovedException("The WarehouseVoucher cannot be approved twice.");
        }

        WarehouseVoucher warehouseVoucher = getEntityManager().find(WarehouseVoucher.class, id);
        getEntityManager().refresh(warehouseVoucher);

        InventoryMovement pendantInventoryMovement = getPendantMovement(warehouseVoucher);
        getEntityManager().refresh(pendantInventoryMovement);

        if (warehouseVoucher.isTransfer() || warehouseVoucher.isExecutorUnitTransfer()) {
            validateOutputDetails(warehouseVoucher, warehouseVoucher.getWarehouse());
            updateWarehouseVoucher(warehouseVoucher);
            // creates a new approved inventory movement
            InventoryMovement approvedMovement = createApprovedInventoryMovement(pendantInventoryMovement, financeUserCode);

            approveOutputDetails(warehouseVoucher,
                    warehouseVoucher.getWarehouse(),
                    pendantInventoryMovement,
                    approvedMovement,
                    MovementDetailType.S);
            approveInputDetails(warehouseVoucher,
                    warehouseVoucher.getTargetWarehouse(),
                    pendantInventoryMovement,
                    approvedMovement,
                    MovementDetailType.E);
        } else {
            MovementDetailType movementDetailType = WarehouseUtil.getMovementTye(warehouseVoucher.getDocumentType());

            if (MovementDetailType.E.equals(movementDetailType)) {
                validateInputDetails(warehouseVoucher, warehouseVoucher.getWarehouse());
                updateWarehouseVoucher(warehouseVoucher);

                InventoryMovement approvedMovement = createApprovedInventoryMovement(pendantInventoryMovement, financeUserCode);

                approveInputDetails(warehouseVoucher,
                        warehouseVoucher.getWarehouse(),
                        pendantInventoryMovement, approvedMovement,
                        movementDetailType);
            }

            if (MovementDetailType.S.equals(movementDetailType)) {
                validateOutputDetails(warehouseVoucher, warehouseVoucher.getWarehouse());
                updateWarehouseVoucher(warehouseVoucher);

                InventoryMovement approvedMovement = createApprovedInventoryMovement(pendantInventoryMovement, financeUserCode);

                approveOutputDetails(warehouseVoucher,
                        warehouseVoucher.getWarehouse(),
                        pendantInventoryMovement,
                        approvedMovement,
                        movementDetailType);
            }
        }

        delete(pendantInventoryMovement);

        WarehouseVoucherCreateAction warehouseVoucherCreateAction = (WarehouseVoucherCreateAction) Component.getInstance("warehouseVoucherCreateAction");
        WarehouseVoucherUpdateAction warehouseVoucherUpdateAction = (WarehouseVoucherUpdateAction) Component.getInstance("warehouseVoucherUpdateAction");
        WarehouseVoucherGeneralAction warehouseVoucherGeneralAction = warehouseVoucherCreateAction != null ? warehouseVoucherCreateAction : warehouseVoucherUpdateAction;

        warehouseVoucherGeneralAction.resetValidateQuantityMappings();
        List<MovementDetail> inputMovementDetailList = movementDetailService.findDetailByVoucherAndType(warehouseVoucher, MovementDetailType.E);
        List<MovementDetail> outputMovementDetailList = movementDetailService.findDetailByVoucherAndType(warehouseVoucher, MovementDetailType.S);
        List<MovementDetail> approvedMovementDetailList = new ArrayList<MovementDetail>();
        approvedMovementDetailList.addAll(inputMovementDetailList);
        approvedMovementDetailList.addAll(outputMovementDetailList);
        for (MovementDetail movementDetail : approvedMovementDetailList) {
            warehouseVoucherGeneralAction.buildValidateQuantityMappings(movementDetail);
        }

        for (MovementDetail movementDetail : approvedMovementDetailList) {
            // update detail warnings
            warehouseService.fillMovementDetail(movementDetail, movementDetailUnderMinimalStockMap,
                    movementDetailOverMaximumStockMap,
                    movementDetailWithoutWarnings);
            try {
                update(movementDetail);
            } catch (EntryDuplicatedException e) {
                log.debug(e, "this won't happen because the attribute to update haven't a constraint");
            }
        }

        // Replace warehouse voucher number for current value
        gloss[0] = gloss[0].replaceAll(Constants.WAREHOUSEVOUCHER_NUMBER_PARAM, warehouseVoucher.getNumber());
        if (gloss[1] != null) {
            gloss[1] = gloss[1].replaceAll(Constants.WAREHOUSEVOUCHER_NUMBER_PARAM, warehouseVoucher.getNumber());
        }

        warehouseAccountEntryService.createAccountEntryFromCollection(warehouseVoucher, gloss);

        updatePendantVoucherWarningContent(productItemService.findByWarehouseVoucher(warehouseVoucher));
    }

    // change the state to partial to a parent WarehouseVoucher
    public void approvePartialInputParentWarehouseVoucher(WarehouseVoucherPK id)
            throws InventoryException,
            WarehouseVoucherApprovedException,
            WarehouseVoucherNotFoundException,
            WarehouseVoucherEmptyException,
            ProductItemAmountException,
            InventoryUnitaryBalanceException,
            InventoryProductItemNotFoundException,
            CompanyConfigurationNotFoundException,
            FinancesCurrencyNotFoundException,
            FinancesExchangeRateNotFoundException,
            ConcurrencyException,
            ReferentialIntegrityException, ProductItemNotFoundException, MovementDetailTypeException, WarehouseVoucherStateException {

        String financeUserCode = financesUserService.getFinancesUserCode();

        if (!warehouseService.existsWarehouseVoucherInDataBase(id)) {
            throw new WarehouseVoucherNotFoundException("Cannot approve the warehouse voucher because" +
                    " it was deleted by other user.");
        }

        if (warehouseService.isEmptyWarehouseVoucher(id)) {
            throw new WarehouseVoucherEmptyException("Cannot approve the warehouse voucher because" +
                    " it not contains movement details.");
        }

        if (warehouseService.isWarehouseVoucherPendant(id)) {
            throw new WarehouseVoucherStateException(warehouseService.findWarehouseVoucher(id).getState());
        }

        WarehouseVoucher warehouseVoucher = getEntityManager().find(WarehouseVoucher.class, id);
        getEntityManager().refresh(warehouseVoucher);

        InventoryMovement pendantInventoryMovement = getPendantMovement(warehouseVoucher);
        getEntityManager().refresh(pendantInventoryMovement);

        MovementDetailType movementDetailType = WarehouseUtil.getMovementTye(warehouseVoucher.getDocumentType());

        if (MovementDetailType.E.equals(movementDetailType)) {
            validateInputDetails(warehouseVoucher, warehouseVoucher.getWarehouse());
            updateWarehouseVoucherByState(warehouseVoucher, WarehouseVoucherState.PAR);

            InventoryMovement partialMovement = createPartialInventoryMovement(pendantInventoryMovement, financeUserCode);

            updateParentInputDetails(warehouseVoucher,
                    pendantInventoryMovement, partialMovement,
                    movementDetailType);
        } else {
            throw new MovementDetailTypeException(MovementDetailType.E);
        }

        delete(pendantInventoryMovement);
    }

    @Override
    public WarehouseVoucher findWarehouseVoucherByNumber(String number) {
        WarehouseVoucher warehouse = (WarehouseVoucher) getEntityManager().createNamedQuery("WarehouseVoucher.findByNumber")
                .setParameter("number", number)
                .getSingleResult();
        return warehouse;
    }

    public void validateOutputQuantity(BigDecimal outputQuantity,
                                       Warehouse warehouse,
                                       ProductItem productItem,
                                       CostCenter costCenter) throws InventoryException {
        Inventory inventory = getInventory(warehouse, productItem);
        if (null == inventory) {
            throw new InventoryException(
                    Arrays.asList(new InventoryMessage(productItem, true))
            );
        }

        InventoryDetail inventoryDetail = getInventoryDetail(inventory,
                warehouse.getExecutorUnit(),
                costCenter.getId().getCode());

        if (null == inventoryDetail || BigDecimalUtil.isZeroOrNull(inventoryDetail.getQuantity())) {
            inventoryDetail = getInventoryDetailWithPublicCostCenter(inventory,
                    warehouse.getExecutorUnit());
            if (null == inventoryDetail) {
                throw new InventoryException(
                        Arrays.asList(new InventoryMessage(productItem, true))
                );
            }
        }

        BigDecimal availableQuantity = inventoryDetail.getQuantity();

        if (outputQuantity.compareTo(availableQuantity) == 1) {
            throw new InventoryException(
                    Arrays.asList(new InventoryMessage(productItem, true, availableQuantity))
            );
        }
    }

    public void validateOutputMovementDetail(WarehouseVoucher warehouseVoucher,
                                             Warehouse warehouse,
                                             MovementDetail movementDetail,
                                             boolean isUpdate) throws InventoryException {

        Inventory inventory = getInventory(warehouse, movementDetail.getProductItem());
        if (null == inventory) {
            throw new InventoryException(
                    Arrays.asList(new InventoryMessage(movementDetail.getProductItem(), true))
            );
        }

        InventoryDetail inventoryDetail = getInventoryDetail(inventory,
                warehouseVoucher.getExecutorUnit(),
                warehouseVoucher.getCostCenterCode());
        if (null == inventoryDetail || BigDecimalUtil.isZeroOrNull(inventoryDetail.getQuantity())) {
            inventoryDetail = getInventoryDetailWithPublicCostCenter(inventory,
                    warehouseVoucher.getExecutorUnit());
            if (null == inventoryDetail) {
                throw new InventoryException(
                        Arrays.asList(new InventoryMessage(movementDetail.getProductItem(), true))
                );
            }
        }

        BigDecimal totalUsages = (BigDecimal) getEntityManager().
                createNamedQuery("MovementDetail.sumQuantitiesByProductItem").
                setParameter("companyNumber", warehouseVoucher.getId().getCompanyNumber()).
                setParameter("transactionNumber", warehouseVoucher.getId().getTransactionNumber()).
                setParameter("state", warehouseVoucher.getState()).
                setParameter("productItem", movementDetail.getProductItem()).
                setParameter("warehouseCode", warehouseVoucher.getWarehouse().getId().getWarehouseCode()).
                setParameter("movementDetailType", MovementDetailType.S).getSingleResult();

        if (null == totalUsages) {
            totalUsages = BigDecimal.ZERO;
        }

        if (isUpdate) {
            MovementDetail dbMovementDetail = listEm.find(MovementDetail.class, movementDetail.getId());
            if (null == dbMovementDetail) {
                return;
            }

            BigDecimal actualQuantity = dbMovementDetail.getQuantity();

            totalUsages = BigDecimalUtil.subtract(totalUsages, actualQuantity);
        }

        BigDecimal availableQuantity = inventoryDetail.getQuantity();
        BigDecimal requiredQuantity = BigDecimalUtil.sum(totalUsages, movementDetail.getQuantity());

        if (requiredQuantity.compareTo(availableQuantity) == 1) {
            throw new InventoryException(
                    Arrays.asList(new InventoryMessage(movementDetail.getProductItem(), true, availableQuantity))
            );
        }
    }


    private void approveOutputDetails(WarehouseVoucher warehouseVoucher,
                                      Warehouse warehouse,
                                      InventoryMovement pendantInventoryMovement,
                                      InventoryMovement approvedMovement,
                                      MovementDetailType movementDetailType)
            throws ProductItemAmountException, InventoryUnitaryBalanceException, InventoryProductItemNotFoundException {

        List<MovementDetail> pendantDetails = getPendantDetails(pendantInventoryMovement, movementDetailType);

        for (MovementDetail movementDetail : pendantDetails) {
            movementDetail.setState(warehouseVoucher.getState());
            movementDetail.setMovementDetailDate(approvedMovement.getMovementDate());
            removeFromInventory(warehouseVoucher, warehouse, movementDetail);
            inventoryHistoryService.updateInventoryHistory(movementDetail);
            updateProductItemInformationForOutputs(warehouseVoucher, movementDetail);
            getEntityManager().merge(movementDetail);
            getEntityManager().flush();

            getEntityManager().refresh(movementDetail);
        }
    }


    private void approveInputDetails(WarehouseVoucher warehouseVoucher,
                                     Warehouse warehouse,
                                     InventoryMovement pendantMovement,
                                     InventoryMovement approvedMovement,
                                     MovementDetailType movementDetailType) {
        List<MovementDetail> pendantDetails = getPendantDetails(pendantMovement, movementDetailType);

        for (MovementDetail movementDetail : pendantDetails) {
            getEntityManager().refresh(movementDetail);
            movementDetail.setState(warehouseVoucher.getState());
            movementDetail.setMovementDetailDate(approvedMovement.getMovementDate());
            if (!getEntityManager().contains(movementDetail)) {
                getEntityManager().merge(movementDetail);
            }
            getEntityManager().flush();
            addAtInventory(warehouseVoucher, warehouse, movementDetail);
            inventoryHistoryService.updateInventoryHistory(movementDetail);
            updateProductItemInformationForInputs(warehouseVoucher, movementDetail);
            getEntityManager().flush();

            getEntityManager().refresh(movementDetail);
        }
    }

    // approves a parents voucher details
    private void updateParentInputDetails(WarehouseVoucher warehouseVoucher,
                                          InventoryMovement pendantMovement,
                                          InventoryMovement approvedMovement,
                                          MovementDetailType movementDetailType) {
        List<MovementDetail> pendantDetails = getPendantDetails(pendantMovement, movementDetailType);

        for (MovementDetail movementDetail : pendantDetails) {
            movementDetail.setState(warehouseVoucher.getState());
            movementDetail.setMovementDetailDate(approvedMovement.getMovementDate());
            getEntityManager().merge(movementDetail);
            getEntityManager().flush();
            getEntityManager().refresh(movementDetail);
        }
    }

    private void updateProductItemInformationForInputs(WarehouseVoucher warehouseVoucher, MovementDetail movementDetail) {
        ProductItem productItem = getEntityManager().find(ProductItem.class, movementDetail.getProductItem().getId());

        BigDecimal sumUnitaryBalances = (BigDecimal) getEntityManager().
                createNamedQuery("Inventory.sumUnitaryBalancesByArticleCode").
                setParameter("articleNumber", productItem.getId().getProductItemCode()).
                setParameter("companyNumber", movementDetail.getCompanyNumber()).getSingleResult();

        if (warehouseVoucher.isDevolution()
                && productItem.getControlValued()) {
            BigDecimal newInvestmentAmount = BigDecimalUtil.multiply(sumUnitaryBalances, productItem.getUnitCost(), 6);
            productItem.setInvestmentAmount(newInvestmentAmount);
        }

        if ((warehouseVoucher.isReception() || warehouseVoucher.isInput())
                && productItem.getControlValued()) {
            BigDecimal newInvestmentAmount =
                    BigDecimalUtil.sum(productItem.getInvestmentAmount(), movementDetail.getAmount(), 6);

            BigDecimal newUnitCost = BigDecimalUtil.divide(newInvestmentAmount, sumUnitaryBalances, 6);

            productItem.setInvestmentAmount(newInvestmentAmount);
            productItem.setUnitCost(newUnitCost);
        }

        getEntityManager().merge(productItem);
        getEntityManager().flush();
    }

    private void updateProductItemInformationForOutputs(WarehouseVoucher warehouseVoucher,
                                                        MovementDetail movementDetail) throws ProductItemAmountException {
        ProductItem productItem = getEntityManager().find(ProductItem.class, movementDetail.getProductItem().getId());

        BigDecimal sumUnitaryBalances = (BigDecimal) getEntityManager().
                createNamedQuery("Inventory.sumUnitaryBalancesByArticleCode").
                setParameter("articleNumber", productItem.getId().getProductItemCode()).
                setParameter("companyNumber", movementDetail.getCompanyNumber()).getSingleResult();

        if (warehouseVoucher.isConsumption()
                && productItem.getControlValued()) {
            BigDecimal newInvestmentAmount = BigDecimalUtil.multiply(sumUnitaryBalances, productItem.getUnitCost(), 6);
            productItem.setInvestmentAmount(newInvestmentAmount);
        }

        if (warehouseVoucher.isOutput()
                && productItem.getControlValued()) {
            BigDecimal newInvestmentAmount =
                    BigDecimalUtil.subtract(productItem.getInvestmentAmount(), movementDetail.getAmount(), 6);

            if (BigDecimal.ZERO.compareTo(newInvestmentAmount) == 1) {
                throw new ProductItemAmountException(
                        "There is not enough amount for process the movement detail",
                        productItem.getInvestmentAmount(),
                        productItem);
            }

            productItem.setInvestmentAmount(newInvestmentAmount);
            if (sumUnitaryBalances.compareTo(BigDecimal.ZERO) == 1 &&
                    newInvestmentAmount.compareTo(BigDecimal.ZERO) == 1) {
                BigDecimal newUnitCost = BigDecimalUtil.divide(newInvestmentAmount, sumUnitaryBalances, 6);
                productItem.setUnitCost(newUnitCost);
            }
        }

        getEntityManager().merge(productItem);
        getEntityManager().flush();
    }

    private void removeFromInventory(WarehouseVoucher warehouseVoucher,
                                     Warehouse warehouse,
                                     MovementDetail movementDetail)
            throws InventoryUnitaryBalanceException,
            InventoryProductItemNotFoundException {
        Inventory inventory = getInventory(warehouse, movementDetail.getProductItem());

        InventoryDetail inventoryDetail = getInventoryDetail(inventory,
                warehouseVoucher.getExecutorUnit(),
                warehouseVoucher.getCostCenterCode());

        if (null == inventoryDetail || BigDecimalUtil.isZeroOrNull(inventoryDetail.getQuantity())) {
            inventoryDetail = getInventoryDetailWithPublicCostCenter(inventory, warehouseVoucher.getExecutorUnit());
            if (null == inventoryDetail) {
                throw new InventoryProductItemNotFoundException("The selected productItem is not registered in inventories",
                        warehouseVoucher.getExecutorUnit().getFullName(),
                        movementDetail.getProductItem(),
                        warehouse);
            }
        }

        BigDecimal requiredQuantity = movementDetail.getQuantity();
        BigDecimal availableQuantity = inventoryDetail.getQuantity();
        BigDecimal newAvailableQuantity = BigDecimalUtil.subtract(availableQuantity, requiredQuantity);

        if (BigDecimal.ZERO.compareTo(newAvailableQuantity) == 1) {
            throw new InventoryUnitaryBalanceException(availableQuantity, movementDetail.getProductItem());
        }

        BigDecimal actualUnitaryBalance = inventory.getUnitaryBalance();
        BigDecimal newUnitaryBalance = BigDecimalUtil.subtract(actualUnitaryBalance, requiredQuantity);

        if (BigDecimal.ZERO.compareTo(newUnitaryBalance) == 1) {
            throw new InventoryUnitaryBalanceException(actualUnitaryBalance, movementDetail.getProductItem());
        }

        inventoryDetail.setQuantity(newAvailableQuantity);
        getEntityManager().merge(inventoryDetail);
        getEntityManager().flush();

        inventory.setUnitaryBalance(newUnitaryBalance);
        getEntityManager().merge(inventory);
        getEntityManager().flush();
    }

    private void addAtInventory(WarehouseVoucher warehouseVoucher, Warehouse warehouse, MovementDetail movementDetail) {

        Inventory inventory = getInventory(warehouse, movementDetail.getProductItem());

        if (null == inventory) {
            inventory = new Inventory();
            inventory.setId(getInventoryPK(warehouse, movementDetail.getProductItem()));
            inventory.setUnitaryBalance(movementDetail.getQuantity());
            getEntityManager().persist(inventory);
            getEntityManager().flush();
        } else {
            BigDecimal actualUnitaryBalance = inventory.getUnitaryBalance();
            inventory.setUnitaryBalance(BigDecimalUtil.sum(actualUnitaryBalance, movementDetail.getQuantity()));
            if (!getEntityManager().contains(inventory)) {
                getEntityManager().merge(inventory);
            }
            getEntityManager().flush();
        }

        BusinessUnit executorUnit = warehouseVoucher.getExecutorUnit();
        String costCenterCode = warehouseVoucher.getCostCenterCode();

        if (warehouseVoucher.isExecutorUnitTransfer()) {
            executorUnit = warehouseVoucher.getTargetExecutorUnit();
            costCenterCode = warehouseVoucher.getTargetCostCenterCode();
        }

        InventoryDetail inventoryDetail = getInventoryDetail(inventory, executorUnit, costCenterCode);

        if (null == inventoryDetail) {
            createInventoryDetail(inventory,
                    executorUnit,
                    costCenterCode,
                    movementDetail.getQuantity());
        } else {
            updateInventoryDetail(inventoryDetail, movementDetail.getQuantity());
        }
    }

    @SuppressWarnings(value = "unchecked")
    private InventoryDetail getInventoryDetail(Inventory inventory, BusinessUnit executorUnit, String costCenterCode) {

        List<InventoryDetail> inventoryDetails = getEntityManager().
                createNamedQuery("InventoryDetail.findByExecutorUnitAndCostCenter").
                setParameter("companyNumber", inventory.getId().getCompanyNumber()).
                setParameter("productItemCode", inventory.getId().getArticleCode()).
                setParameter("warehouseCode", inventory.getId().getWarehouseCode()).
                setParameter("executorUnit", executorUnit).
                setParameter("costCenterCode", costCenterCode).getResultList();

        if (null == inventoryDetails || inventoryDetails.isEmpty()) {
            return null;
        }

        if (inventoryDetails.size() > 1) {
            throw new RuntimeException("Cannot process multiple Inventory details entities for inventory=" + inventory
                    + ", executorUnitCode=" + executorUnit.getFullName()
                    + ", costCenterCode=" + costCenterCode);
        }

        return inventoryDetails.get(0);
    }

    @SuppressWarnings(value = "unchecked")
    private InventoryDetail getInventoryDetailWithPublicCostCenter(Inventory inventory,
                                                                   BusinessUnit executorUnit) {
        InventoryDetail inventoryDetail = null;
        List<InventoryDetail> inventoryDetails = getEntityManager().
                createNamedQuery("InventoryDetail.findByInventoryAndExecutorUnitCode").
                setParameter("companyNumber", inventory.getId().getCompanyNumber()).
                setParameter("productItemCode", inventory.getId().getArticleCode()).
                setParameter("warehouseCode", inventory.getId().getWarehouseCode()).
                setParameter("executorUnit", executorUnit).getResultList();

        if (!ValidatorUtil.isEmptyOrNull(inventoryDetails)) {
            for (int i = 0; i < inventoryDetails.size() && inventoryDetail == null; i++) {
                InventoryDetail inventoryDetailTemp = inventoryDetails.get(i);
                if (isPublicCostCenter(inventoryDetailTemp.getCostCenterCode(), inventoryDetailTemp.getCompanyNumber())
                        && BigDecimalUtil.isPositive(inventoryDetailTemp.getQuantity())) {
                    inventoryDetail = inventoryDetailTemp;
                }
            }
        }
        return inventoryDetail;
    }

    private boolean isPublicCostCenter(String costCenterCode, String companyNumber) {
        CostCenterPk costCenterPk = new CostCenterPk(companyNumber, costCenterCode);
        CostCenter costCenter = getEntityManager().find(CostCenter.class, costCenterPk);
        return !costCenter.getExclusiveConsumption();
    }

    private Inventory getInventory(Warehouse warehouse, ProductItem productItem) {
        InventoryPK inventoryPK = getInventoryPK(warehouse, productItem);

        Inventory inventory = getEntityManager().find(Inventory.class, inventoryPK);
//        getEntityManager().refresh(inventory);
        if (null != inventory) {
            return inventory;
        }

        return null;
    }

    private void createInventoryDetail(Inventory inventory,
                                       BusinessUnit executorUnit,
                                       String costCenterCode,
                                       BigDecimal quantity) {

        InventoryDetail inventoryDetail = new InventoryDetail();
        inventoryDetail.setProductItemCode(inventory.getId().getArticleCode());
        inventoryDetail.setWarehouseCode(inventory.getId().getWarehouseCode());

        inventoryDetail.setExecutorUnit(executorUnit);
        inventoryDetail.setCostCenterCode(costCenterCode);
        inventoryDetail.setQuantity(quantity);

        getEntityManager().persist(inventoryDetail);
        getEntityManager().flush();
    }

    private void updateInventoryDetail(InventoryDetail inventoryDetail, BigDecimal quantity) {
        BigDecimal newQuantity = BigDecimalUtil.sum(inventoryDetail.getQuantity(), quantity);
        inventoryDetail.setQuantity(newQuantity);
        getEntityManager().merge(inventoryDetail);
        getEntityManager().flush();
    }

    private InventoryPK getInventoryPK(Warehouse warehouse, ProductItem productItem) {
        return new InventoryPK(warehouse.getId().getCompanyNumber(),
                warehouse.getId().getWarehouseCode(),
                productItem.getId().getProductItemCode());
    }

    @SuppressWarnings(value = "unchecked")
    private List<MovementDetail> getPendantDetails(InventoryMovement pendantMovement,
                                                   MovementDetailType movementDetailType) {
        WarehouseVoucherState state =
                WarehouseVoucherState.valueOf(WarehouseVoucherState.class, pendantMovement.getId().getState());

        return getEntityManager().createNamedQuery("MovementDetail.findByState").
                setParameter("companyNumber", pendantMovement.getId().getCompanyNumber()).
                setParameter("transactionNumber", pendantMovement.getId().getTransactionNumber()).
                setParameter("state", state).
                setParameter("movementDetailType", movementDetailType).getResultList();
    }

    private InventoryMovement createApprovedInventoryMovement(InventoryMovement pendantMovement,
                                                              String financeUserCode) {

        InventoryMovementPK pk = new InventoryMovementPK(pendantMovement.getId().getCompanyNumber(),
                pendantMovement.getId().getTransactionNumber(),
                WarehouseVoucherState.APR.name());

        InventoryMovement newMovement = new InventoryMovement();
        newMovement.setId(pk);
        newMovement.setCreationDate(pendantMovement.getCreationDate());
        newMovement.setDescription(pendantMovement.getDescription());
        newMovement.setUserNumber(financeUserCode);

        Date processMonthDate = monthProcessService.getMothProcessDate(new Date());

        newMovement.setMovementDate(processMonthDate);

        getEntityManager().persist(newMovement);
        getEntityManager().flush();

        return newMovement;
    }

    private InventoryMovement createPartialInventoryMovement(InventoryMovement pendantMovement,
                                                             String financeUserCode) {

        InventoryMovementPK pk = new InventoryMovementPK(pendantMovement.getId().getCompanyNumber(),
                pendantMovement.getId().getTransactionNumber(),
                WarehouseVoucherState.PAR.name());

        InventoryMovement newMovement = new InventoryMovement();
        newMovement.setId(pk);
        newMovement.setCreationDate(pendantMovement.getCreationDate());
        newMovement.setDescription(pendantMovement.getDescription());
        newMovement.setUserNumber(financeUserCode);

        Date processMonthDate = monthProcessService.getMothProcessDate(new Date());

        newMovement.setMovementDate(processMonthDate);

        getEntityManager().persist(newMovement);
        getEntityManager().flush();

        return newMovement;
    }

    private String generateWarehouseVoucherNumber(WarehouseVoucher warehouseVoucher) {
        Long counter = (Long) listEm.createNamedQuery("WarehouseVoucher.countByWarehouseCode").
                setParameter("warehouse", warehouseVoucher.getWarehouse()).
                setParameter("states", WarehouseVoucherState.getValidStates()).getSingleResult();
        log.debug("1: The next warehouse voucher number is: " + counter);

        if (null == counter) {
            counter = (long) 0;
        } else {
            log.debug("2: The next warehouse voucher number is: " + counter);
            counter++;
        }
        log.debug("3: The next warehouse voucher number is: " + counter);
        return warehouseVoucher.getWarehouse().getId().getWarehouseCode() + "-" + counter;
    }

    @SuppressWarnings(value = "unchecked")
    private void validateOutputDetails(WarehouseVoucher warehouseVoucher,
                                       Warehouse warehouse) throws InventoryException {

        List<InventoryMessage> messages = new ArrayList<InventoryMessage>();

        List<ProductItem> productItems = getEntityManager().createNamedQuery("MovementDetail.findProductItems").
                setParameter("companyNumber", warehouseVoucher.getId().getCompanyNumber()).
                setParameter("transactionNumber", warehouseVoucher.getId().getTransactionNumber()).
                setParameter("state", warehouseVoucher.getState()).
                setParameter("movementDetailType", MovementDetailType.S).getResultList();

        for (ProductItem productItem : productItems) {
            BigDecimal total = (BigDecimal) getEntityManager().
                    createNamedQuery("MovementDetail.sumQuantitiesByProductItem").
                    setParameter("companyNumber", warehouseVoucher.getId().getCompanyNumber()).
                    setParameter("transactionNumber", warehouseVoucher.getId().getTransactionNumber()).
                    setParameter("state", warehouseVoucher.getState()).
                    setParameter("productItem", productItem).
                    setParameter("warehouseCode", warehouse.getId().getWarehouseCode()).
                    setParameter("movementDetailType", MovementDetailType.S).getSingleResult();

            Inventory inventory = getInventory(warehouse, productItem);
            if (null == inventory) {
                messages.add(new InventoryMessage(productItem, true));
                continue;
            }

            InventoryDetail inventoryDetail = getInventoryDetail(inventory,
                    warehouseVoucher.getExecutorUnit(),
                    warehouseVoucher.getCostCenterCode());

            if (null == inventoryDetail || BigDecimalUtil.isZeroOrNull(inventoryDetail.getQuantity())) {
                inventoryDetail = getInventoryDetailWithPublicCostCenter(inventory,
                        warehouseVoucher.getExecutorUnit());
                if (null == inventoryDetail) {
                    messages.add(new InventoryMessage(productItem, true));
                    continue;
                }
            }

            if (total.compareTo(inventoryDetail.getQuantity()) == 1) {
                messages.add(new InventoryMessage(productItem, true, inventoryDetail.getQuantity()));
            }
        }

        if (!messages.isEmpty()) {
            throw new InventoryException(messages);
        }
    }

    @SuppressWarnings(value = "unchecked")
    private void validateInputDetails(WarehouseVoucher warehouseVoucher,
                                      Warehouse warehouse) throws InventoryException {

        if (warehouseVoucher.isDevolution()) {

            List<InventoryMessage> messages = new ArrayList<InventoryMessage>();

            List<ProductItem> productItems = getEntityManager().createNamedQuery("MovementDetail.findProductItems").
                    setParameter("companyNumber", warehouseVoucher.getId().getCompanyNumber()).
                    setParameter("transactionNumber", warehouseVoucher.getId().getTransactionNumber()).
                    setParameter("state", warehouseVoucher.getState()).
                    setParameter("movementDetailType", MovementDetailType.E).getResultList();

            for (ProductItem productItem : productItems) {
                Inventory inventory = getInventory(warehouse, productItem);

                if (null == inventory) {
                    messages.add(new InventoryMessage(productItem, true));
                }
            }

            if (!messages.isEmpty()) {
                throw new InventoryException(messages);
            }
        }
    }

    private InventoryMovement getPendantMovement(WarehouseVoucher warehouseVoucher) {
        return (InventoryMovement) getEntityManager().createNamedQuery("InventoryMovement.findByState").
                setParameter("state", WarehouseVoucherState.PEN.name()).
                setParameter("transactionNumber", warehouseVoucher.getId().getTransactionNumber()).
                getSingleResult();
    }

    private void updateWarehouseVoucher(WarehouseVoucher warehouseVoucher) {
        warehouseVoucher.setState(WarehouseVoucherState.APR);
        warehouseVoucher.setNumber(generateWarehouseVoucherNumber(warehouseVoucher));
        getEntityManager().merge(warehouseVoucher);
        getEntityManager().flush();
    }

    private void updateWarehouseVoucherByState(WarehouseVoucher warehouseVoucher, WarehouseVoucherState warehouseVoucherState) {
        warehouseVoucher.setState(warehouseVoucherState);
        warehouseVoucher.setNumber(generateWarehouseVoucherNumber(warehouseVoucher));
        if (warehouseVoucher.getState().equals(WarehouseVoucherState.PAR)) {
            warehouseVoucher.setWarehouseVoucherReceptionType(WarehouseVoucherReceptionType.RP);
        }
        getEntityManager().merge(warehouseVoucher);
        getEntityManager().flush();
    }

    /**
     * This method update in a batch query the warning columns of the pendant vouchers
     * according its minimal, maximum stock configs, type of movement ,state, and a set of given products
     *
     * @param productItemList a list of ProductItem to filter the details to be updated
     */
    private void updatePendantVoucherWarningContent(List<ProductItem> productItemList) {
        String idealMessage = messages.get("MovementDetail.idealWarning");
        String underMinimalMessage = MessageUtils.getMessage("MovementDetail.underMinimalStockWarning");
        String overMaximumMessage = MessageUtils.getMessage("MovementDetail.overMaximumStockWarning");

        getEntityManager().createNamedQuery("MovementDetail.changeMovementDetailOverMaximumWarning")
                .setParameter("overMaximumMessage", overMaximumMessage)
                .setParameter("inputType", MovementDetailType.E)
                .setParameter("pendingState", WarehouseVoucherState.PEN)
                .setParameter("productItemList", productItemList)
                .executeUpdate();

        getEntityManager().createNamedQuery("MovementDetail.changeMovementDetailUnderMinimalWarning")
                .setParameter("underMinimalMessage", underMinimalMessage)
                .setParameter("inputType", MovementDetailType.E)
                .setParameter("pendingState", WarehouseVoucherState.PEN)
                .setParameter("productItemList", productItemList)
                .executeUpdate();
        getEntityManager().createNamedQuery("MovementDetail.changeMovementDetailIdealWarning")
                .setParameter("idealMessage", idealMessage)
                .setParameter("inputType", MovementDetailType.E)
                .setParameter("pendingState", WarehouseVoucherState.PEN)
                .setParameter("productItemList", productItemList)
                .executeUpdate();
        getEntityManager().flush();
    }

}
