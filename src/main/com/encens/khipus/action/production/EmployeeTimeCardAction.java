package com.encens.khipus.action.production;

import com.encens.khipus.exception.ConcurrencyException;
import com.encens.khipus.exception.EntryDuplicatedException;
import com.encens.khipus.framework.action.GenericAction;
import com.encens.khipus.model.admin.AdministrativeEventType;
import com.encens.khipus.model.admin.User;
import com.encens.khipus.model.employees.Employee;
import com.encens.khipus.model.production.*;
import com.encens.khipus.model.warehouse.Group;
import com.encens.khipus.model.warehouse.SubGroup;
import com.encens.khipus.service.employees.EmployeeService;
import com.encens.khipus.service.production.EmployeeTimeCardService;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.*;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage;

import javax.persistence.EntityManager;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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

    @Out(required = false)
    @In(required = false)
    private User currentUser;

    @In
    protected FacesMessages facesMessages;

    private Employee employeeSelect;

    private Date dateConcurrent;

    private String nameEmployeed = "Ingrese el número de C.I.";

    private String ci;

    private Group groupUHT;

    private Group groupYogurt;

    private Group groupChees;

    private Group selectGroup;

    private List<ProductionTaskType>  productionTaskTypesSelectede;

    private List<SubGroup> subGroups;

    private List<Group> groupList = new ArrayList<Group>();

    private SubGroup subGroup;

    private ProductionTaskType productionTaskType;

    DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    private ProductionPlanning productionPlanning;

    @Factory(value = "employeeChoise", scope = ScopeType.STATELESS)
    public Employee initEmployee() {
        return employeeSelect;
    }

    @Factory(value = "employeeTimeCard", scope = ScopeType.STATELESS)
    public EmployeeTimeCard initEmployeeTimeCard() {
        return getInstance();
    }

    public void register() {
        try {
            searchEmployeed();
            EmployeeTimeCard timeCard = getInstance();
            timeCard.setCostPerHour(employeeTimeCardService.getCostPerHour(employeeSelect));
            timeCard.setProductionTaskType(productionTaskType);
            timeCard.setSubGroup(subGroup);
            EmployeeTimeCard lastMark = employeeTimeCardService.getLastEmployeeTimeCard(employeeSelect);
            if(lastMark != null)
            {
                try {
                    lastMark.setEndTime(new Date());
                    getService().update(lastMark);
                } catch (ConcurrencyException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
            }
            timeCard.setStartTime(new Date());
            timeCard.setGroupCode(subGroup.getGroupCode());
            timeCard.setEmployee(employeeSelect);
            timeCard.setSubGroupCode(subGroup.getSubGroupCode());
            timeCard.setCompany(currentUser.getCompany());
            timeCard.setCompanyNumber(subGroup.getc)
            getService().create(timeCard);
            addCreatedMessage();
        } catch (EntryDuplicatedException e) {
            addDuplicatedMessage();
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
        if(ci!=null)
        facesMessages.addFromResourceBundle(StatusMessage.Severity.ERROR,
                "Common.message.idNumberPerson", ci);
        else
        facesMessages.addFromResourceBundle(StatusMessage.Severity.INFO,
                    "Common.message.idNumberPersonInput");
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
        if(employeeSelect == null)
            searchEmployeed();

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

    @Factory(value = "listTaskTypes", scope = ScopeType.STATELESS)
    public List<ProductionTaskType> getProductionTaskTypesSelectede() {
        /*if(productionTaskTypesSelectede == null)
            productionTaskTypesSelectede = employeeTimeCardService.getTaskType();*/

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

    public void selectGroupYogurt()
    {
        if(groupYogurt == null)
        {
            List<ConfigGroup> groups = employeeTimeCardService.getConfigGroupsProduction();
            for(ConfigGroup configGroup:groups)
            {
                if(configGroup.getGroup().getFullName().compareTo("6 - PRODUCTOS LACTEOS") == 0)
                {
                    groupYogurt = configGroup.getGroup();
                }
            }
        }
        if(productionTaskTypesSelectede != null)
            productionTaskTypesSelectede.clear();

        productionTaskTypesSelectede = employeeTimeCardService.getTaskTypeGroup(groupYogurt);
        subGroups = groupYogurt.getSubGroupList();
    }

    public Group getSelectGroup() {
        return selectGroup;
    }

    public void setSelectGroup(Group selectGroup) {
        this.selectGroup = selectGroup;
    }

    public void setSubGroupTask()
    {
        productionTaskTypesSelectede = employeeTimeCardService.getTaskTypeGroup(selectGroup);
        subGroups = selectGroup.getSubGroupList();
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

    public List<SubGroup> getSubGroups() {
        return subGroups;
    }

    public void setSubGroups(List<SubGroup> subGroups) {
        this.subGroups = subGroups;
    }

    public SubGroup getSubGroup() {
        return subGroup;
    }

    public void setSubGroup(SubGroup subGroup) {
        this.subGroup = subGroup;
    }

    public List<Group> getGroupList() {
        if(groupList.size() == 0)
        {
            List<ConfigGroup> groups = employeeTimeCardService.getConfigGroupsProduction();
            for(ConfigGroup configGroup:groups)
            {
                groupList.add(configGroup.getGroup());
            }

        }
        return groupList;
    }

    public void setGroupList(List<Group> groupList) {
        this.groupList = groupList;
    }
}