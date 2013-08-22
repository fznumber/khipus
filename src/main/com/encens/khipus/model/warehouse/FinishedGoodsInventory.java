package main.com.encens.khipus.model.warehouse;

import com.encens.hp90.model.BaseModel;
import com.encens.hp90.model.CompanyListener;
import com.encens.hp90.model.admin.Company;
import org.hibernate.annotations.Filter;

import javax.persistence.*;
import java.util.Date;

@TableGenerator(name = "FinishedGoodsInventory_Generator",
        table = "SECUENCIA",
        pkColumnName = "TABLA",
        valueColumnName = "VALOR",
        pkColumnValue = "INVENTARIOPRODUCTOTERMINADO",
        allocationSize = 10)

@Entity
@Table(name = "INVENTARIOPRODUCTOTERMINADO")
@Filter(name = "companyFilter")
@EntityListeners(CompanyListener.class)
public class FinishedGoodsInventory implements BaseModel {
    @Id
    @Column(name = "ID_INVENTARIO_PRODUCTO_TERMINADO", nullable = false)
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "FinishedGoodsInventory_Generator")
    private Long id;

    @Column(name = "CANTIDAD", nullable = false)
    private Double amount;

    @Column(name = "FECHA", nullable = false)
    private Date date;

    @ManyToOne(optional = false, fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "ID_AMBIENTE_DEPOSITO", nullable = false, updatable = false, insertable = true)
    private WarehouseSlot warehouseSlot;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_COMPANIA", nullable = false, updatable = false, insertable = true)
    private Company company;

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

    public WarehouseSlot getWarehouseSlot() {
        return warehouseSlot;
    }

    public void setWarehouseSlot(WarehouseSlot warehouseSlot) {
        this.warehouseSlot = warehouseSlot;
    }

    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
    }
}
