package main.com.encens.khipus.model.warehouse;

import com.encens.hp90.model.BaseModel;
import com.encens.hp90.model.CompanyListener;
import com.encens.hp90.model.admin.Company;
import com.encens.hp90.model.production.OutputProductionVoucher;
import com.encens.hp90.model.production.ProductionOrder;
import org.hibernate.annotations.Filter;

import javax.persistence.*;


@TableGenerator(name = "IncomingProductionOrder_Generator",
        table = "SECUENCIA",
        pkColumnName = "TABLA",
        valueColumnName = "VALOR",
        pkColumnValue = "ENTRADAORDENPRODUCCION",
        allocationSize = 10)

@Entity
@Table(name = "ENTRADAORDENPRODUCCION")
@Filter(name = "companyFilter")
@EntityListeners(CompanyListener.class)
public class IncomingProductionOrder implements BaseModel {

    @Id
    @Column(name = "ID_ENTRADA_ORDEN_PRODUCCION", nullable = false)
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "IncomingProductionOrder_Generator")
    private Long id;

    @Column(name = "OBSERVACION_ENTREGA", nullable = true, length = 1500)
    private String deliveredObservation;

    @Column(name = "OBSERVACION_RECEPCION", nullable = true, length = 1500)
    private String receivedObservation;

    @Column(name = "CANTIDAD_ENTREGADA", nullable = false)
    private double deliveredAmount;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_COMPANIA", nullable = false, updatable = false, insertable = true)
    private Company company;

    @OneToOne(optional = true, fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @JoinColumn(name = "ID_INVENTARIO_PRODUCTO_TERMINADO", nullable = true, updatable = true, insertable = true)
    private FinishedGoodsInventory finishedGoodsInventory;

    @OneToOne(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH}, mappedBy = "incomingProductionOrder")
    private OutputProductionVoucher outputProductionVoucher;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_REGISTRO_TRANSFERENCIA_PRODUCTOS", nullable = false, updatable = false, insertable = true)
    private ProductionTransferLog productionTransferLog;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
    }

    public FinishedGoodsInventory getFinishedGoodsInventory() {
        return finishedGoodsInventory;
    }

    public void setFinishedGoodsInventory(FinishedGoodsInventory finishedGoodsInventory) {
        this.finishedGoodsInventory = finishedGoodsInventory;
    }

    public String getDeliveredObservation() {
        return deliveredObservation;
    }

    public void setDeliveredObservation(String deliveredObservation) {
        this.deliveredObservation = deliveredObservation;
    }

    public String getReceivedObservation() {
        return receivedObservation;
    }

    public void setReceivedObservation(String receivedObservation) {
        this.receivedObservation = receivedObservation;
    }

    public double getDeliveredAmount() {
        return deliveredAmount;
    }

    public void setDeliveredAmount(double deliveredAmount) {
        this.deliveredAmount = deliveredAmount;
    }

    public OutputProductionVoucher getOutputProductionVoucher() {
        return outputProductionVoucher;
    }

    public void setOutputProductionVoucher(OutputProductionVoucher outputProductionVoucher) {
        this.outputProductionVoucher = outputProductionVoucher;
    }

    public ProductionTransferLog getProductionTransferLog() {
        return productionTransferLog;
    }

    public void setProductionTransferLog(ProductionTransferLog productionTransferLog) {
        this.productionTransferLog = productionTransferLog;
    }
}
