package com.encens.khipus.model.production;

import com.encens.khipus.model.admin.Company;
import org.hibernate.annotations.Filter;

import javax.persistence.*;

@TableGenerator(name = "CollectedRawMaterial_Generator",
        table = "SECUENCIA",
        pkColumnName = "TABLA",
        valueColumnName = "VALOR",
        pkColumnValue = "ACOPIOMATERIAPRIMA",
        allocationSize = 10)

@Entity
@Table(name = "ACOPIOMATERIAPRIMA", uniqueConstraints = @UniqueConstraint(columnNames = {"IDSESIONACOPIO", "IDPRODUCTORMATERIAPRIMA"}))
@Filter(name = "companyFilter")
@EntityListeners(com.encens.khipus.model.CompanyListener.class)
public class CollectedRawMaterial implements com.encens.khipus.model.BaseModel {

    @Id
    @Column(name = "IDACOPIOMATERIAPRIMA", nullable = false)
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "CollectedRawMaterial_Generator")
    private Long id;

    @Column(name = "CANTIDAD", nullable = false, columnDefinition = "NUMBER(16,2)")
    private Double amount;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "IDPRODUCTORMATERIAPRIMA", nullable = false, updatable = false, insertable = true)
    private RawMaterialProducer rawMaterialProducer;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "IDSESIONACOPIO", nullable = false, updatable = false, insertable = true)
    private RawMaterialCollectionSession rawMaterialCollectionSession;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "IDCOMPANIA", nullable = false, updatable = false, insertable = true)
    private Company company;

    @Version
    @Column(name = "VERSION", nullable = false)
    private long version;

    @Transient
    private String rawMaterialProducerLastName;

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

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public RawMaterialCollectionSession getRawMaterialCollectionSession() {
        return rawMaterialCollectionSession;
    }

    public void setRawMaterialCollectionSession(RawMaterialCollectionSession rawMaterialCollectionSession) {
        this.rawMaterialCollectionSession = rawMaterialCollectionSession;
    }

    public com.encens.khipus.model.admin.Company getCompany() {
        return company;
    }

    public void setCompany(com.encens.khipus.model.admin.Company company) {
        this.company = company;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public String getRawMaterialProducerLastName() {
        return rawMaterialProducer.getLastName();
    }

    public void setRawMaterialProducerLastName(String rawMaterialProducerLastName) {
        this.rawMaterialProducerLastName = rawMaterialProducerLastName;
    }
}


