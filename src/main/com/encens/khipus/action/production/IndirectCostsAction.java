package com.encens.khipus.action.production;

import com.encens.khipus.framework.action.GenericAction;
import com.encens.khipus.model.finances.CashAccount;
import com.encens.khipus.model.production.IndirectCosts;
import com.encens.khipus.model.production.IndirectCostsConfig;
import com.encens.khipus.model.production.PeriodIndirectCost;
import com.encens.khipus.util.Constants;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;

/**
 * Created with IntelliJ IDEA.
 * User: david
 * Date: 6/5/13
 * Time: 10:39 AM
 * To change this template use File | Settings | File Templates.
 */
@Name("indirectCostsAction")
@Scope(ScopeType.CONVERSATION)
public class IndirectCostsAction extends GenericAction<IndirectCosts> {
    private PeriodIndirectCost periodIndirectCost;
    private IndirectCostsConfig costsConifg;

    @Factory(value = "indirectCosts", scope = ScopeType.STATELESS)
    public IndirectCosts initProcessedProduct() {
        return getInstance();
    }

    @Override
    protected String getDisplayNameProperty() {
        return "name";
    }

    public PeriodIndirectCost getPeriodIndirectCost() {
        return periodIndirectCost;
    }

    public void setPeriodIndirectCost(PeriodIndirectCost periodIndirectCost) {
        this.periodIndirectCost = periodIndirectCost;
    }

    public IndirectCostsConfig getCostsConifg() {
        return costsConifg;
    }

    public void setCostsConifg(IndirectCostsConfig costsConifg) {
        this.costsConifg = costsConifg;
    }
}
