package com.encens.khipus.service.production;

import com.encens.khipus.framework.service.GenericService;
import com.encens.khipus.model.production.MetaProduct;
import com.encens.khipus.model.production.ProductiveZone;
import com.encens.khipus.model.production.RawMaterialCollectionSession;

import javax.ejb.Local;

@Local
public interface RawMaterialCollectionSessionService extends GenericService {
    void updateRawMaterialProducer(RawMaterialCollectionSession rawMaterialCollectionSession,ProductiveZone productiveZone);
}
