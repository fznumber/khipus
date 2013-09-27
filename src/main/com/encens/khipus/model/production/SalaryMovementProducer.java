package com.encens.khipus.model.production;

import com.encens.khipus.model.admin.Company;
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

    @ManyToOne(optional = false, fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @JoinColumn(name = "IDPRODUCTORMATERIAPRIMA", nullable = false, updatable = false, insertable = true)
    private RawMaterialProducer rawMaterialProducer;

    @ManyToOne(optional = false, fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @JoinColumn(name = "IDTIPOMOVIMIENTOPRODUCTOR", nullable = false, updatable = false, insertable = true)
    private TypeMovementProducer typeMovementProducer;

    @Column(name = "DESCRIPCION",nullable = false)
    private String description;

    @Column(name = "FECHA", nullable = false, columnDefinition = "DATE")
    private Date date;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "IDCOMPANIA", nullable = false, updatable = false, insertable = true)
    private Company company;

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

    public TypeMovementProducer getTypeMovementProducer() {
        return typeMovementProducer;
    }

    public void setTypeMovementProducer(TypeMovementProducer typeMovementProducer) {
        this.typeMovementProducer = typeMovementProducer;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
    }
}
