package com.encens.khipus.model.production;

import com.encens.khipus.model.BaseModel;
import com.encens.khipus.model.CompanyListener;
import com.encens.khipus.model.admin.Company;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.Filter;
import org.hibernate.validator.NotNull;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Diego
 * Date: 12/11/13
 * Time: 16:40
 * To change this template use File | Settings | File Templates.
 */

@TableGenerator(name = "Reworked_Generator",
        table = "SECUENCIA",
        pkColumnName = "TABLA",
        valueColumnName = "VALOR",
        pkColumnValue = "ESTADOARTICULO",
        allocationSize = 10)

@Entity
@Table(name = "PRODUCTOREPROCESADO")
@Filter(name = "companyFilter")
@EntityListeners(CompanyListener.class)
public class Reworked implements BaseModel {

    @Id
    @Column(name = "IDREPROCESADO", columnDefinition = "NUMBER(24,0)", nullable = false)
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "Reworked_Generator")
    private Long id;

    @Column(name = "UNIDADES", nullable = true)
    private Integer units;

    @Column(name = "VOLUMEN", nullable = true)
    private Double volume;

    @OneToOne(fetch = FetchType.LAZY)
    @PrimaryKeyJoinColumn(name = "IDMETAPRODUCTOPRODUCCION")
    private MetaProduct metaProduct;

    @ManyToOne(optional = false, fetch = FetchType.LAZY, cascade = {CascadeType.REFRESH})
    @JoinColumn(name = "IDPLANIFICACIONPRODUCCION", columnDefinition = "NUMBER(24,0)", nullable = false, updatable = false, insertable = true)
    private ProductionPlanning productionPlanningReprocessed;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "IDCOMPANIA", columnDefinition = "NUMBER(24,0)", nullable = false, updatable = false, insertable = true)
    @NotNull
    private Company company;

    @OneToMany(mappedBy = "reworked", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
    private List<ReworkedProduct> reworkedProducts = new ArrayList<ReworkedProduct>();


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

    public ProductionPlanning getProductionPlanningReprocessed() {
        return productionPlanningReprocessed;
    }

    public void setProductionPlanningReprocessed(ProductionPlanning productionPlanningReprocessed) {
        this.productionPlanningReprocessed = productionPlanningReprocessed;
    }

    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
    }
}
