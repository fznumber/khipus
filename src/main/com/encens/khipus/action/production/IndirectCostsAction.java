package com.encens.khipus.action.production;

import com.encens.khipus.exception.EntryDuplicatedException;
import com.encens.khipus.framework.action.*;
import com.encens.khipus.model.employees.Gestion;
import com.encens.khipus.model.employees.Month;
import com.encens.khipus.model.finances.CashAccount;
import com.encens.khipus.model.production.IndirectCosts;
import com.encens.khipus.model.production.IndirectCostsConfig;
import com.encens.khipus.model.production.PeriodIndirectCost;
import com.encens.khipus.service.production.IndirectCostsService;
import com.encens.khipus.service.production.PeriodIndirectCostService;
import com.encens.khipus.util.Constants;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.*;
import org.jboss.seam.annotations.Outcome;
import org.jboss.seam.international.StatusMessage;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

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
    private List<IndirectCosts> indirectCostses = new ArrayList<IndirectCosts>();
    private Gestion gestion;
    private Month month = Month.getCurrentMonth();

    @In
    private IndirectCostsService indirectCostsService;

    @In
    private PeriodIndirectCostService periodIndirectCostService;

    @Factory(value = "indirectCosts", scope = ScopeType.STATELESS)
    public IndirectCosts initProcessedProduct() {
        return getInstance();
    }

    @Override
    protected String getDisplayNameProperty() {
        return "name";
    }

    @Override
    @End
    public String create() {
        try {
            //todo:verificar q no repita el periodo y q los montos no sean cero
            refreshPeriodIndirectCost();
            if(indirectCostses == null || indirectCostses.size() == 0)
            {
                addNotFoundCostsIndirectMessage();
                return com.encens.khipus.framework.action.Outcome.REDISPLAY;
            }

            if(periodIndirectCost == null)
            {
                addNotFoundPeriodMessage();
                return com.encens.khipus.framework.action.Outcome.REDISPLAY;
            }

            if(periodIndirectCostService.findPeriodIndirect(periodIndirectCost))
            {
                addRepeatedPeriodMessage();
                return com.encens.khipus.framework.action.Outcome.REDISPLAY;
            }
            for(IndirectCosts costs:indirectCostses)
            {
                if(costs.getAmountBs().equals(BigDecimal.ZERO))
                {
                    addMountZeroMessage();
                    return com.encens.khipus.framework.action.Outcome.REDISPLAY;
                }
            }

            for(IndirectCosts costs:indirectCostses){
                costs.setPeriodIndirectCost(periodIndirectCost);
            getService().create(costs);
            }
            addCreatedMessage();
            return com.encens.khipus.framework.action.Outcome.SUCCESS;
        } catch (EntryDuplicatedException e) {
            addDuplicatedMessage();
            return com.encens.khipus.framework.action.Outcome.REDISPLAY;
        }
    }

    private void addMountZeroMessage() {
        facesMessages.addFromResourceBundle(StatusMessage.Severity.INFO,
                "Indirectcosts.message.mountZero");
    }

    private void addRepeatedPeriodMessage() {
        facesMessages.addFromResourceBundle(StatusMessage.Severity.INFO,
                "Indirectcosts.message.repeatedPeriod");
    }

    private void addNotFoundCostsIndirectMessage() {
        facesMessages.addFromResourceBundle(StatusMessage.Severity.INFO,
                "Indirectcosts.message.NotFoundPeriod");
    }

    private void addNotFoundPeriodMessage() {
        facesMessages.addFromResourceBundle(StatusMessage.Severity.INFO,
                "Indirectcosts.message.NotFoundPeriod");
    }

    public void refreshPeriodIndirectCost() {
        this.periodIndirectCost = periodIndirectCostService.findPeriodIndirect(month,gestion);
    }

    @Override
    protected void addCreatedMessage() {
        facesMessages.addFromResourceBundle(StatusMessage.Severity.INFO,
                "Indirectcosts.message.created");
    }

    public void findLastPeriod()
    {
        List<IndirectCosts> costGeneral = indirectCostsService.getIndirectCostGeneral(periodIndirectCostService.findLastPeriodIndirectCostUsed());
       for(IndirectCosts costs:costGeneral){
           IndirectCosts cost= new IndirectCosts();
           cost.setAmountBs(BigDecimal.ZERO);
           cost.setName(costs.getName());
           cost.setCostsConifg(costs.getCostsConifg());
           indirectCostses.add(cost);
       }

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

    public List<IndirectCosts> getIndirectCostses() {
        return indirectCostses;
    }

    public void setIndirectCostses(List<IndirectCosts> indirectCostses) {
        this.indirectCostses = indirectCostses;
    }

    public Gestion getGestion() {
        return gestion;
    }

    public void setGestion(Gestion gestion) {
        this.gestion = gestion;
    }

    public Month getMonth() {
        return month;
    }

    public void setMonth(Month month) {
        this.month = month;
    }
}
