package main.com.encens.khipus.model.production;

import org.hibernate.annotations.Filter;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@NamedQueries({
        @NamedQuery(name = "ProductionOrder.findBySubDateOnCode",
                    query = "select productionOrder.code " +
                            "from ProductionOrder productionOrder " +
                            "where productionOrder.code like concat(:seed, '%')")
})

@TableGenerator(name = "ProductionOrder_Generator",
        table = "SECUENCIA",
        pkColumnName = "TABLA",
        valueColumnName = "VALOR",
        pkColumnValue = "ORDENPRODUCCION",
        allocationSize = 10)

@Entity
@Table(name = "ORDENPRODUCCION", uniqueConstraints = @UniqueConstraint(columnNames = {"CODIGO", "IDCOMPANIA"}))
@Filter(name = "companyFilter")
@EntityListeners(com.encens.khipus.model.CompanyListener.class)
public class ProductionOrder implements com.encens.khipus.model.BaseModel {

    @Id
    @Column(name = "IDORDENPRODUCCION", nullable = false)
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "ProductionOrder_Generator")
    private Long id;

    @Column(name = "CODIGO", length = 50, nullable = false)
    private String code;

    @Column(name = "CANTIDADPRODUCIR", nullable = false)
    private Double producingAmount;

    @Column(name = "PESOCONTENEDOR", nullable = false)
    private Double containerWeight;

    @Column(name = "TEORICOOBTENIDO", nullable = false)
    private Double supposedAmount;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "IDCOMPANIA", nullable = false, updatable = false, insertable = true)
    private com.encens.khipus.model.admin.Company company;

    @ManyToOne(optional = false, fetch = FetchType.LAZY, cascade = {CascadeType.REFRESH})
    @JoinColumn(name = "IDPLANIFICACIONPRODUCCION", nullable = false, updatable = false, insertable = true)
    private ProductionPlanning productionPlanning;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "productionOrder", cascade = CascadeType.ALL)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
    private List<InputProductionVoucher> inputProductionVoucherList = new ArrayList<InputProductionVoucher>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "productionOrder", cascade = CascadeType.ALL)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
    private List<OutputProductionVoucher> outputProductionVoucherList = new ArrayList<OutputProductionVoucher>();

    @ManyToOne(optional = false, fetch = FetchType.LAZY, cascade = {CascadeType.REFRESH})
    @JoinColumn(name = "IDCOMPOSICIONPRODUCTO", nullable = false, updatable = true,  insertable = true)
    private ProductComposition productComposition;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public ProductionPlanning getProductionPlanning() {
        return productionPlanning;
    }

    public void setProductionPlanning(ProductionPlanning productionPlanning) {
        this.productionPlanning = productionPlanning;
    }

    public ProductComposition getProductComposition() {
        return productComposition;
    }

    public void setProductComposition(ProductComposition productComposition) {
        this.productComposition = productComposition;
    }

    public com.encens.khipus.model.admin.Company getCompany() {
        return company;
    }

    public void setCompany(com.encens.khipus.model.admin.Company company) {
        this.company = company;
    }

    public Double getProducingAmount() {
        return producingAmount;
    }

    public void setProducingAmount(Double producingAmount) {
        this.producingAmount = producingAmount;
    }

    public List<InputProductionVoucher> getInputProductionVoucherList() {
        return inputProductionVoucherList;
    }

    public void setInputProductionVoucherList(List<InputProductionVoucher> inputProductionVoucherList) {
        this.inputProductionVoucherList = inputProductionVoucherList;
    }

    public List<OutputProductionVoucher> getOutputProductionVoucherList() {
        return outputProductionVoucherList;
    }

    public void setOutputProductionVoucherList(List<OutputProductionVoucher> outputProductionVoucherList) {
        this.outputProductionVoucherList = outputProductionVoucherList;
    }

    public Double getContainerWeight() {
        return containerWeight;
    }

    public void setContainerWeight(Double containerWeight) {
        this.containerWeight = containerWeight;
    }

    public Double getSupposedAmount() {
        return supposedAmount;
    }

    public void setSupposedAmount(Double supposedAmount) {
        this.supposedAmount = supposedAmount;
    }
}
