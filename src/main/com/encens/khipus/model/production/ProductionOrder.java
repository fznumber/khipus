package com.encens.khipus.model.production;

import com.encens.khipus.model.BaseModel;
import com.encens.khipus.model.CompanyListener;
import com.encens.khipus.model.admin.Company;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.Filter;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@NamedQueries({
        @NamedQuery(name = "ProductionOrder.findBySubDateOnCode",
                query = "select productionOrder.code " +
                        "from ProductionOrder productionOrder " +
                        "where productionOrder.code like concat(:seed, '%')"),
        @NamedQuery(name = "ProductionOrder.findById", query = "Select p from ProductionOrder p where p.id=:id")
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
@EntityListeners(CompanyListener.class)
public class ProductionOrder implements BaseModel {

    @Id
    @Column(name = "IDORDENPRODUCCION", columnDefinition = "NUMBER(24,0)", nullable = false)
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "ProductionOrder_Generator")
    private Long id;

    @Column(name = "CODIGO", length = 50, nullable = false)
    private String code;

    @Column(name = "ESTADOORDEN", length = 20, nullable = true)
    @Enumerated(EnumType.STRING)
    private ProductionPlanningState estateOrder = ProductionPlanningState.PENDING;

    @Column(name = "CANTIDADESPERADA", nullable = false, columnDefinition = "NUMBER(24,0)")
    private Double expendAmount;

    @Column(name = "PESOCONTENEDOR", nullable = false, columnDefinition = "NUMBER(24,0)")
    private Double containerWeight;

    @Column(name = "CANTIDADPRODUCIDA", nullable = false, columnDefinition = "NUMBER(24,0)")
    private Double producedAmount = 0.0;

    @Column(name = "PRECIOTOTALMATERIAL", nullable = true, columnDefinition = "NUMBER(24,0)")
    private Double totalPriceMaterial = 0.0;

    @Column(name = "PRECIOTOTALINSUMO", nullable = true, columnDefinition = "NUMBER(24,0)")
    private Double totalPriceInput = 0.0;

    @Column(name = "PRECIOTOTALMANOOBRA", nullable = true, columnDefinition = "NUMBER(24,0)")
    private Double totalPriceJourney = 0.0;

    @Column(name = "COSTOTOALPRODUCCION", nullable = true, columnDefinition = "NUMBER(24,0)")
    private Double totalCostProduction = 0.0;

    @Transient
    private Double milk;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "IDCOMPANIA", columnDefinition = "NUMBER(24,0)", nullable = false, updatable = false, insertable = true)
    private Company company;

    @ManyToOne(optional = false, fetch = FetchType.LAZY, cascade = {CascadeType.REFRESH})
    @JoinColumn(name = "IDPLANIFICACIONPRODUCCION", columnDefinition = "NUMBER(24,0)", nullable = false, updatable = false, insertable = true)
    private ProductionPlanning productionPlanning;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "productionOrder", cascade = CascadeType.ALL)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
    private List<InputProductionVoucher> inputProductionVoucherList = new ArrayList<InputProductionVoucher>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "productionOrder", cascade = CascadeType.ALL)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
    private List<OutputProductionVoucher> outputProductionVoucherList = new ArrayList<OutputProductionVoucher>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "productionOrder", cascade = CascadeType.ALL)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
    private List<OrderMaterial> orderMaterials = new ArrayList<OrderMaterial>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "productionOrder", cascade = CascadeType.ALL)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
    private List<OrderInput> orderInputs = new ArrayList<OrderInput>();

    @ManyToOne(optional = false, fetch = FetchType.LAZY, cascade = {CascadeType.REFRESH})
    @JoinColumn(name = "IDCOMPOSICIONPRODUCTO", columnDefinition = "NUMBER(24,0)", nullable = false, updatable = true, insertable = true)
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

    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
    }

    public Double getExpendAmount() {
        return expendAmount;
    }

    public void setExpendAmount(Double producingAmount) {
        this.expendAmount = producingAmount;
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

    public Double getProducedAmount() {
        return producedAmount;
    }

    public void setProducedAmount(Double supposedAmount) {
        this.producedAmount = supposedAmount;
    }

    public Double getMilk() {
        return milk;
    }

    public void setMilk(Double milk) {
        this.milk = milk;
    }

    public Double getMountMilk() {
        Double mount = 0.0;
        if (this.productComposition != null) {
            List<ProductionIngredient> ingredients = this.productComposition.getProductionIngredientList();

            for (ProductionIngredient ingredient : ingredients) {
                if (ingredient.getMetaProduct().getName().compareTo("LECHE CRUDA") == 0) {
                    mount = ingredient.getAmount();
                }
            }
        }
        return mount;
    }

    public List<OrderMaterial> getOrderMaterials() {

        if (orderMaterials == null) orderMaterials = new ArrayList<OrderMaterial>();

        return orderMaterials;
    }

    public void setOrderMaterials(List<OrderMaterial> orders) {
        orderMaterials.clear();
        if (orders != null) {
            this.orderMaterials.addAll(orders);
        }
    }

    public Double getTotalPriceMaterial() {
        return totalPriceMaterial;
    }

    public void setTotalPriceMaterial(Double totalPriceMaterial) {
        this.totalPriceMaterial = totalPriceMaterial;
    }

    public Double getTotalPriceInput() {
        return totalPriceInput;
    }

    public void setTotalPriceInput(Double totalPriceInput) {
        this.totalPriceInput = totalPriceInput;
    }

    public Double getTotalPriceJourney() {
        return totalPriceJourney;
    }

    public void setTotalPriceJourney(Double totalPriceJourney) {
        this.totalPriceJourney = totalPriceJourney;
    }

    public Double getTotalCostProduction() {
        return totalCostProduction;
    }

    public void setTotalCostProduction(Double totalCostProduction) {
        this.totalCostProduction = totalCostProduction;
    }

    public String getOrderProduct() {
        return this.code + " " + this.productComposition.getProcessedProduct().getProductItem().getName();

    }

    public ProductionPlanningState getEstateOrder() {
        return estateOrder;
    }

    public void setEstateOrder(ProductionPlanningState estateOrder) {
        this.estateOrder = estateOrder;
    }

    public List<OrderInput> getOrderInputs() {
        return orderInputs;
    }

    public void setOrderInputs(List<OrderInput> orderInputs) {
        this.orderInputs = orderInputs;
    }
}
