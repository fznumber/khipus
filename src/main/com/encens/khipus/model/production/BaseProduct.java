package com.encens.khipus.model.production;

import com.encens.khipus.model.BaseModel;
import com.encens.khipus.model.CompanyListener;
import com.encens.khipus.model.admin.Company;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.Filter;
import org.hibernate.validator.NotNull;

import javax.persistence.*;
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

    @OneToOne(fetch = FetchType.LAZY)
    @PrimaryKeyJoinColumn(name = "IDMETAPRODUCTOPRODUCCION")
    private MetaProduct metaProduct;

    @ManyToOne(optional = false, fetch = FetchType.LAZY, cascade = {CascadeType.REFRESH})
    @JoinColumn(name = "IDPLANIFICACIONPRODUCCION", columnDefinition = "NUMBER(24,0)", nullable = false, updatable = false, insertable = true)
    private ProductionPlanning productionPlanningBase;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "IDCOMPANIA", columnDefinition = "NUMBER(24,0)", nullable = false, updatable = false, insertable = true)
    @NotNull
    private Company company;

    @OneToMany(mappedBy = "baseProduct", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
    private List<SingleProduct> singleProducts = new ArrayList<SingleProduct>();

    @Column(name = "COSTOTOTALINSUMOS", nullable = true, columnDefinition = "NUMBERNUMBER(16,6)")
    private BigDecimal totalInput = new BigDecimal(0.0);

    @OneToMany(mappedBy = "baseProductInput", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
    private List<OrderInput> orderMaterials = new ArrayList<OrderInput>();

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

    public MetaProduct getMetaProduct() {
        return metaProduct;
    }

    public void setMetaProduct(MetaProduct metaProduct) {
        this.metaProduct = metaProduct;
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

    public List<OrderInput> getOrderMaterials() {
        return orderMaterials;
    }

    public void setOrderMaterials(List<OrderInput> orderMaterials) {
        this.orderMaterials = orderMaterials;
    }
}
