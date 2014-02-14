package com.encens.khipus.service.production;

import com.encens.khipus.exception.ConcurrencyException;
import com.encens.khipus.exception.EntryDuplicatedException;
import com.encens.khipus.framework.service.GenericService;
import com.encens.khipus.model.production.*;
import com.encens.khipus.model.warehouse.ProductItem;
import com.encens.khipus.model.warehouse.WarehouseDocumentType;

import javax.ejb.Local;
import java.math.BigDecimal;
import java.util.List;


@Local
public interface ProductionPlanningService extends GenericService {

    public void refresh(Object entity);

    public ProductionPlanning find(long id);

    public BigDecimal getMountInWarehouse(MetaProduct metaProduct);

    public BigDecimal getMountInWarehouse(Long id);

    public BigDecimal getMountInWarehouse(ProductItem productItem);

    public WarehouseDocumentType getDefaultDocumentType();

    public WarehouseDocumentType getRecepcionDocumentType();

    public void updateProductionPlanning(ProductionPlanning instance,ProductionOrder order) throws ConcurrencyException, EntryDuplicatedException;

    public void deleteIndirectCost(ProductionOrder order);

    public void deleteIndirectCost(SingleProduct singleProduct);

    public List<IndirectCosts> findIndirectCostFromSingle(SingleProduct singleProduct);

    public void updateOrder(ProductionOrder order);

    public void addIndirectCostToSingleProduct(IndirectCosts indirectCosts);
}
