package com.encens.khipus.model.production;


import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.Filter;

import javax.persistence.*;
import java.util.Date;

@TableGenerator(name = "rawMaterialPayRecord_Generator",
        table = "SECUENCIA",
        pkColumnName = "TABLA",
        valueColumnName = "VALOR",
        pkColumnValue = "REGISTROPAGOMATERIAPRIMA",
        allocationSize = 10)

@Entity
@Table(name = "REGISTROPAGOMATERIAPRIMA")
@Filter(name = "companyFilter")
@EntityListeners(com.encens.khipus.model.CompanyListener.class)
public class RawMaterialPayRecord {

    @Id
    @Column(name = "IDREGISTROPAGOMATERIAPRIMA", nullable = false)
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "rawMaterialPayRecord_Generator")
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "IDDESCUENTPRODUCTMATERIAPRIMA", nullable = false, updatable = false, insertable = true)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
    private RawMaterialProducerDiscount rawMaterialProducerDiscount;

    @ManyToOne(optional = false, fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    @JoinColumn(name = "IDPLANILLAPAGOMATERIAPRIMA", nullable = false, updatable = false, insertable = true)
    private RawMaterialPayRoll rawMaterialPayRoll;

    @Column(name = "CANTIDADTOTAL", columnDefinition = "DECIMAL(24,2)" ,nullable = false)
    private double totalAmount = 0.0;

    @Column(name = "AJUSTEZONAPRODUCTIVA",columnDefinition = "DECIMAL(16,2)", nullable = false)
    private double productiveZoneAdjustment = 0.0;

    @Column(name = "DESCUENTORESERVA",columnDefinition = "DECIMAL(16,2)", nullable = false)
    private double discountReserve = 0.0;

    @Column(name = "LICENCIAIMPUESTOS", nullable = true, length = 200)
    private String taxLicense;

    @Column(name = "FECHAEXPIRALICENCIAIMPUESTO",columnDefinition = "DATE" ,nullable = true)
    private Date expirationDateTaxLicence;

    @Column(name = "FECHAINICIALICENCIAIMPUESTO",columnDefinition = "DATE" ,nullable = true)
    private Date startDateTaxLicence;

    @Column(name = "TOTALGANADO", columnDefinition = "DECIMAL(16,2)", nullable = false)
    private double earnedMoney;

    @Column(name = "TOTALPAGOACOPIO", columnDefinition = "DECIMAL(16,2)", nullable = false)
    private double totalPayCollected = 0.0;

    @Column(name = "LIQUIDOPAGABLE", columnDefinition = "DECIMAL(16,2)", nullable = false)
    private double liquidPayable = 0.0;

    @Version
    @Column(name = "VERSION", nullable = false)
    private long version;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "IDCOMPANIA", nullable = false, updatable = false, insertable = true)
    private com.encens.khipus.model.admin.Company company;

    public double getTotalPayCollected() {
        return totalPayCollected;
    }

    public void setTotalPayCollected(double totalPayCollected) {
        this.totalPayCollected = totalPayCollected;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public RawMaterialProducerDiscount getRawMaterialProducerDiscount() {
        return rawMaterialProducerDiscount;
    }

    public void setRawMaterialProducerDiscount(RawMaterialProducerDiscount rawMaterialProducerDiscount) {
        this.rawMaterialProducerDiscount = rawMaterialProducerDiscount;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public double getProductiveZoneAdjustment() {
        return productiveZoneAdjustment;
    }

    public void setProductiveZoneAdjustment(double productiveZoneAdjustment) {
        this.productiveZoneAdjustment = productiveZoneAdjustment;
    }

    public String getTaxLicense() {
        return taxLicense;
    }

    public void setTaxLicense(String taxLicense) {
        this.taxLicense = taxLicense;
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

    public RawMaterialPayRoll getRawMaterialPayRoll() {
        return rawMaterialPayRoll;
    }

    public void setRawMaterialPayRoll(RawMaterialPayRoll rawMaterialPayRoll) {
        this.rawMaterialPayRoll = rawMaterialPayRoll;
    }

    public double getLiquidPayable() {
        return liquidPayable;
    }

    public void setLiquidPayable(double liquidPayable) {
        this.liquidPayable = liquidPayable;
    }

    public double getEarnedMoney() {
        return earnedMoney;
    }

    public void setEarnedMoney(double earnedMoney) {
        this.earnedMoney = earnedMoney;
    }

    public Date getExpirationDateTaxLicence() {
        return expirationDateTaxLicence;
    }

    public void setExpirationDateTaxLicence(Date expirationDateTaxLicence) {
        this.expirationDateTaxLicence = expirationDateTaxLicence;
    }

    public Date getStartDateTaxLicence() {
        return startDateTaxLicence;
    }

    public void setStartDateTaxLicence(Date startDateTaxLicence) {
        this.startDateTaxLicence = startDateTaxLicence;
    }

    public double getDiscountReserve() {
        return discountReserve;
    }

    public void setDiscountReserve(double discountReserve) {
        this.discountReserve = discountReserve;
    }
}
