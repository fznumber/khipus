package com.encens.khipus.model.production;


import com.encens.khipus.model.BaseModel;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@TableGenerator(name = "ProducerTax_Generator",
        table = "SECUENCIA",
        pkColumnName = "TABLA",
        valueColumnName = "VALOR",
        pkColumnValue = "impuestoproductor",
        allocationSize = 10)

@Entity
@Table(name = "impuestoproductor")
public class ProducerTax implements BaseModel {

    @Id
    @Column(name = "idgimpuestoproductor", columnDefinition = "NUMBER(24)", nullable = false)
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "ProducerTax_Generator")
    private Long id;

    @Column(name = "numeroformulario",nullable = false)
    private Integer formNumber;

    @ManyToOne(cascade = {CascadeType.MERGE, CascadeType.REFRESH})
    @JoinColumn(name = "IDPRODUCTORMATERIAPRIMA", columnDefinition = "NUMBER(24,0)", nullable = false, updatable = false, insertable = true)
    private RawMaterialProducer rawMaterialProducerTax;

    @ManyToOne(cascade = {CascadeType.MERGE, CascadeType.REFRESH})
    @JoinColumn(name = "idgestionimpuesto", columnDefinition = "NUMBER(24,0)", nullable = false, updatable = false, insertable = true)
    private GestionTax gestionTax;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public GestionTax getGestionTax() {
        return gestionTax;
    }

    public void setGestionTax(GestionTax gestionTax) {
        this.gestionTax = gestionTax;
    }

    public Integer getFormNumber() {
        return formNumber;
    }

    public void setFormNumber(Integer formNumber) {
        this.formNumber = formNumber;
    }

    public RawMaterialProducer getRawMaterialProducerTax() {
        return rawMaterialProducerTax;
    }

    public void setRawMaterialProducerTax(RawMaterialProducer rawMaterialProducerTax) {
        this.rawMaterialProducerTax = rawMaterialProducerTax;
    }
}
