package com.encens.khipus.action.production;

import com.encens.khipus.framework.action.GenericAction;
import com.encens.khipus.framework.action.Outcome;
import com.encens.khipus.framework.service.GenericService;
import com.encens.khipus.model.production.CollectedRawMaterial;
import com.encens.khipus.model.production.ProductiveZone;
import com.encens.khipus.model.production.RawMaterialCollectionSession;
import com.encens.khipus.model.production.RawMaterialProducer;
import com.encens.khipus.service.production.RawMaterialCollectionSessionService;
import com.encens.khipus.service.production.RawMaterialProducerService;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.*;

@Name("rawMaterialCollectionSessionAction")
@Scope(ScopeType.CONVERSATION)
public class RawMaterialCollectionSessionAction extends GenericAction<RawMaterialCollectionSession> {

    @In
    private RawMaterialProducerService rawMaterialProducerService;

    @In
    private RawMaterialCollectionSessionService rawMaterialCollectionSessionService;

    @Override
    protected GenericService getService() {
        return rawMaterialCollectionSessionService;
    }

    @Factory(value = "rawMaterialCollectionSession", scope = ScopeType.STATELESS)
    public RawMaterialCollectionSession initRawMaterialCollectionSession() {
        return getInstance();
    }

    @Override
    protected String getDisplayNameProperty() {
        return "date";
    }

    @Begin(ifOutcome = Outcome.SUCCESS, flushMode = FlushModeType.MANUAL)
    public String startCreate() {
        return Outcome.SUCCESS;
    }

    public double getTotalAmount() {
        double total = 0.0;
        for(CollectedRawMaterial cm : getInstance().getCollectedRawMaterialList()) {
            total += cm.getAmount();
        }
        return total;
    }

    public void selectProductiveZone(ProductiveZone productiveZone) {
        RawMaterialCollectionSession session = getInstance();
        session.setProductiveZone(productiveZone);
        session.getCollectedRawMaterialList().clear();
        for (RawMaterialProducer producer : rawMaterialProducerService.findAll(productiveZone)) {
            CollectedRawMaterial rawMaterial = new CollectedRawMaterial();
            rawMaterial.setAmount(0.0);
            rawMaterial.setRawMaterialProducer(producer);
            rawMaterial.setRawMaterialCollectionSession(session);
            session.getCollectedRawMaterialList().add(rawMaterial);
        }
    }

}
