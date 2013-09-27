package com.encens.khipus.model.production;

import org.hibernate.annotations.Filter;

import javax.persistence.*;
import java.util.Date;



@TableGenerator(name = "SalaryMovementProducer_Generator",
        table = "SECUENCIA",
        pkColumnName = "TABLA",
        valueColumnName = "VALOR",
        pkColumnValue = "MOVIMIENTOSALARIOPRODUCTOR",
        allocationSize = 10)

@Entity
@Table(name = "MOVIMIENTOSALARIOPRODUCTOR", uniqueConstraints = @UniqueConstraint(columnNames = {"IDMOVIMIENTOSALARIOPRODUCTOR", "IDCOMPANIA"}))
@Filter(name = "companyFilter")
@EntityListeners(com.encens.khipus.model.CompanyListener.class)
public class SalaryMovementProducer implements com.encens.khipus.model.BaseModel {

    @Id
    @Column(name = "IDMOVIMIENTOSALARIOPRODUCTOR",nullable = false)
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "SalaryMovementProducer_Generator")
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "IDPRODUCTORMATERIAPRIMA", nullable = false, updatable = false, insertable = true)
    private com.encens.khipus.model.production.RawMaterialProducer rawMaterialProducer;

    @Column(name = "YOGURT",columnDefinition = "NUMBER(16,2)", nullable = false)
    private double yogurt = 0.0;

    @Column(name = "VETERINARIO",columnDefinition = "NUMBER(16,2)", nullable = false)
    private double veterinary = 0.0;

    @Column(name = "CREDITO",columnDefinition = "NUMBER(16,2)" , nullable = false)
    private double credit = 0.0;

    @Column(name = "TACHOS",columnDefinition = "NUMBER(16,2)" ,nullable = false)
    private double cans = 0.0;

    @Column(name = "OTROSDESCUENTOS", nullable = false ,columnDefinition = "NUMBER(16,2)")
    private double otherDiscount = 0.0;

    @Column(name = "OTROSINGRESOS", nullable = false,columnDefinition = "NUMBER(16,2)")
    private double otherIncoming = 0.0;

    @Column(name = "RETENCION", nullable = false,columnDefinition = "NUMBER(16,2)")
    private double withholdingTax = 0.0;

    @Column(name = "CONCENTRADOS", nullable = false,columnDefinition = "NUMBER(16,2)")
    private double concentrated = 0.0;

    @Column(name = "FECHA", nullable = false)
    @Temporal(TemporalType.DATE)
    private Date date;

    @Version
    @Column(name = "VERSION", nullable = false)
    private long version;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "IDCOMPANIA", nullable = false, updatable = false, insertable = true)
    private com.encens.khipus.model.admin.Company company;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public RawMaterialProducer getRawMaterialProducer() {
        return rawMaterialProducer;
    }

    public void setRawMaterialProducer(RawMaterialProducer rawMaterialProducer) {
        this.rawMaterialProducer = rawMaterialProducer;
    }

    public double getConcentrated() {
        return concentrated;
    }

    public void setConcentrated(double concentrated) {
        this.concentrated = concentrated;
    }

    public double getYogurt() {
        return yogurt;
    }

    public void setYogurt(double yogurt) {
        this.yogurt = yogurt;
    }

    public double getVeterinary() {
        return veterinary;
    }

    public void setVeterinary(double veterinary) {
        this.veterinary = veterinary;
    }

    public double getCredit() {
        return credit;
    }

    public void setCredit(double credit) {
        this.credit = credit;
    }

    public double getCans() {
        return cans;
    }

    public void setCans(double cans) {
        this.cans = cans;
    }

    public double getWithholdingTax() {
        return withholdingTax;
    }

    public void setWithholdingTax(double withholdingTax) {
        this.withholdingTax = withholdingTax;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public com.encens.khipus.model.admin.Company getCompany() {
        return company;
    }

    public void setCompany(com.encens.khipus.model.admin.Company company) {
        this.company = company;
    }

    public double getOtherDiscount() {
        return otherDiscount;
    }

    public void setOtherDiscount(double otherDiscount) {
        this.otherDiscount = otherDiscount;
    }

    public double getOtherIncoming() {
        return otherIncoming;
    }

    public void setOtherIncoming(double otherIncoming) {
        this.otherIncoming = otherIncoming;
    }
}
