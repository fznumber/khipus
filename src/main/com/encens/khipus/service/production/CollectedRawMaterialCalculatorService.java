package com.encens.khipus.service.production;

import com.encens.khipus.model.production.MetaProduct;

import javax.ejb.Local;
import java.util.Date;

@Local
public interface CollectedRawMaterialCalculatorService {

    public double calculateCollectedAmount(Date date, MetaProduct rawMaterial);

    public double calculateAvailableAmount(Date date, MetaProduct rawMaterial);

    public double calculateUsedAmount(Date date, MetaProduct rawMaterial);
}
