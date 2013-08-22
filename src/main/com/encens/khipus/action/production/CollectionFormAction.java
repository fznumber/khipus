package com.encens.khipus.action.production;

import com.encens.khipus.framework.action.GenericAction;
import com.encens.khipus.framework.action.Outcome;
import com.encens.khipus.framework.service.GenericService;
import com.encens.khipus.model.production.CollectionForm;
import com.encens.khipus.model.production.CollectionRecord;
import com.encens.khipus.service.production.CollectionFormService;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.*;
import org.jboss.seam.log.Log;

import javax.faces.event.ActionEvent;

import static org.jboss.seam.international.StatusMessage.Severity.ERROR;

@Name("collectionFormAction")
@Scope(ScopeType.CONVERSATION)
public class CollectionFormAction extends GenericAction<CollectionForm> {

    @In("CollectionFormService")
    private CollectionFormService collectionFormService;

    @Logger
    private Log log;

    @Override
    protected GenericService getService() {
        return collectionFormService;
    }

    @Factory(value = "collectionForm", scope = ScopeType.STATELESS)
    public CollectionForm initCollectionForm() {
        return getInstance();
    }

    @Override
    protected String getDisplayNameProperty() {
        return "date";
    }

    @Override
    public CollectionForm createInstance() {
        CollectionForm form = super.createInstance();

        try {
            collectionFormService.populateWithCollectionRecords(form);
        } catch (Exception ex) {
            recordUnexpectedException(ex);
        }
        return form;
    }

    private void recordUnexpectedException(Exception ex) {
        log.error("Exception caught", ex);
        facesMessages.addFromResourceBundle(ERROR, "Common.globalError.description");
    }

    public void recalculateTotalAmounts(ActionEvent e) {
        CollectionForm collectionForm = getInstance();
        log.info("recalculating totals");
        if (collectionForm.getDate() == null || collectionForm.getMetaProduct() == null) {
            log.info("Canceling calculus of totals {0}, {1}", collectionForm.getDate(), collectionForm.getMetaProduct());
            return;
        }

        try {
            collectionFormService.populateWithTotalsOfCollectedAmount(getInstance());
            collectionFormService.populateWithTotalsOfRejectedAmount(getInstance());
        } catch (Exception ex) {
            recordUnexpectedException(ex);
        }
    }

    @Begin(ifOutcome = Outcome.SUCCESS, flushMode = FlushModeType.MANUAL)
    public String startCreate() {
        return Outcome.SUCCESS;
    }

    public double getTotalWeightedAmount() {
        double total = 0.0;
        for(CollectionRecord record : getInstance().getCollectionRecordList()) {
            total += record.getWeightedAmount();
        }
        return total;
    }

    public double getTotalRejectedAmount() {
        double total = 0.0;
        for(CollectionRecord record : getInstance().getCollectionRecordList()) {
            total += record.getRejectedAmount();
        }
        return total;
    }

    public double getTotalReceivedAmount() {
        double total = 0.0;
        for(CollectionRecord record : getInstance().getCollectionRecordList()) {
            total += record.getReceivedAmount();
        }
        return total;
    }

}
