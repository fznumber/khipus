package com.encens.khipus.service.production;

import com.encens.khipus.framework.service.GenericService;
import com.encens.khipus.model.production.CollectionForm;

import javax.ejb.Local;

@Local
public interface CollectionFormService extends GenericService {

    public void populateWithCollectionRecords(CollectionForm collectionForm);

    public void populateWithTotalsOfCollectedAmount(CollectionForm collectionForm);

    public void populateWithTotalsOfRejectedAmount(CollectionForm collectionForm);

    public void updateProductiveZone(CollectionForm collectionForm);
}
