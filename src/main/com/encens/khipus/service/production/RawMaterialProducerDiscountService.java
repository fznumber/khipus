package main.com.encens.khipus.service.production;

import com.encens.hp90.exception.EntryNotFoundException;
import com.encens.hp90.framework.service.GenericService;
import com.encens.hp90.model.production.RawMaterialProducer;
import com.encens.hp90.model.production.RawMaterialProducerDiscount;

import javax.ejb.Local;

@Local
public interface RawMaterialProducerDiscountService extends GenericService {
    public RawMaterialProducerDiscount prepareDiscount(RawMaterialProducer rawMaterialProducer) throws EntryNotFoundException;
}
