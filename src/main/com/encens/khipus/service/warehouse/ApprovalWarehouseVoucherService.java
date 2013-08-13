package com.encens.khipus.service.warehouse;


import com.encens.khipus.exception.ConcurrencyException;
import com.encens.khipus.exception.ReferentialIntegrityException;
import com.encens.khipus.exception.finances.CompanyConfigurationNotFoundException;
import com.encens.khipus.exception.finances.FinancesCurrencyNotFoundException;
import com.encens.khipus.exception.finances.FinancesExchangeRateNotFoundException;
import com.encens.khipus.exception.warehouse.*;
import com.encens.khipus.framework.service.GenericService;
import com.encens.khipus.model.finances.CostCenter;
import com.encens.khipus.model.warehouse.*;

import javax.ejb.Local;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * @author
 * @version 3.0
 */
@Local
public interface ApprovalWarehouseVoucherService extends GenericService {

    void approveWarehouseVoucher(WarehouseVoucherPK id, String[] gloss,
                                 Map<MovementDetail, BigDecimal> movementDetailUnderMinimalStockMap,
                                 Map<MovementDetail, BigDecimal> movementDetailOverMaximumStockMap,
                                 List<MovementDetail> movementDetailWithoutWarnings) throws InventoryException,
            WarehouseVoucherApprovedException,
            WarehouseVoucherNotFoundException,
            WarehouseVoucherEmptyException,
            ProductItemAmountException, InventoryUnitaryBalanceException,
            InventoryProductItemNotFoundException, CompanyConfigurationNotFoundException,
            FinancesCurrencyNotFoundException, FinancesExchangeRateNotFoundException,
            ConcurrencyException, ReferentialIntegrityException, ProductItemNotFoundException;

    void validateOutputMovementDetail(WarehouseVoucher warehouseVoucher,
                                      Warehouse warehouse,
                                      MovementDetail movementDetail,
                                      boolean isUpdate) throws InventoryException;

    void validateOutputQuantity(BigDecimal outputQuantity,
                                Warehouse warehouse,
                                ProductItem productItem,
                                CostCenter costCenter) throws InventoryException;

    // change the state to partial to a parent WarehouseVoucher
    void approvePartialInputParentWarehouseVoucher(WarehouseVoucherPK id)
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
            ReferentialIntegrityException, ProductItemNotFoundException, MovementDetailTypeException, WarehouseVoucherStateException;
}
