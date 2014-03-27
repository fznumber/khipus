package com.encens.khipus.service.warehouse;

import com.encens.khipus.action.production.ProductionPlanningAction;
import com.encens.khipus.exception.finances.CompanyConfigurationNotFoundException;
import com.encens.khipus.exception.finances.FinancesCurrencyNotFoundException;
import com.encens.khipus.exception.finances.FinancesExchangeRateNotFoundException;
import com.encens.khipus.framework.service.GenericService;
import com.encens.khipus.model.admin.BusinessUnit;
import com.encens.khipus.model.purchases.PurchaseOrder;
import com.encens.khipus.model.purchases.PurchaseOrderPayment;
import com.encens.khipus.model.warehouse.WarehouseVoucher;

import javax.ejb.Local;
import java.math.BigDecimal;
import java.util.List;

/**
 * @author
 * @version 2.24
 */
@Local
public interface WarehouseAccountEntryService extends GenericService {
    void createAccountEntry(WarehouseVoucher warehouseVoucher, String[] gloss) throws CompanyConfigurationNotFoundException, FinancesCurrencyNotFoundException, FinancesExchangeRateNotFoundException;

    void createAccountEntryFromProductDelivery(WarehouseVoucher warehouseVoucher, String[] gloss) throws CompanyConfigurationNotFoundException, FinancesCurrencyNotFoundException, FinancesExchangeRateNotFoundException;

    void createAccountEntryFromCollection(WarehouseVoucher warehouseVoucher, String[] gloss) throws CompanyConfigurationNotFoundException, FinancesCurrencyNotFoundException, FinancesExchangeRateNotFoundException;

    void createAdvancePaymentAccountEntry(PurchaseOrderPayment purchaseOrderPayment) throws CompanyConfigurationNotFoundException;

    void createEntryAccountForLiquidatedPurchaseOrder(PurchaseOrder purchaseOrder, BigDecimal defaultExchangeRate)
            throws CompanyConfigurationNotFoundException,
            FinancesCurrencyNotFoundException,
            FinancesExchangeRateNotFoundException;

    void createEntryAccountForPurchaseOrderPayment(PurchaseOrder purchaseOrder, PurchaseOrderPayment purchaseOrderPayment)
            throws CompanyConfigurationNotFoundException,
            FinancesCurrencyNotFoundException,
            FinancesExchangeRateNotFoundException;
    public String createAccountEntryForReceptionProductionOrder(WarehouseVoucher warehouseVoucher,
                                                  BusinessUnit executorUnit,
                                                  String costCenterCode,
                                                  String gloss,
                                                  List<ProductionPlanningAction.AccountOrderProduction> accountOrderProductions)
            throws CompanyConfigurationNotFoundException;
}
