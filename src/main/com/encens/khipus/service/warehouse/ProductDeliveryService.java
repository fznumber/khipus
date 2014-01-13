package com.encens.khipus.service.warehouse;

import com.encens.khipus.exception.ConcurrencyException;
import com.encens.khipus.exception.EntryDuplicatedException;
import com.encens.khipus.exception.ReferentialIntegrityException;
import com.encens.khipus.exception.finances.CompanyConfigurationNotFoundException;
import com.encens.khipus.exception.finances.FinancesCurrencyNotFoundException;
import com.encens.khipus.exception.finances.FinancesExchangeRateNotFoundException;
import com.encens.khipus.exception.warehouse.*;
import com.encens.khipus.framework.service.GenericService;
import com.encens.khipus.model.warehouse.ProductDelivery;

import javax.ejb.Local;

/**
 * @author
 * @version 3.0
 */

@Local
public interface ProductDeliveryService extends GenericService {
    ProductDelivery create(String invoiceNumber,
                           String warehouseDescription)
            throws InventoryException,
            WarehouseDocumentTypeNotFoundException,
            PublicCostCenterNotFound,
            ProductItemAmountException,
            InventoryUnitaryBalanceException,
            InventoryProductItemNotFoundException,
            SoldProductDeliveredException,
            CompanyConfigurationNotFoundException,
            FinancesCurrencyNotFoundException,
            FinancesExchangeRateNotFoundException,
            EntryDuplicatedException,
            ConcurrencyException,
            ReferentialIntegrityException,
            ProductItemNotFoundException;

    ProductDelivery select(ProductDelivery entity);

    public void updateOrderEstate(String invoiceNumber);
}
