package com.encens.khipus.action.production;

import com.encens.khipus.exception.EntryDuplicatedException;
import com.encens.khipus.exception.EntryNotFoundException;
import com.encens.khipus.framework.action.GenericAction;
import com.encens.khipus.framework.action.Outcome;
import com.encens.khipus.framework.service.GenericService;
import com.encens.khipus.model.employees.RHMark;
import com.encens.khipus.model.production.*;
import com.encens.khipus.service.production.RawMaterialCollectionSessionService;
import com.encens.khipus.service.production.RawMaterialProducerService;
import org.apache.poi.hssf.record.formula.functions.T;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.*;
import org.jboss.seam.international.StatusMessage;

import javax.persistence.EntityManager;
import java.util.List;

@Name("rawMaterialCollectionSessionAction")
@Scope(ScopeType.CONVERSATION)
public class RawMaterialCollectionSessionAction extends GenericAction<RawMaterialCollectionSession> {

    @In
    private RawMaterialProducerService rawMaterialProducerService;

    @In
    private RawMaterialCollectionSessionService rawMaterialCollectionSessionService;

    @In("#{entityManager}")
    private EntityManager em;

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

    @Override
    @End
    public String create() {
        try {

            RawMaterialCollectionSession session = getInstance();
            List<Object[]> result = em.createQuery("select s from RawMaterialCollectionSession s where s.date = :date and s.productiveZone = :productiveZone")
                    .setParameter("date", session.getDate())
                    .setParameter("productiveZone", session.getProductiveZone())
                    .getResultList();

            if(result.size()>0)
            {
                addDuplicateDateMessage(session);
                return Outcome.REDISPLAY;
            }
            getService().create(session);
            addCreatedMessage();
            return Outcome.SUCCESS;
        } catch (EntryDuplicatedException e) {
            addDuplicatedMessage();
            return Outcome.REDISPLAY;
        }
    }

    @Begin(ifOutcome = Outcome.SUCCESS, flushMode = FlushModeType.MANUAL)
    @Override
    public String select(RawMaterialCollectionSession instance) {

        try {
            rawMaterialCollectionSessionService.updateRawMaterialProducer(getService().findById(getEntityClass(), getId(instance)),instance.getProductiveZone());
            setOp(OP_UPDATE);
            //define the unmanaged instance as current instance
            this.setInstance(instance);
            //Ensure the instance exists in the database, find it
            setInstance(getService().findById(getEntityClass(), getId(instance)));
            return Outcome.SUCCESS;

        } catch (EntryNotFoundException e) {
            addNotFoundMessage();
            return Outcome.FAIL;
        }
    }

    protected void addDuplicateDateMessage(RawMaterialCollectionSession session) {
        facesMessages.addFromResourceBundle(StatusMessage.Severity.ERROR,
                "RawMaterialCollectionSession.message.duplicateDate", session.getDate());
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

    public boolean isPending() {
        return ProductionCollectionState.PENDING.equals(getInstance().getState());
    }

}
