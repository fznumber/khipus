package com.encens.khipus.service.production;

import com.encens.khipus.framework.service.GenericServiceBean;
import com.encens.khipus.model.employees.Employee;
import com.encens.khipus.model.finances.JobContract;
import com.encens.khipus.model.production.ConfigGroup;
import com.encens.khipus.model.production.EmployeeTimeCard;
import com.encens.khipus.model.production.ProductionOrder;
import com.encens.khipus.model.production.ProductionTaskType;
import com.encens.khipus.model.warehouse.Group;
import com.encens.khipus.service.employees.JobContractService;
import com.encens.khipus.util.DateUtils;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author Ariel Siles Encias
 */

@Stateless
@Name("employeeTimeCardService")
@AutoCreate
public class EmployeeTimeCardServiceBean extends GenericServiceBean implements EmployeeTimeCardService {

    @In("#{entityManager}")
    private EntityManager em;

    @In
    private JobContractService jobContractService;

    public BigDecimal costProductionOrder(ProductionOrder productionOrder) {

        ArrayList employeeTimeCardList = new ArrayList<EmployeeTimeCard>();
        employeeTimeCardList = (ArrayList<EmployeeTimeCard>) em.createNamedQuery("EmployeeTimeCard.findEmployeeTimeCardByProductionOrder").setParameter("productionOrder", productionOrder).getResultList();

        double totalMinutes = 0;
        double totalCost = 0;

        for (int i = 0; i < employeeTimeCardList.size(); i++) {
            Employee employee = ((EmployeeTimeCard) employeeTimeCardList.get(i)).getEmployee();
            System.out.println(". . . . . EMP: " + employee.getFullName());

            Date startTime = ((EmployeeTimeCard) employeeTimeCardList.get(i)).getStartTime();
            Date endTime = ((EmployeeTimeCard) employeeTimeCardList.get(i)).getEndTime();

            long diffHours = DateUtils.differenceBetween(startTime, endTime, TimeUnit.HOURS);
            diffHours = diffHours - 1;
            long diffMinutes = DateUtils.differenceBetween(startTime, endTime, TimeUnit.MINUTES);
            diffMinutes = diffMinutes - 1;
            totalMinutes = totalMinutes + diffMinutes;

            System.out.println(". . . . . Hrs: " + startTime.toString() + " " + endTime.toString() + ". . ." + diffHours);
            System.out.println(". . . . . Mns: " + startTime.toString() + " " + endTime.toString() + ". . ." + diffMinutes);

            JobContract jobContract = jobContractService.lastJobContractByEmployee(employee);

            System.out.println(". . . . . SALARY: " + jobContract.getJob().getSalary().getBasicAmount());
            double basicMinute = ((jobContract.getJob().getSalary().getBasicAmount().doubleValue() / 30) / 8) / 60;
            System.out.println(". . . . . Salary Long: " + jobContract.getJob().getSalary().getBasicAmount().longValue());
            System.out.println(". . . . . Basic Minute: " + basicMinute);
            totalCost = totalCost + (diffMinutes * basicMinute);
            System.out.println(". . . . . COST REG: " + (diffMinutes * basicMinute));
        }

        System.out.println(". . . . . COST: " + (new BigDecimal(totalCost)).toString());
        return new BigDecimal(totalCost);
    }

    @Override
    public Double getCostPerHour(Employee employee)
    {
        JobContract jobContract = jobContractService.lastJobContractByEmployee(employee);
        Double costPerHour = ((jobContract.getJob().getSalary().getBasicAmount().doubleValue() / 30) / 8);

        return costPerHour;
    }

    @Override
    public List<ProductionTaskType> getTaskTypeGroup(Group group)
    {
        try{
            return em.createNativeQuery("SELECT productionTaskType FROM ProductionTaskType productionTaskType WHERE COD_GRU = "+group.getGroupCode())
                    .getResultList();
        }catch (NoResultException e)
        {
            return new ArrayList<ProductionTaskType>();
        }
    }

    @Override
    public List<ConfigGroup> getConfigGroupsProduction() {
        List<ConfigGroup> groups = new ArrayList<ConfigGroup>();
        try {
            groups =  em.createQuery("SELECT configGroup FROM ConfigGroup configGroup WHERE configGroup.type = :type")
                    .setParameter("type","AREA_PRODUCTOS")
                    .getResultList();
        } catch (NoResultException e) {
            return new ArrayList<ConfigGroup>();
        }

        return groups;
    }

}
