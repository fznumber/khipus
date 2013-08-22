package main.com.encens.khipus.service.production;


import com.encens.hp90.framework.service.ExtendedGenericServiceBean;
import com.encens.hp90.model.production.CollectionForm;
import com.encens.hp90.model.production.CollectionRecord;
import com.encens.hp90.model.production.ProductiveZone;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;

import javax.ejb.Stateless;
import javax.persistence.Query;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Name("CollectionFormService")
@AutoCreate
@Stateless
public class CollectionFormServiceBean extends ExtendedGenericServiceBean implements CollectionFormService {

    @In
    private ProductiveZoneService productiveZoneService;

    @Override
    public void populateWithCollectionRecords(CollectionForm collectionForm) {
        List<ProductiveZone> zones = productiveZoneService.findAll();
        for (ProductiveZone zone : zones) {
            CollectionRecord record = createCollectionRecord(collectionForm, zone);
            collectionForm.getCollectionRecordList().add(record);
        }
    }

    private CollectionRecord createCollectionRecord(CollectionForm collectionForm, ProductiveZone zone) {
        CollectionRecord record = new CollectionRecord();
        record.setCollectionForm(collectionForm);
        record.setProductiveZone(zone);
        record.setReceivedAmount(0.0);
        record.setWeightedAmount(0.0);
        record.setRejectedAmount(0.0);
        return record;
    }

    @Override
    public void populateWithTotalsOfCollectedAmount(CollectionForm collectionForm) {
        Query query = getEntityManager().createNamedQuery("CollectionForm.calculateCollectedAmountOnDateByMetaProduct")
                                        .setParameter("date", collectionForm.getDate())
                                        .setParameter("metaProduct", collectionForm.getMetaProduct());
        Map<Long, Double> map = createMap(query);
        for(CollectionRecord record : collectionForm.getCollectionRecordList()) {
            Double collectedRawMaterial = map.get(record.getProductiveZone().getId());
            if (collectedRawMaterial != null) {
                record.setReceivedAmount(collectedRawMaterial);
            } else {
                record.setReceivedAmount(0.0);
            }
        }
    }

    @Override
    public void populateWithTotalsOfRejectedAmount(CollectionForm collectionForm) {

    }

    private Map<Long, Double> createMap(Query query) {
        Map<Long, Double> map = new HashMap<Long, Double>();
        List<Object[]> results = query.getResultList();
        for(Object[] result : results) {
            Long id = (Long)result[0];
            Double value = (Double)result[1];
            map.put(id, value);
        }
        return map;
    }

    @Override
    protected Object preCreate(Object entity) {
        updateTotalsForCollectedAndRejectedRawMaterial((CollectionForm)entity);
        return entity;
    }

    @Override
    protected Object preUpdate(Object entity) {
        updateTotalsForCollectedAndRejectedRawMaterial((CollectionForm)entity);
        return entity;
    }

    private void updateTotalsForCollectedAndRejectedRawMaterial(CollectionForm form) {
        populateWithTotalsOfCollectedAmount(form);
        populateWithTotalsOfRejectedAmount(form);
    }
}
