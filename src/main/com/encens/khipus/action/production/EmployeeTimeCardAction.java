package com.encens.khipus.action.production;

import com.encens.khipus.exception.EntryDuplicatedException;
import com.encens.khipus.framework.action.GenericAction;
import com.encens.khipus.model.employees.Employee;
import com.encens.khipus.model.production.EmployeeTimeCard;
import com.encens.khipus.model.production.ProductionOrder;
import com.encens.khipus.model.production.ProductionPlanning;
import com.encens.khipus.model.production.ProductionTaskType;
import com.encens.khipus.model.warehouse.Group;
import com.encens.khipus.service.employees.EmployeeService;
import com.encens.khipus.service.production.EmployeeTimeCardService;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.*;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage;

import javax.persistence.EntityManager;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

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

    @In
    private EmployeeService employeeService;

    @In("#{entityManager}")
    private EntityManager em;

    @In
    protected FacesMessages facesMessages;

    private Employee employeeSelect;

    private Date dateConcurrent;

    private String nameEmployeed = "Ingrese el número de C.I.";

    private String ci;

    private Group groupUHT;

    private Group groupYogurt;

    private Group groupChees;

    private List<ProductionTaskType>  productionTaskTypesSelectede;

    private ProductionTaskType productionTaskType;

    DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    private ProductionPlanning productionPlanning;

    public EmployeeTimeCardAction()
    {

       List<Group> groups = employeeTimeCardService.getGroupsProduction();
        for(Group group:groups)
        {
            if(group.getFullName().compareTo("PRODUCTOS LACTEOS") == 0)
            {
                groupYogurt = group;
                productionTaskTypesSelectede = employeeTimeCardService.getTaskTypeGroup(group);
            }
            if(group.getFullName().compareTo("PRODUCTOS UHT") == 0)
            {
                groupYogurt = group;
                productionTaskTypesSelectede = employeeTimeCardService.getTaskTypeGroup(group);
            }
            if(group.getFullName().compareTo("PRODUCTOS QUESOS") == 0)
            {
                groupYogurt = group;
                productionTaskTypesSelectede = employeeTimeCardService.getTaskTypeGroup(group);
            }
        }
    }

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

    public void searchEmployeed()
    {
      List<Employee> employeeList =  employeeService.getEmployeesByIdNumber(ci);
        if(employeeList.size()>0)
        {
            employeeSelect = employeeList.get(0);
            nameEmployeed = employeeSelect.getFullName();
        }
        else
            addNoFoundCIMessage();
    }

    protected void addNoFoundCIMessage() {
        facesMessages.addFromResourceBundle(StatusMessage.Severity.ERROR,
                "Common.message.idNumberPerson", ci);
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

    public Date getDateConcurrent() {
        if(dateConcurrent == null)
            dateConcurrent = new Date();

        return dateConcurrent;
    }

    public void setDateConcurrent(Date dateConcurrent) {
        this.dateConcurrent = dateConcurrent;
    }

    public String getNameEmployeed() {
        return nameEmployeed;
    }

    public void setNameEmployeed(String nameEmployeed) {
        this.nameEmployeed = nameEmployeed;
    }

    public Employee getEmployeeSelect() {
        return employeeSelect;
    }

    public void setEmployeeSelect(Employee employeeSelect) {
        this.employeeSelect = employeeSelect;
    }

    public String getCi() {
        return ci;
    }

    public void setCi(String ci) {
        this.ci = ci;
    }

    public List<ProductionTaskType> getProductionTaskTypesSelectede() {
        return productionTaskTypesSelectede;
    }

    public void setProductionTaskTypesSelectede(List<ProductionTaskType> productionTaskTypesSelectede) {
        this.productionTaskTypesSelectede = productionTaskTypesSelectede;
    }

    public Group getGroupUHT() {
        return groupUHT;
    }

    public void setGroupUHT(Group groupUHT) {
        this.groupUHT = groupUHT;
    }

    public Group getGroupYogurt() {
        return groupYogurt;
    }

    public void setGroupYogurt(Group groupYogurt) {
        this.groupYogurt = groupYogurt;
    }

    public Group getGroupChees() {
        return groupChees;
    }

    public void setGroupChees(Group groupChees) {
        this.groupChees = groupChees;
    }

    public ProductionTaskType getProductionTaskType() {
        return productionTaskType;
    }

    public void setProductionTaskType(ProductionTaskType productionTaskType) {
        this.productionTaskType = productionTaskType;
    }
}