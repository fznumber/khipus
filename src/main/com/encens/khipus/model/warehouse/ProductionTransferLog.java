package main.com.encens.khipus.model.warehouse;


import com.encens.hp90.model.BaseModel;
import com.encens.hp90.model.CompanyListener;
import com.encens.hp90.model.admin.Company;
import org.hibernate.annotations.*;

import javax.persistence.*;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@TableGenerator(name = "ProductionTransferLog_Generator",
        table = "SECUENCIA",
        pkColumnName = "TABLA",
        valueColumnName = "VALOR",
        pkColumnValue = "REGISTROTRANSFERENCIAPRODUCTOS",
        allocationSize = 10)

@Entity
@Table(name = "REGISTROTRANSFERENCIAPRODUCTOS")
@Filter(name = "companyFilter")
@EntityListeners(CompanyListener.class)
public class ProductionTransferLog implements BaseModel {

    @Id
    @Column(name = "ID_REGISTRO_TRANSFERENCIA_PRODUCTOS", nullable = false)
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "ProductionTransferLog_Generator")
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_COMPANIA", nullable = false, updatable = false, insertable = true)
    private Company company;

    @Column(name = "FECHA_ENTREGA", nullable = false)
    private Date deliveredDate;

    @Column(name = "FECHA_RECEPCION", nullable = true)
    private Date receivedDate;

    @Column(name = "ESTADO", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private ProductionTransferLogState state;

    @Version
    @Column(name = "VERSION", nullable = false)
    private long version;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "productionTransferLog", cascade = CascadeType.ALL)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
    private List<IncomingProductionOrder> incomingProductionOrderList = new ArrayList<IncomingProductionOrder>();

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

    public Date getDeliveredDate() {
        return deliveredDate;
    }

    public void setDeliveredDate(Date deliveredDate) {
        this.deliveredDate = deliveredDate;
    }

    public Date getReceivedDate() {
        return receivedDate;
    }

    public void setReceivedDate(Date receivedDate) {
        this.receivedDate = receivedDate;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public List<IncomingProductionOrder> getIncomingProductionOrderList() {
        return incomingProductionOrderList;
    }

    public void setIncomingProductionOrderList(List<IncomingProductionOrder> incomingProductionOrderList) {
        this.incomingProductionOrderList = incomingProductionOrderList;
    }

    public ProductionTransferLogState getState() {
        return state;
    }

    public void setState(ProductionTransferLogState state) {
        this.state = state;
    }
}
