package main.com.encens.khipus.service.production;

import com.encens.hp90.framework.service.GenericService;
import com.encens.hp90.model.production.CollectionForm;

import javax.ejb.Local;

@Local
public interface CollectionFormService extends GenericService {

    public void populateWithCollectionRecords(CollectionForm collectionForm);

    public void populateWithTotalsOfCollectedAmount(CollectionForm collectionForm);

    public void populateWithTotalsOfRejectedAmount(CollectionForm collectionForm);
}
