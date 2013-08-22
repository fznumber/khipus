package com.encens.khipus.service.production;

import com.encens.khipus.framework.service.ExtendedGenericServiceBean;
import com.encens.khipus.model.production.*;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;

import javax.ejb.Stateless;
import javax.persistence.NoResultException;

@Name("rawMaterialCollectionSessionService")
@Stateless
@AutoCreate
public class RawMaterialCollectionSessionServiceBean extends ExtendedGenericServiceBean implements RawMaterialCollectionSessionService {

    @In
    private ProductiveZoneService productiveZoneService;

    @Override
    protected Object preCreate(Object entity) {
        updateCollectionRecord((RawMaterialCollectionSession) entity);
        return entity;
    }

    private void updateCollectionRecord(RawMaterialCollectionSession entity) {
        RawMaterialCollectionSession session = (RawMaterialCollectionSession)entity;
        double totalAmount = sumCollectedAmount(session);
        updateReceivedAmountOnCollectionRecord(session, totalAmount);
    }

    private void updateReceivedAmountOnCollectionRecord(RawMaterialCollectionSession session, double totalAmount) {
        CollectionRecord record = findCollectionRecord(session);
        if (record == null) {
            return;
        }

        record.setReceivedAmount(totalAmount);
    }

    @Override
    protected Object preUpdate(Object entity) {
        updateCollectionRecord((RawMaterialCollectionSession) entity);
        return entity;
    }

    @Override
    protected Object preDelete(Object entity) {
        RawMaterialCollectionSession session = (RawMaterialCollectionSession)entity;
        updateReceivedAmountOnCollectionRecord(session, 0.0);
        return entity;
    }

    private CollectionRecord findCollectionRecord(RawMaterialCollectionSession rawMaterialCollectionSession) {
        try {
            CollectionRecord record = (CollectionRecord) getEntityManager().createNamedQuery("CollectionRecord.findByDateAndProductiveZoneAndMetaProduct")
                                                                            .setParameter("date", rawMaterialCollectionSession.getDate())
                                                                            .setParameter("productiveZone", rawMaterialCollectionSession.getProductiveZone())
                                                                            .setParameter("metaProduct", rawMaterialCollectionSession.getMetaProduct())
                                                                            .getSingleResult();
            return record;
        } catch (NoResultException ex) {
            return null;
        }
    }

    private double sumCollectedAmount(RawMaterialCollectionSession rawMaterialCollectionSession) {
        double total = 0.0;
        for (CollectedRawMaterial rawMaterial : rawMaterialCollectionSession.getCollectedRawMaterialList()) {
            total += rawMaterial.getAmount();
        }
        return total;
    }
}
