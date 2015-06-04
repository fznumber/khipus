package com.encens.khipus.action.accounting;

import com.encens.khipus.framework.action.GenericAction;
import com.encens.khipus.model.finances.Voucher;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.*;

/**
 * OrganizationAction
 *
 * @author
 * @version 2.26
 */
@Name("voucherAction")
@Scope(ScopeType.CONVERSATION)
public class VoucherAction extends GenericAction<Voucher> {

    @Factory(value = "voucher", scope = ScopeType.STATELESS)
    public Voucher initVoucher() {
        return getInstance();
    }

    @Override
    protected String getDisplayNameProperty() {
        return "name";
    }


    @Override
    @Begin(ifOutcome = com.encens.khipus.framework.action.Outcome.SUCCESS, flushMode = FlushModeType.MANUAL)
    public String select(Voucher instance) {
        String outCome = super.select(instance);
        return outCome;
    }
}
