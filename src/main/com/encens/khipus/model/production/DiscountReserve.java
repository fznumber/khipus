package com.encens.khipus.model.production;

import com.encens.khipus.model.BaseModel;
import com.encens.khipus.model.CompanyListener;

import javax.persistence.*;
import java.util.Date;

/**
 * Created with IntelliJ IDEA.
 * User: Diego
 * Date: 12/11/13
 * Time: 16:40
 * To change this template use File | Settings | File Templates.
 */

@TableGenerator(name = "DiscountReserve_Generator",
        table = "SECUENCIA",
        pkColumnName = "TABLA",
        valueColumnName = "VALOR",
        pkColumnValue = "DESCUENTORESERVA",
        allocationSize = 10)

@Entity
@Table(name = "DESCUENTORESERVA")
@EntityListeners(CompanyListener.class)
public class DiscountReserve implements BaseModel {

    @Id
    @Column(name = "IDDESCUENTOPRODUCTOR", columnDefinition = "NUMBER(24,0)", nullable = false)
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "DiscountReserve_Generator")
    private Long id;

    @Column(name = "MONTO",columnDefinition = "NUMBER(16,2)",nullable = false)
    private Double amount;

    @Column(name = "FECHA",columnDefinition = "DATE",nullable = false)
    private Date date;

    @OneToOne(cascade = {CascadeType.MERGE, CascadeType.REFRESH})
    @JoinColumn(name = "IDDESCUENTOPRODUCTOR", columnDefinition = "NUMBER(24,0)", nullable = false, updatable = true, insertable = true)
    private DiscountProducer discountProducer;

    @ManyToOne(cascade = {CascadeType.MERGE, CascadeType.REFRESH})
    @JoinColumn(name = "IDPRODUCTORMATERIAPRIMA", columnDefinition = "NUMBER(24,0)", nullable = false, updatable = false, insertable = true)
    private RawMaterialProducer materialProducer;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public DiscountProducer getDiscountProducer() {
        return discountProducer;
    }

    public void setDiscountProducer(DiscountProducer discountProducer) {
        this.discountProducer = discountProducer;
    }

    public RawMaterialProducer getMaterialProducer() {
        return materialProducer;
    }

    public void setMaterialProducer(RawMaterialProducer materialProducer) {
        this.materialProducer = materialProducer;
    }
}
