package com.encens.khipus.service.production;

import com.encens.khipus.framework.service.ExtendedGenericServiceBean;
import com.encens.khipus.model.finances.CashAccount;
import com.encens.khipus.model.production.IndirectCosts;
import com.encens.khipus.model.production.PeriodIndirectCost;
import com.encens.khipus.model.production.ProductionOrder;
import com.encens.khipus.model.warehouse.Group;
import com.encens.khipus.model.warehouse.SubGroup;
import com.encens.khipus.util.Constants;
import com.encens.khipus.util.DateUtils;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Diego
 * Date: 12/11/13
 * Time: 19:23
 * To change this template use File | Settings | File Templates.
 */
@Name("indirectCostsService")
@AutoCreate
@Stateless
public class IndirectCostsServiceBean extends ExtendedGenericServiceBean implements IndirectCostsService {

    @In(value = "#{entityManager}")
    private EntityManager em;

    @Override
    public Double getTotalCostIndirectGeneral() {
        BigDecimal total = new BigDecimal(0.0);
        Date date = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(date.getTime());
        calendar.add(Calendar.MONTH, -1);

        try {

            total = (BigDecimal) em.createQuery("SELECT sum(indirectCosts.amountBs) from IndirectCostsConifg indirectCosts" +
                    " where indirectCosts.month = :month and indirectCosts.year = :year and indirectCosts.type = :type")
                    .setParameter("month", DateUtils.getCurrentMonth(calendar.getTime()))
                    .setParameter("year", DateUtils.getCurrentYear(calendar.getTime()))
                    .setParameter("type", Constants.INDIRECT_COST_TYPE_GENERAL)
                    .getSingleResult();

        } catch (NoResultException e) {
            total = new BigDecimal(0.0);
        }
        return total.doubleValue() / 30;
    }

    public List<IndirectCosts> getIndirectCostGeneral(PeriodIndirectCost indirectCost)
    {
        List<IndirectCosts> indirectCostsList = new ArrayList<IndirectCosts>();

        try{
            indirectCostsList = em.createQuery("select indirectCosts from IndirectCosts indirectCosts " +
                                               " inner join indirectCosts.costsConifg costsConifg " +
                                               " where indirectCosts.periodIndirectCost = :periodIndirectCost")
                                  .setParameter("periodIndirectCost",indirectCost)
                                  .getResultList();

        } catch(NoResultException e){
            return new ArrayList<IndirectCosts>();
        }

        return indirectCostsList;
    }

    public List<IndirectCosts> getIndirectCostByGroup(PeriodIndirectCost indirectCost, SubGroup subGroup)
    {
        List<IndirectCosts> indirectCostsList = new ArrayList<IndirectCosts>();

        try{
            indirectCostsList = em.createQuery("select indirectCosts from IndirectCosts indirectCosts " +
                    " inner join indirectCosts.costsConifg costsConifg " +
                    " where indirectCosts.periodIndirectCost = :periodIndirectCost" +
                    " and indirectCosts.costsConifg.group = :group")
                    .setParameter("group",subGroup.getGroup())
                    .setParameter("periodIndirectCost", indirectCost)
                    .getResultList();

        } catch(NoResultException e){
            return new ArrayList<IndirectCosts>();
        }

        return indirectCostsList;
    }

    @Override
    public List<IndirectCosts> getCostTotalIndirect(ProductionOrder productionOrder, Double totalVolumDay, Double totalVolumGeneralDay, PeriodIndirectCost periodIndirectCost) {

        List<IndirectCosts> indirectCostses = new ArrayList<IndirectCosts>();

        indirectCostses.addAll(generateCosts(periodIndirectCost, productionOrder, totalVolumGeneralDay, getIndirectCostGeneral(periodIndirectCost)));
        indirectCostses.addAll(generateCosts(periodIndirectCost, productionOrder, totalVolumDay, getIndirectCostByGroup(periodIndirectCost, productionOrder.getProductComposition().getProcessedProduct().getProductItem().getSubGroup())));

        /*Double totalVolumeOrder = getTotalVolumeOrder(productionOrder);
        Double totalCostIndirectGeneral = getTotalCostIndirectGeneral();
        Double totalCostIndirectByGroup = getTotalCostIndirectByGroup(productionOrder.getProductComposition().getProcessedProduct().getProductItem().getSubGroup());
        Double costGeneral = totalCostIndirectGeneral * getPorcent(totalVolumGeneralDay, totalVolumeOrder) / 100;
        Double costByGroup = totalCostIndirectByGroup * getPorcent(totalVolumDay, totalVolumeOrder) / 100;*/

        return indirectCostses;
    }

    @Override
    public PeriodIndirectCost getLastPeroidIndirectCost()
    {
        List<PeriodIndirectCost> periodIndirectCosts = new ArrayList<PeriodIndirectCost>();

        try{

            periodIndirectCosts = em.createQuery("select periodIndirectCost from PeriodIndirectCost periodIndirectCost " +
                                                 "order by periodIndirectCost.id desc")
                                                .getResultList();
        }catch (NoResultException e){
            return null;
        }

        return periodIndirectCosts.get(0);
    }

    private List<IndirectCosts> generateCosts( PeriodIndirectCost periodIndirectCost,ProductionOrder productionOrder, Double totalVolumDay, List<IndirectCosts> costses)
    {
        List<IndirectCosts> indirectCostses = new ArrayList<IndirectCosts>();
        for(IndirectCosts costs: costses)
        {
            IndirectCosts aux = new IndirectCosts();
            aux.setName(costs.getName());
            aux.setCompany(costs.getCompany());
            aux.setCostsConifg(costs.getCostsConifg());
            Double totalVolumeOrder = getTotalVolumeOrder(productionOrder);
            Double amountCost = costs.getAmountBs().doubleValue() / 30;
            Double costGeneral = amountCost * getPorcent(totalVolumDay, totalVolumeOrder) / 100;
            aux.setAmountBs(new BigDecimal(costGeneral));
            indirectCostses.add(aux);
        }
        return indirectCostses;
    }

    public Double getTotalCostIndirectByAccount(CashAccount cashAccount) {
        BigDecimal total = new BigDecimal(0.0);
        Date date = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(date.getTime());
        calendar.add(Calendar.MONTH, -1);

        try {

            total = (BigDecimal) em.createQuery("SELECT sum(indirectCosts.amountBs) from IndirectCostsConifg indirectCosts" +
                    " where indirectCosts.month = :month and indirectCosts.year = :year and indirectCosts.cashAccount = :cashAccount")
                    .setParameter("month", DateUtils.getCurrentMonth(calendar.getTime()))
                    .setParameter("year", DateUtils.getCurrentYear(calendar.getTime()))
                    .setParameter("cashAccount", cashAccount)
                    .getSingleResult();

        } catch (NoResultException e) {
            total = new BigDecimal(0.0);
        }
        return total.doubleValue() / 30;
    }

    public Double getTotalCostIndirectGeneral(String type) {
        BigDecimal total = new BigDecimal(0.0);
        Date date = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(date.getTime());
        calendar.add(Calendar.MONTH, -1);

        try {

            total = (BigDecimal) em.createQuery("SELECT sum(indirectCosts.amountBs) from IndirectCostsConifg indirectCosts" +
                    " where indirectCosts.month = :month and indirectCosts.year = :year and indirectCosts.type = :type")
                    .setParameter("month", DateUtils.getCurrentMonth(calendar.getTime()))
                    .setParameter("year", DateUtils.getCurrentYear(calendar.getTime()))
                    .setParameter("type", type)
                    .getSingleResult();

        } catch (NoResultException e) {
            total = new BigDecimal(0.0);
        }
        return total.doubleValue() / 30;
    }

    public Double getPorcent(Double totalDay, Double totalOrder) {
        if (totalDay == 0.0)
            return 0.0;

        return (totalOrder * 100) / totalDay;
    }

    @Override
    public Double getCostTotalIndirect(ProductionOrder productionOrder, Double totalVolumDay, Double totalVolumGeneralDay) {
        Double totalCost = 0.0;

        Double totalVolumeOrder = getTotalVolumeOrder(productionOrder);
        Double totalCostIndirectGeneral = getTotalCostIndirectGeneral();
        Double totalCostIndirectByGroup = getTotalCostIndirectByGroup(productionOrder.getProductComposition().getProcessedProduct().getProductItem().getSubGroup());
        Double costGeneral = totalCostIndirectGeneral * getPorcent(totalVolumGeneralDay, totalVolumeOrder) / 100;
        Double costByGroup = totalCostIndirectByGroup * getPorcent(totalVolumDay, totalVolumeOrder) / 100;
        totalCost = costGeneral + costByGroup;
        return totalCost;
    }

    @Override
    public Double getTotalCostIndirectByGroup(SubGroup subGroup) {
        BigDecimal total = new BigDecimal(0.0);
        try {

            total = (BigDecimal) em.createQuery("SELECT sum(indirectCosts.amountBs) from IndirectCostsConifg indirectCosts" +
                    " where indirectCosts.month = :month and indirectCosts.year = :year and indirectCosts.group = :group ")
                    //" where indirectCosts.month = :month and indirectCosts.year = :year and indirectCosts.group.id = :id ")
                    //.setParameter("id",subGroup.getGroupCode())
                    //.setParameter("id","8")
                    .setParameter("group", subGroup.getGroup())
                    .setParameter("month", DateUtils.getCurrentMonth(new Date()) - 1)
                    .setParameter("year", DateUtils.getCurrentYear(new Date()))
                    .getSingleResult();


        } catch (NoResultException e) {
            total = new BigDecimal(0.0);
        }
        if (total == null )
            total = new BigDecimal(0.0);

        return total.doubleValue() / 30;
    }
    //TODO: se utilizara la cantidad esperada en lugar de la producida
    public Double getTotalVolumeOrder(ProductionOrder productionOrder) {
        //Double total = productionOrder.getProducedAmount();
        Double total = productionOrder.getExpendAmount();
        String unitMeasure = productionOrder.getProductComposition().getProcessedProduct().getUnidMeasure();
        Double amount = 0.0;
        if (productionOrder.getProductComposition().getProcessedProduct().getAmount() != null)
            amount = productionOrder.getProductComposition().getProcessedProduct().getAmount();

        if (unitMeasure == "KG" || unitMeasure == "LT")
            amount = productionOrder.getProductComposition().getProcessedProduct().getAmount() * 1000;

        //total = amount * productionOrder.getProducedAmount();
        total = amount * productionOrder.getExpendAmount();
        return total;
    }
}
