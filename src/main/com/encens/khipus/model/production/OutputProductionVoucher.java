package main.com.encens.khipus.model.production;

import com.encens.hp90.model.BaseModel;
import com.encens.hp90.model.CompanyListener;
import com.encens.hp90.model.admin.Company;
import com.encens.hp90.model.warehouse.IncomingProductionOrder;
import org.hibernate.annotations.Filter;

import javax.persistence.*;

@TableGenerator(name = "OutputProductionVoucher_Generator",
        table = "SECUENCIA",
        pkColumnName = "TABLA",
        valueColumnName = "VALOR",
        pkColumnValue = "VALEPRODUCTOTERMINADO",
        allocationSize = 10)

@Entity
@Table(name = "VALEPRODUCTOTERMINADO")
@Filter(name = "companyFilter")
@EntityListeners(CompanyListener.class)
public class OutputProductionVoucher implements BaseModel {

    @Id
    @Column(name = "ID_VALE_PRODUCTO_TERMINADO", nullable = false)
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "OutputProductionVoucher_Generator")
    private Long id;

    @ManyToOne(cascade = {CascadeType.MERGE, CascadeType.REMOVE})
    @JoinColumn(name = "ID_ORDEN_PRODUCCION", nullable = false, updatable = false, insertable = true)
    private ProductionOrder productionOrder;

    @OneToOne(cascade = {CascadeType.MERGE, CascadeType.REMOVE})
    @JoinColumn(name = "ID_PRODUCTO_PROCESADO", nullable = false, updatable = false, insertable = true)
    private ProcessedProduct processedProduct;

    @OneToOne(cascade = {CascadeType.MERGE, CascadeType.REMOVE})
    @JoinColumn(name = "ID_ENTRADA_ORDEN_PRODUCCION", nullable = true, updatable = true, insertable = true)
    private IncomingProductionOrder incomingProductionOrder;

    @Column(name = "CANTIDAD_PRODUCIDA", nullable = false)
    private Double producedAmount;

    @Column(name = "OBSERVACIONES", nullable = true, length = 1500)
    private String observations;

    @Version
    @Column(name = "VERSION", nullable = false)
    private Long version;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_COMPANIA", nullable = false, updatable = false, insertable = true)
    private Company company;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ProductionOrder getProductionOrder() {
        return productionOrder;
    }

    public void setProductionOrder(ProductionOrder productionOrder) {
        this.productionOrder = productionOrder;
    }

    public IncomingProductionOrder getIncomingProductionOrder() {
        return incomingProductionOrder;
    }

    public void setIncomingProductionOrder(IncomingProductionOrder incomingProductionOrder) {
        this.incomingProductionOrder = incomingProductionOrder;
    }

    public Double getProducedAmount() {
        return producedAmount;
    }

    public void setProducedAmount(Double producedAmount) {
        this.producedAmount = producedAmount;
    }

    public String getObservations() {
        return observations;
    }

    public void setObservations(String observations) {
        this.observations = observations;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
    }

    public ProcessedProduct getProcessedProduct() {
        return processedProduct;
    }

    public void setProcessedProduct(ProcessedProduct processedProduct) {
        this.processedProduct = processedProduct;
    }
}
