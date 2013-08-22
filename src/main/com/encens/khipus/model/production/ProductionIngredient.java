package com.encens.khipus.model.production;

import javax.persistence.*;

/**
 * Created with IntelliJ IDEA.
 * User: david
 * Date: 6/17/13
 * Time: 12:43 PM
 * To change this template use File | Settings | File Templates.
 */

@TableGenerator(name = "ProductIngredient_Generator",
        table = "SECUENCIA",
        pkColumnName = "TABLA",
        valueColumnName = "VALOR",
        pkColumnValue = "INGREDIENTEPRODUCCION",
        allocationSize = 10)

@Entity
@Table(name = "INGREDIENTEPRODUCCION")
public class ProductionIngredient implements com.encens.khipus.model.BaseModel {

    @Id
    @Column(name = "IDINGREDIENTEPRODUCCION",columnDefinition = "NUMBER(24,0)" , nullable = false)
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "ProductIngredient_Generator")
    private Long id;

    @Transient
    private double amount;

    @Column(name = "FORMULAMATEMATICA", nullable = false, length = 500)
    private String mathematicalFormula;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "IDCOMPOSICIONPRODUCTO",columnDefinition = "NUMBER(24,0)" , nullable = false, updatable = false, insertable = true)
    private ProductComposition productComposition;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "IDMETAPRODUCTOPRODUCCION",columnDefinition = "NUMBER(24,0)" , nullable = false, updatable = false, insertable = true)
    private MetaProduct metaProduct;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public ProductComposition getProductComposition() {
        return productComposition;
    }

    public void setProductComposition(ProductComposition productComposition) {
        this.productComposition = productComposition;
    }

    public MetaProduct getMetaProduct() {
        return metaProduct;
    }

    public void setMetaProduct(MetaProduct metaProduct) {
        this.metaProduct = metaProduct;
    }

    public String getMathematicalFormula() {
        return mathematicalFormula;
    }

    public void setMathematicalFormula(String mathematicalFormula) {
        this.mathematicalFormula = mathematicalFormula;
    }
}
