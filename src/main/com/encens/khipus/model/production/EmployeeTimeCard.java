package com.encens.khipus.model.production;

import com.encens.khipus.model.BaseModel;
import com.encens.khipus.model.CompanyListener;
import com.encens.khipus.model.UpperCaseStringListener;
import com.encens.khipus.model.admin.Company;
import com.encens.khipus.model.employees.Employee;
import com.encens.khipus.util.Constants;
import org.hibernate.annotations.Filter;

import javax.persistence.*;
import java.util.Date;

/**
 * Entity for Employee Time CArd
 *
 * @author Ariel Siles Encinas
 * @version 1.1.5
 */

@TableGenerator(schema = Constants.KHIPUS_SCHEMA, name = "EmployeeTimeCard.tableGenerator",
        table = Constants.SEQUENCE_TABLE_NAME,
        pkColumnName = Constants.SEQUENCE_TABLE_PK_COLUMN_NAME,
        valueColumnName = Constants.SEQUENCE_TABLE_VALUE_COLUMN_NAME,
        pkColumnValue = "employeeTimeCard",
        allocationSize = Constants.SEQUENCE_ALLOCATION_SIZE)

@NamedQueries(
        {
                @NamedQuery(name = "EmployeeTimeCard.findEmployeeTimeCardByProductionOrder", query = "select e from EmployeeTimeCard e where e.productionOrder=:productionOrder ")
        })
@Entity
@Filter(name = com.encens.khipus.util.Constants.COMPANY_FILTER_NAME)
@EntityListeners({CompanyListener.class, UpperCaseStringListener.class})
@Table(schema = Constants.KHIPUS_SCHEMA, name = "TARJETATIEMPOEMPLEADO")
public class EmployeeTimeCard implements BaseModel {

    @Id
    @Column(name = "IDTARJETATIEMPOEMPLEADO", nullable = false)
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "EmployeeTimeCard.tableGenerator")
    private Long id;

    @Column(name = "HORAINICIO", nullable = false, updatable = false)
    @Temporal(TemporalType.TIME)
    private Date startTime;

    @Column(name = "HORAFIN", nullable = false, updatable = false)
    @Temporal(TemporalType.TIME)
    private Date endTime;

    @Column(name = "COSTOPORHORA", nullable = true, columnDefinition = "NUMBER(16,2)")
    private Double costPerHour;

    @Column(name = "DESCRIPCION", nullable = true)
    @Lob
    private String description;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "IDEMPLEADO", nullable = false, updatable = false)
    private Employee employee;

    @ManyToOne(optional = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "IDORDENPRODUCCION", nullable = true, updatable = false)
    private ProductionOrder productionOrder;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "IDTIPOTAREAPROD", nullable = false, updatable = false)
    private ProductionTaskType productionTaskType;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "IDCOMPANIA", nullable = false, updatable = false, insertable = true)
    private Company company;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Employee getEmployee() {
        return employee;
    }

    public void setEmployee(Employee employee) {
        this.employee = employee;
    }

    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
    }

    public ProductionOrder getProductionOrder() {
        return productionOrder;
    }

    public void setProductionOrder(ProductionOrder productionOrder) {
        this.productionOrder = productionOrder;
    }

    public ProductionTaskType getProductionTaskType() {
        return productionTaskType;
    }

    public void setProductionTaskType(ProductionTaskType productionTaskType) {
        this.productionTaskType = productionTaskType;
    }

    public Double getCostPerHour() {
        return costPerHour;
    }

    public void setCostPerHour(Double costPerHour) {
        this.costPerHour = costPerHour;
    }
}
