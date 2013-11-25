package com.encens.khipus.service.production;

import com.encens.khipus.model.employees.Employee;
import com.encens.khipus.model.production.ProductionOrder;

import javax.ejb.Local;
import java.math.BigDecimal;

/**
 * @author Ariel Siles Encias
 */
@Local
public interface EmployeeTimeCardService {
    BigDecimal costProductionOrder(ProductionOrder productionOrder);

    public Double getCostPerHour(Employee employee);
}
