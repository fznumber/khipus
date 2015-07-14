package com.encens.khipus.model.customers;

import com.encens.khipus.model.BaseModel;

import javax.persistence.*;
import java.util.Collection;
import java.util.Date;

/**
 * Created with IntelliJ IDEA.
 * User: Diego
 * Date: 12/11/13
 * Time: 16:40
 * To change this template use File | Settings | File Templates.
 */

@Entity
@Table(name = "ventadirecta")
public class VentaDirecta implements BaseModel  {

    //todo:revisar por q el id no es correlativo
    @Id
    @Column(name = "IDVENTADIRECTA")
    private Long idventadirecta;
    @Column(name = "DESCRIPCION")
    private String descripcion;
    @Column(name = "FECHA_PEDIDO")
    @Temporal(TemporalType.DATE)
    private Date fechaPedido;
    @Column(name = "OBSERVACION")
    private String observacion;
    @Column(name = "ESTADO")
    private String estado;
    @Column(name = "TOTAL")
    private Double total = 0.0;
    //cantidad * precio de venta
    @Basic(optional = false)
    @Column(name = "TOTALIMPORTE")
    private Double totalimporte = 0.0;

    @OneToMany(cascade = CascadeType.ALL,mappedBy = "ventaDirecta")
    private Collection<ArticleOrder> articulosPedidos ;

    @Column(name="CODIGO",columnDefinition="BIGINT(20)")
    private Long codigo;

    @JoinColumn(name = "IDCLIENTE", referencedColumnName = "IDPERSONACLIENTE")
    @ManyToOne(optional = false)
    private ClientePedido cliente;

    public Long getIdventadirecta() {
        return idventadirecta;
    }

    public void setIdventadirecta(Long idventadirecta) {
        this.idventadirecta = idventadirecta;
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

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
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
        hash += (idventadirecta != null ? idventadirecta.hashCode() : 0);
        return hash;
    }

    @Override
    public String toString() {
        return "com.encens.khipus.model.Pedidos[ idpedidos=" + idventadirecta + " ]";
    }

    public void setTotalimporte(Double totalimporte) {
        this.totalimporte = totalimporte;
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
