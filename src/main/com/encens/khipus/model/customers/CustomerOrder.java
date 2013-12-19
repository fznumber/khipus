package com.encens.khipus.model.customers;

import com.encens.khipus.model.BaseModel;
import com.encens.khipus.model.CompanyListener;
import com.encens.khipus.model.contacts.Address;
import com.encens.khipus.model.contacts.Zone;
import com.encens.khipus.model.employees.Employee;
import com.encens.khipus.model.warehouse.ProductItem;
import com.encens.khipus.util.Constants;
import org.hibernate.annotations.Filter;
import org.hibernate.validator.Length;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Created with IntelliJ IDEA.
 * User: Diego
 * Date: 12/11/13
 * Time: 16:40
 * To change this template use File | Settings | File Templates.
 */

@TableGenerator(name = "CustomerOrder_Generator",
        table = "SECUENCIA",
        pkColumnName = "TABLA",
        valueColumnName = "VALOR",
        pkColumnValue = "PEDIDOS",
        allocationSize = 10)

@Entity
@Table(name = "PEDIDOS",schema = Constants.CASHBOX_SCHEMA)
@Filter(name = "companyFilter")
@EntityListeners(CompanyListener.class)
public class CustomerOrder implements BaseModel {

    @EmbeddedId
    private CustomerOrderPK id = new CustomerOrderPK();

    @Column(name = "PEDIDO", nullable = false)
    private String order;

    @Column(name = "ID", nullable = false)
    private String orderID;

    @Column(name = "ID1", nullable = false)
    private Integer getOrderID1;

    @Column(name = "DESCRIPCION")
    private String description;

    @Column(name = "ESTADO_PEDIDO")
    private String estate;

    @Column(name = "TIPO_PEDIDO")
    private String type;

    @Temporal(value = TemporalType.TIMESTAMP)
    @Column(name = "FECHA_PEDIDO")
    private Date date;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "IDEMPLEADO", nullable = false, updatable = false)
    private Employee employee;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "IDDIRECCION", nullable = false, updatable = false)
    private Address address;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "IDZONA", nullable = false, updatable = false)
    private Zone zone;

    @Column(name = "TOTAL")
    private BigDecimal total = BigDecimal.ZERO;

    @Temporal(value = TemporalType.TIMESTAMP)
    @Column(name = "FECHA_ENTREGA")
    private Date dateDelicery;

    @Temporal(value = TemporalType.TIMESTAMP)
    @Column(name = "FECHA_A_PAGAR")
    private Date dateToPay;

    @Column(name = "OBSERVACION")
    private String observation;

    @Column(name = "FACTURA")
    private String estateInvoice;

    @Column(name = "SUPERVISOR")
    private Long supervisorsID;

    @Column(name = "DISTRIBUIDOR")
    private Long distributorID;

    public CustomerOrderPK getId() {
        return id;
    }

    public void setId(CustomerOrderPK id) {
        this.id = id;
    }

    public String getOrder() {
        return order;
    }

    public void setOrder(String order) {
        this.order = order;
    }

    public String getOrderID() {
        return orderID;
    }

    public void setOrderID(String orderID) {
        this.orderID = orderID;
    }

    public Integer getGetOrderID1() {
        return getOrderID1;
    }

    public void setGetOrderID1(Integer getOrderID1) {
        this.getOrderID1 = getOrderID1;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getEstate() {
        return estate;
    }

    public void setEstate(String estate) {
        this.estate = estate;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Employee getEmployee() {
        return employee;
    }

    public void setEmployee(Employee employee) {
        this.employee = employee;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public Zone getZone() {
        return zone;
    }

    public void setZone(Zone zone) {
        this.zone = zone;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public Date getDateDelicery() {
        return dateDelicery;
    }

    public void setDateDelicery(Date dateDelicery) {
        this.dateDelicery = dateDelicery;
    }

    public Date getDateToPay() {
        return dateToPay;
    }

    public void setDateToPay(Date dateToPay) {
        this.dateToPay = dateToPay;
    }

    public String getObservation() {
        return observation;
    }

    public void setObservation(String observation) {
        this.observation = observation;
    }

    public String getEstateInvoice() {
        return estateInvoice;
    }

    public void setEstateInvoice(String estateInvoice) {
        this.estateInvoice = estateInvoice;
    }

    public Long getSupervisorsID() {
        return supervisorsID;
    }

    public void setSupervisorsID(Long supervisorsID) {
        this.supervisorsID = supervisorsID;
    }

    public Long getDistributorID() {
        return distributorID;
    }

    public void setDistributorID(Long distributorID) {
        this.distributorID = distributorID;
    }
}
