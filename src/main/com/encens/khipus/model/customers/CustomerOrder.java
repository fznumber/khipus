package com.encens.khipus.model.customers;

import com.encens.khipus.model.BaseModel;
import com.encens.khipus.model.CompanyListener;
import com.encens.khipus.model.contacts.Address;
import com.encens.khipus.model.contacts.Zone;
import com.encens.khipus.model.employees.Employee;
import com.encens.khipus.model.production.OrderMaterial;
import com.encens.khipus.model.warehouse.ProductItem;
import com.encens.khipus.util.Constants;
import org.hibernate.annotations.Filter;
import org.hibernate.validator.Length;

import javax.persistence.*;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Diego
 * Date: 12/11/13
 * Time: 16:40
 * To change this template use File | Settings | File Templates.
 */

@Entity
@Table(name = "pedidos")
public class CustomerOrder implements BaseModel  {

    //todo:revisar por q el id no es correlativo
    @Id
    @Column(name = "IDPEDIDOS")
    private Long idpedidos;
    @Column(name = "DESCRIPCION")
    private String descripcion;
    @Column(name = "FECHA_PEDIDO")
    @Temporal(TemporalType.DATE)
    private Date fechaPedido;
    @Basic(optional = false)
    @Column(name = "FECHA_ENTREGA")
    @Temporal(TemporalType.DATE)
    private Date fechaEntrega;
    @Column(name = "FECHA_A_PAGAR")
    @Temporal(TemporalType.DATE)
    private Date fechaAPagar;
    @Column(name = "OBSERVACION")
    private String observacion;
    @Column(name = "FACTURA")
    private String factura;
    @Column(name = "PORCENTAJECOMISION")
    private Double porcentajeComision = 0.0;
    @Column(name = "PORCENTAJEGARANTIA")
    private Double porcentajeGarantia = 0.0;
    @Column(name = "VALORCOMISION")
    private Double valorComision = 0.0;
    @Column(name = "VALORGARANTIA")
    private Double valorGarantia = 0.0;
    @Column(name = "ESTADO")
    private String estado;
    @Column(name = "CON_REPOSICION",columnDefinition = "INT(1)")
    private Boolean conReposicion = false;
    @Column(name = "TOTAL")
    private Double total = 0.0;
    //cantidad * precio de venta
    @Basic(optional = false)
    @Column(name = "TOTALIMPORTE")
    private Double totalimporte = 0.0;

    @OneToMany(cascade = CascadeType.ALL,mappedBy = "customerOrder")
    private Collection<ArticleOrder> articulosPedidos ;

    @Basic
    @Column(name="codigo",columnDefinition="bigint(20)")
    private Long codigo;

    @JoinColumn(name = "IDCLIENTE", referencedColumnName = "IDPERSONACLIENTE")
    @ManyToOne(optional = false)
    private ClientePedido cliente;

    public Long getIdpedidos() {
        return idpedidos;
    }

    public void setIdpedidos(Long idpedidos) {
        this.idpedidos = idpedidos;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Date getFechaPedido() {
        return fechaPedido;
    }

    public void setFechaPedido(Date fechaPedido) {
        this.fechaPedido = fechaPedido;
    }

    public Double getTotal() {
        return total;
    }

    public void setTotal(Double total) {
        this.total = total;
    }

    public Date getFechaEntrega() {
        return fechaEntrega;
    }

    public void setFechaEntrega(Date fechaEntrega) {
        this.fechaEntrega = fechaEntrega;
    }

    public Date getFechaAPagar() {
        return fechaAPagar;
    }

    public void setFechaAPagar(Date fechaAPagar) {
        this.fechaAPagar = fechaAPagar;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }

    public String getFactura() {
        return factura;
    }

    public void setFactura(String factura) {
        this.factura = factura;
    }

    public Double getPorcentajeComision() {
        return porcentajeComision;
    }

    public void setPorcentajeComision(Double porcenDescuento) {
        this.porcentajeComision = porcenDescuento;
    }

    public Double getPorcentajeGarantia() {
        return porcentajeGarantia;
    }

    public void setPorcentajeGarantia(Double porcenRetencion) {
        this.porcentajeGarantia = porcenRetencion;
    }

    public String getEstado() {
        if(estado == null)
        {
            estado = "ACTIVO";
        }
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (idpedidos != null ? idpedidos.hashCode() : 0);
        return hash;
    }

    @Override
    public String toString() {
        return "com.encens.khipus.model.Pedidos[ idpedidos=" + idpedidos + " ]";
    }

    public Double getValorComision() {
        return valorComision;
    }

    public void setValorComision(Double valorDescuento) {
        this.valorComision = valorDescuento;
    }

    public Double getValorGarantia() {
        return valorGarantia;
    }

    public void setValorGarantia(Double valorRetencion) {
        this.valorGarantia = valorRetencion;
    }

    public void setTotalimporte(Double totalimporte) {
        this.totalimporte = totalimporte;
    }

    public Boolean getConReposicion() {
        return conReposicion;
    }

    public void setConReposicion(Boolean conReposicion) {
        this.conReposicion = conReposicion;
    }

    @Override
    public Object getId() {
        return null;
    }

    public Collection<ArticleOrder> getArticulosPedidos() {
        return articulosPedidos;
    }

    public void setArticulosPedidos(Collection<ArticleOrder> articulosPedidos) {
        this.articulosPedidos = articulosPedidos;
    }

    public Long getCodigo() {
        return codigo;
    }

    public void setCodigo(Long codigo) {
        this.codigo = codigo;
    }

    public ClientePedido getCliente() {
        return cliente;
    }

    public void setCliente(ClientePedido cliente) {
        this.cliente = cliente;
    }

    public Double getTotalimporte() {
        return totalimporte;
    }
}
