package com.encens.khipus.action.production;

import com.encens.khipus.exception.EntryDuplicatedException;
import com.encens.khipus.framework.action.GenericAction;
import com.encens.khipus.framework.action.Outcome;
import com.encens.khipus.model.production.RawMaterialProducer;
import com.encens.khipus.model.production.SalaryMovementProducer;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.*;

import static org.jboss.seam.international.StatusMessage.Severity.ERROR;

@Name("salaryMovementProducerAction")
@Scope(ScopeType.CONVERSATION)
public class SalaryMovementProducerAction extends GenericAction<SalaryMovementProducer> {

    private boolean readonly;

    @Factory(value = "salaryMovementProducer", scope = ScopeType.STATELESS)
    public SalaryMovementProducer initSalaryMovementProducer() {
        return getInstance();
    }

    public void selectRawMaterialProducer(RawMaterialProducer rawMaterialProducer) {
        try {
            rawMaterialProducer = getService().findById(RawMaterialProducer.class, rawMaterialProducer.getId());
            getInstance().setRawMaterialProducer(rawMaterialProducer);
        } catch (Exception ex) {
            log.error("Exception caught", ex);
            facesMessages.addFromResourceBundle(ERROR, "Common.globalError.description");
        }
    }
    @End
    @Override
    public String create() {
        try {
            getService().create(getInstance());
            addCreatedMessage();
            return Outcome.SUCCESS;
        } catch (EntryDuplicatedException e) {
            addDuplicatedMessage();
            return Outcome.REDISPLAY;
        }
    }

    @SuppressWarnings({"NullableProblems"})
    public void clearRawMaterialProducer() {
        getInstance().setRawMaterialProducer(null);
    }

    public boolean isReadonly() {
        return readonly;
    }

    public void setReadonly(boolean readonly) {
        this.readonly = readonly;
    }
}
