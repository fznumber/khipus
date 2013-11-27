package com.encens.khipus.service.production;

import com.encens.khipus.model.employees.Employee;
import com.encens.khipus.model.production.ProductionOrder;
import com.encens.khipus.model.production.ProductionTaskType;
import com.encens.khipus.model.warehouse.Group;

import javax.ejb.Local;
import java.math.BigDecimal;
import java.util.List;

/**
 * @author Ariel Siles Encias
 */
@Local
public interface EmployeeTimeCardService {
    BigDecimal costProductionOrder(ProductionOrder productionOrder);

    public Double getCostPerHour(Employee employee);

    public List<Group> getGroupsProduction();

    public List<ProductionTaskType> getTaskTypeGroup(Group group);
}
