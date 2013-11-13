package com.encens.khipus.service.production;

import com.encens.khipus.framework.service.GenericService;
import com.encens.khipus.model.production.MetaProduct;
import com.encens.khipus.model.production.ProductionPlanning;
import com.encens.khipus.model.warehouse.ProductItem;

import javax.ejb.Local;
import java.math.BigDecimal;


@Local
public interface ProductionPlanningService extends GenericService {

    public void refresh(Object entity);

    public ProductionPlanning find(long id);

    public BigDecimal getMountInWarehouse(MetaProduct metaProduct);

    public BigDecimal getMountInWarehouse(Long id);

    public BigDecimal getMountInWarehouse(ProductItem productItem);

}
