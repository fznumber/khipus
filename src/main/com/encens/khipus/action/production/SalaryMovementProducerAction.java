package com.encens.khipus.action.production;

import com.encens.khipus.exception.EntryDuplicatedException;
import com.encens.khipus.exception.EntryNotFoundException;
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
  /*  @End
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
    }*/

    @SuppressWarnings({"NullableProblems"})
    public void clearRawMaterialProducer() {
        getInstance().setRawMaterialProducer(null);
    }

    /*@Begin(ifOutcome = Outcome.SUCCESS, flushMode = FlushModeType.MANUAL)
    public String select(SalaryMovementProducer salaryMovementProducer) {
        try {
            SalaryMovementProducer discount = rawMaterialProducerDiscountService.prepareDiscount(rawMaterialProducer);
            setOp(OP_UPDATE);
            setInstance(discount);
            return Outcome.SUCCESS;
        } catch (Exception ex) {
            facesMessages.addFromResourceBundle(StatusMessage.Severity.ERROR, "Common.globalError.description");
            return Outcome.REDISPLAY;
        }
    }*/

/*    @Begin(ifOutcome = Outcome.SUCCESS, flushMode = FlushModeType.MANUAL)
    public String select(SalaryMovementProducer salaryMovementProducer) {
        try {
            setOp(OP_UPDATE);
            //define the unmanaged instance as current instance
            setInstance(salaryMovementProducer);
            //Ensure the instance exists in the database, find it
            setInstance(getService().findById(getEntityClass(), getId(salaryMovementProducer)));
            return Outcome.SUCCESS;

        } catch (EntryNotFoundException e) {
            addNotFoundMessage();
            return Outcome.FAIL;
        }
    }*/

    public boolean isReadonly() {
        return readonly;
    }

    public void setReadonly(boolean readonly) {
        this.readonly = readonly;
    }
}
