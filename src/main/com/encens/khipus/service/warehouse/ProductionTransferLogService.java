package main.com.encens.khipus.service.warehouse;

import com.encens.hp90.framework.service.GenericService;
import com.encens.hp90.model.production.MetaProduct;
import com.encens.hp90.model.warehouse.ProductionTransferLog;
import com.encens.hp90.model.warehouse.WarehouseSlot;

import javax.ejb.Local;
import java.util.List;

@Local
public interface ProductionTransferLogService extends GenericService {

    public void prepareForReceiving(ProductionTransferLog transfer);

    List<WarehouseSlot> findWarehouseSlots(MetaProduct product);
}
