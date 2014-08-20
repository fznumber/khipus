package com.encens.khipus.model.production;

import com.encens.khipus.model.BaseModel;
import com.encens.khipus.model.CompanyListener;
import com.encens.khipus.model.warehouse.ProductItem;
import org.hibernate.annotations.Filter;
import org.hibernate.validator.Length;

import javax.persistence.*;
import java.util.Date;

/**
 * Created with IntelliJ IDEA.
 * User: Diego
 * Date: 12/11/13
 * Time: 16:40
 * To change this template use File | Settings | File Templates.
 */

@TableGenerator(name = "DiscountProducer_Generator",
        table = "SECUENCIA",
        pkColumnName = "TABLA",
        valueColumnName = "VALOR",
        pkColumnValue = "DESCUENTOPRODUCTOR",
        allocationSize = 10)

@Entity
@Table(name = "DESCUENTOPRODUCTOR")
public class DiscountProducer implements BaseModel {

    @Id
    @Column(name = "IDDESCUENTOPRODUCTOR", columnDefinition = "NUMBER(24,0)", nullable = false)
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "DiscountProducer_Generator")
    private Long id;

    @Column(name = "PROMEDIOLECHE",columnDefinition = "NUMBER(16,2)",nullable = false)
    private Double average;

    @Column(name = "RESERVA",columnDefinition = "NUMBER(8,5)",nullable = false)
    private Double reserve;

    @Column(name = "RESERVAQUICENTA",columnDefinition = "NUMBER(8,5)",nullable = false)
    private Double reserveFortnight;

    @Column(name = "FECHAINI",columnDefinition = "DATE",nullable = false)
    private Date startDate;

    @Column(name = "FECHAFIN",columnDefinition = "DATE",nullable = false)
    private Date endDate;

    @Column(name = "MONTOTOTALMN",columnDefinition = "NUMBER(16,2)",nullable = false)
    private Double amountMN;

    @Column(name = "MONTOTOTALME",columnDefinition = "NUMBER(16,2)",nullable = false)
    private Double amountME;

    @Column(name = "TC",columnDefinition = "NUMBER(5,2)",nullable = false)
    private Double tc;

    @Column(name = "ESTADO",columnDefinition = "VARCHAR(10)",nullable = false)
    private String state;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Double getAverage() {
        return average;
    }

    public void setAverage(Double average) {
        this.average = average;
    }

    public Double getReserve() {
        return reserve;
    }

    public void setReserve(Double reserve) {
        this.reserve = reserve;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public Double getAmountMN() {
        return amountMN;
    }

    public void setAmountMN(Double amountMN) {
        this.amountMN = amountMN;
    }

    public Double getAmountME() {
        return amountME;
    }

    public void setAmountME(Double amountME) {
        this.amountME = amountME;
    }

    public Double getTc() {
        return tc;
    }

    public void setTc(Double tc) {
        this.tc = tc;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public Double getReserveFortnight() {
        return reserveFortnight;
    }

    public void setReserveFortnight(Double reserveFortnight) {
        this.reserveFortnight = reserveFortnight;
    }
}
