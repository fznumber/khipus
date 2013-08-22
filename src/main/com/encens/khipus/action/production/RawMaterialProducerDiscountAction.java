package main.com.encens.khipus.action.production;

import com.encens.hp90.framework.action.GenericAction;
import com.encens.hp90.framework.action.Outcome;
import com.encens.hp90.framework.service.GenericService;
import com.encens.hp90.model.production.RawMaterialProducer;
import com.encens.hp90.model.production.RawMaterialProducerDiscount;
import com.encens.hp90.service.production.RawMaterialProducerDiscountService;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.*;
import org.jboss.seam.international.StatusMessage;

@Name("rawMaterialProducerDiscountAction")
@Scope(ScopeType.CONVERSATION)
public class RawMaterialProducerDiscountAction extends GenericAction<RawMaterialProducerDiscount> {

    @In
    private RawMaterialProducerDiscountService rawMaterialProducerDiscountService;

    @Override
    protected GenericService getService() {
        return rawMaterialProducerDiscountService;
    }

    @Factory(value = "rawMaterialProducerDiscount", scope = ScopeType.STATELESS)
    public RawMaterialProducerDiscount initRawMaterialProducerDiscount() {
        return getInstance();
    }

    @Begin(ifOutcome = Outcome.SUCCESS, flushMode = FlushModeType.MANUAL)
    public String select(RawMaterialProducer rawMaterialProducer) {
        try {
            RawMaterialProducerDiscount discount = rawMaterialProducerDiscountService.prepareDiscount(rawMaterialProducer);
            setOp(OP_UPDATE);
            setInstance(discount);
            return Outcome.SUCCESS;
        } catch (Exception ex) {
            facesMessages.addFromResourceBundle(StatusMessage.Severity.ERROR, "Common.globalError.description");
            return Outcome.REDISPLAY;
        }
    }
}
