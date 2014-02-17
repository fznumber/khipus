package com.encens.khipus.model.production;

import com.encens.khipus.model.BaseModel;
import com.encens.khipus.model.CompanyListener;
import com.encens.khipus.model.admin.Company;
import org.hibernate.annotations.*;
import org.hibernate.validator.NotNull;

import javax.persistence.*;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Diego
 * Date: 12/11/13
 * Time: 16:40
 * To change this template use File | Settings | File Templates.
 */

@TableGenerator(name = "BaseProduct_Generator",
        table = "SECUENCIA",
        pkColumnName = "TABLA",
        valueColumnName = "VALOR",
        pkColumnValue = "PRODUCTOBASE",
        allocationSize = 10)

@Entity
@Table(name = "PRODUCTOBASE")
@Filter(name = "companyFilter")
@EntityListeners(CompanyListener.class)
public class BaseProduct implements BaseModel {

    @Id
    @Column(name = "IDPRODUCTOBASE", columnDefinition = "NUMBER(24,0)", nullable = false)
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "BaseProduct_Generator")
    private Long id;

    @Column(name = "UNIDADES", nullable = true)
    private Integer units;

    @Column(name = "VOLUMEN", nullable = true ,columnDefinition = "NUMBER(8,2)")
    private Double volume;

    @Column(name = "ESTADO", length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    private ProductionPlanningState state = ProductionPlanningState.PENDING;

    @OneToMany(mappedBy = "baseProduct", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
    private List<ProductProcessing> productProcessings = new ArrayList<ProductProcessing>();

    @ManyToOne(optional = true, fetch = FetchType.LAZY, cascade = {CascadeType.REFRESH})
    @JoinColumn(name = "IDPLANIFICACIONPRODUCCION", columnDefinition = "NUMBER(24,0)", nullable = false, updatable = false, insertable = true)
    private ProductionPlanning productionPlanningBase;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "IDCOMPANIA", columnDefinition = "NUMBER(24,0)", nullable = false, updatable = false, insertable = true)
    @NotNull
    private Company company;

    @OneToMany(mappedBy = "baseProduct", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
    private List<SingleProduct> singleProducts = new ArrayList<SingleProduct>();

    @Column(name = "COSTOTOTALINSUMOS", nullable = true, columnDefinition = "NUMBER(16,6)")
    private BigDecimal totalInput = new BigDecimal(0.0);

    @OneToMany(mappedBy = "baseProductInput", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
    private List<OrderInput> orderInputs = new ArrayList<OrderInput>();

    @Column(name = "CODIGO", length = 50, nullable = false)
    private String code;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getUnits() {
        return units;
    }

    public void setUnits(Integer units) {
        this.units = units;
    }

    public Double getVolume() {
        return volume;
    }

    public void setVolume(Double volume) {
        this.volume = volume;
    }

    public List<ProductProcessing> getProductProcessings() {
        return productProcessings;
    }

    public void setProductProcessings(List<ProductProcessing> productProcessings) {
        this.productProcessings = productProcessings;
    }

    public ProductionPlanning getProductionPlanningBase() {
        return productionPlanningBase;
    }

    public void setProductionPlanningBase(ProductionPlanning productionPlanningBase) {
        this.productionPlanningBase = productionPlanningBase;
    }

    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
    }

    public List<SingleProduct> getSingleProducts() {
        return singleProducts;
    }

    public void setSingleProducts(List<SingleProduct> singleProducts) {
        this.singleProducts = singleProducts;
    }

    public BigDecimal getTotalInput() {
        return totalInput;
    }

    public void setTotalInput(BigDecimal totalInput) {
        this.totalInput = totalInput;
    }

    public List<OrderInput> getOrderInputs() {
        return orderInputs;
    }

    public void setOrderInputs(List<OrderInput> orderInputs) {
        this.orderInputs = orderInputs;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public ProductionPlanningState getState() {
        return state;
    }

    public void setState(ProductionPlanningState state) {
        this.state = state;
    }
}
