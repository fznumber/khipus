package com.encens.khipus.service.production;

import com.encens.khipus.framework.service.ExtendedGenericServiceBean;
import com.encens.khipus.model.production.ProductionOrder;
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
import java.util.Calendar;
import java.util.Date;

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

            total = (BigDecimal) em.createQuery("SELECT sum(indirectCosts.amountBs) from IndirectCosts indirectCosts" +
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

            total = (BigDecimal) em.createQuery("SELECT sum(indirectCosts.amountBs) from IndirectCosts indirectCosts" +
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

    public Double getTotalVolumeOrder(ProductionOrder productionOrder) {
        Double total = productionOrder.getProducedAmount();
        String unitMeasure = productionOrder.getProductComposition().getProcessedProduct().getUnidMeasure();
        Double amount = 0.0;
        if (productionOrder.getProductComposition().getProcessedProduct().getAmount() != null)
            amount = productionOrder.getProductComposition().getProcessedProduct().getAmount();

        if (unitMeasure == "KG" || unitMeasure == "LT")
            amount = productionOrder.getProductComposition().getProcessedProduct().getAmount() * 1000;

        total = amount * productionOrder.getProducedAmount();
        return total;
    }
}
