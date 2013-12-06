package com.encens.khipus.service.production;

import com.encens.khipus.framework.service.GenericService;
import com.encens.khipus.model.production.ProductionOrder;
import com.encens.khipus.model.warehouse.Group;
import com.encens.khipus.model.warehouse.ProductItem;
import com.encens.khipus.model.warehouse.SubGroup;

import javax.ejb.Local;

/**
 * Created with IntelliJ IDEA.
 * User: Diego
 * Date: 12/11/13
 * Time: 19:11
 * To change this template use File | Settings | File Templates.
 */
@Local
public interface IndirectCostsService extends GenericService {

    public Double getTotalCostIndirectByGroup(SubGroup subGroup);

    public Double getTotalCostIndirectGeneral();

    public Double getCostTotalIndirect(ProductionOrder productionOrder, Double totalVolumDay, Double totalVolumGeneralDay);
}
