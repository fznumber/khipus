package com.encens.khipus.action.production;

import com.encens.khipus.exception.EntryDuplicatedException;
import com.encens.khipus.framework.action.*;
import com.encens.khipus.model.employees.Employee;
import com.encens.khipus.model.production.EmployeeTimeCard;
import com.encens.khipus.model.production.ProductionOrder;
import com.encens.khipus.model.production.ProductionPlanning;
import com.encens.khipus.service.production.EmployeeTimeCardService;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.*;
import org.jboss.seam.annotations.Outcome;

import javax.persistence.EntityManager;

/**
 * Employee Time Card action class
 *
 * @author Ariel Siles Encinas
 * @version 1.0
 */
@Name("employeeTimeCardAction")
@Scope(ScopeType.CONVERSATION)
public class EmployeeTimeCardAction extends GenericAction<EmployeeTimeCard> {

    @In
    private EmployeeTimeCardService employeeTimeCardService;

    @In("#{entityManager}")
    private EntityManager em;

    private ProductionPlanning productionPlanning;

    @Factory(value = "employeeTimeCard", scope = ScopeType.STATELESS)
    public EmployeeTimeCard initEmployeeTimeCard() {
        return getInstance();
    }

    @End
    @Override
    public String create() {
        try {
            EmployeeTimeCard timeCard = getInstance();
            timeCard.setCostPerHour(employeeTimeCardService.getCostPerHour(timeCard.getEmployee()));
            getService().create(timeCard);
            addCreatedMessage();
            return com.encens.khipus.framework.action.Outcome.SUCCESS;
        } catch (EntryDuplicatedException e) {
            addDuplicatedMessage();
            return com.encens.khipus.framework.action.Outcome.REDISPLAY;
        }
    }

    public void createAndNew() {
        try {
            EmployeeTimeCard timeCard = getInstance();
            timeCard.setCostPerHour(employeeTimeCardService.getCostPerHour(timeCard.getEmployee()));
            getService().create(timeCard);
            addCreatedMessage();
            createInstance();
        } catch (EntryDuplicatedException e) {
            addDuplicatedMessage();
        }
    }


    public void assignEmployee(Employee employee) {
        getInstance().setEmployee(employee);
    }

    public ProductionPlanning getProductionPlanning() {
        return productionPlanning;
    }

    public void setProductionPlanning(ProductionPlanning productionPlanning) {
        this.productionPlanning = productionPlanning;
    }

    public void test() {
        ProductionOrder po = (ProductionOrder) em.createNamedQuery("ProductionOrder.findById").setParameter("id", new Long(100)).getSingleResult();
        System.out.println("-______- OP: " + po.getCode());
        employeeTimeCardService.costProductionOrder(po);
    }

}