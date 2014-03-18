package com.encens.khipus.service.production;

import com.encens.khipus.exception.EntryNotFoundException;
import com.encens.khipus.framework.service.GenericService;
import com.encens.khipus.model.production.RawMaterialProducer;
import com.encens.khipus.model.production.RawMaterialProducerDiscount;

import javax.ejb.Local;
import java.util.Date;

@Local
public interface SalaryMovementProducerService extends GenericService {
    public RawMaterialProducerDiscount prepareDiscount(RawMaterialProducer rawMaterialProducer, Date startDate, Date endDate) throws EntryNotFoundException;

    Double getTotalCollectedByProductor(RawMaterialProducer rawMaterialProducer, Date date);
}
