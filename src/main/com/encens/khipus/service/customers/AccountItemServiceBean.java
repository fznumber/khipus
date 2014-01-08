package com.encens.khipus.service.customers;

import com.encens.khipus.action.customers.reports.OrderReceiptReportAction;
import com.encens.khipus.framework.service.ExtendedGenericServiceBean;
import com.encens.khipus.model.customers.AccountItem;
import com.encens.khipus.model.customers.AccountItemPK;
import com.encens.khipus.model.customers.ClientOrder;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TemporalType;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Diego
 * Date: 12/11/13
 * Time: 19:23
 * To change this template use File | Settings | File Templates.
 */
@Name("accountItemService")
@AutoCreate
@Stateless
public class AccountItemServiceBean extends ExtendedGenericServiceBean implements AccountItemService {

    @In(value = "#{entityManager}")
    private EntityManager em;

    @Override
    public List<OrderClient> findClientsOrder(Date date,BigDecimal distribuidor) {
        List<OrderClient> clientOrders = new ArrayList<OrderClient>();
        try{

            List<Object[]> datas = em.createNativeQuery("select nvl(pe.ap,' '),nvl(pe.am,' ') , nvl(pe.nom,' '), ped.pedido, nvl(it.razon_soc,' ') from USER01_DAF.per_insts pi\n" +
                    "                    left join user01_daf.instituciones it\n" +
                    "                    on it.pi_id = pi.id\n" +
                    "                    left join USER01_DAF.personas pe\n" +
                    "                    on pe.nro_doc = pi.nro_doc\n" +
                    "                    inner join USER01_DAF.pedidos ped\n" +
                    "                    on ped.id = pi.id\n" +
                    "where ped.fecha_entrega = :date \n" +
                    "and ped.estado_pedido = 'PEN' \n" +
                    "and ped.distribuidor = :distribuidor")
                    .setParameter("date", date, TemporalType.DATE)
                    .setParameter("distribuidor",distribuidor)
                    .getResultList();

            for(Object[] obj: datas)
            {
                OrderClient client = new OrderClient();
                client.setName((String)obj[3]+"-"+(String)obj[0]+" "+(String)obj[1]+" "+(String)obj[2]+(String)obj[4]);
                client.setIdOrder((String)obj[3]);
                clientOrders.add(client);
            }

        }catch (NoResultException e)
        {
            return new ArrayList<OrderClient>();
        }
        return clientOrders;
    }

    @Override
    public Integer getAmount(String codArt,String codPedido){
        BigDecimal result = BigDecimal.ZERO;
        try{
          result = (BigDecimal)em.createNativeQuery("SELECT nvl(cantidad,0) FROM USER01_DAF.articulos_pedido \n" +
                  "where cod_art = :codArt\n" +
                  "and pedido = :codPedido")
                 .setParameter("codPedido",codPedido)
                 .setParameter("codArt",codArt)
                 .getSingleResult();

        }catch (NoResultException e)
        {
            return 0;
        }
        return result.intValue();
    }

    @Override
    public String getNameEmployeed(BigDecimal codEmployeed)
    {
        String name = "";
        try{
            name =  (String)em.createNativeQuery("select nombres||' '||apellidopaterno  from eos.persona where IDPERSONA = :cod")
                    .setParameter("cod",codEmployeed)
                    .getSingleResult();
        }catch(NoResultException e)
        {
            return "";
        }
        return name;
    }

    @Override
    public List<BigDecimal> findDistributor(Date dateOrder)
    {
        List<BigDecimal> distributors = new ArrayList<BigDecimal>();
        try{
            distributors = em.createNativeQuery("select distinct distribuidor from USER01_DAF.pedidos where distribuidor is not null \n" +
                                                " and fecha_entrega = :dateOrder")
                           .setParameter("dateOrder",dateOrder)
                           .getResultList();

        }catch(NoResultException e)
        {
            return new ArrayList<BigDecimal>();
        }
        return distributors;
    }

    @Override
    public List<OrderItem> findOrderItem(Date dateOrder){
        List<OrderItem> orderItems = new ArrayList<OrderItem>();
        try{
            List<Object[]> datas = em.createNativeQuery("select distinct ia.nombrecorto,ap.id_cuenta,ap.cod_art,ap.no_cia from USER01_DAF.articulos_pedido ap\n" +
                    "inner join WISE.inv_articulos ia\n" +
                    "on ia.cod_art = ap.cod_art\n" +
                    "inner join USER01_DAF.pedidos pe\n" +
                    "on ap.pedido = pe.pedido\n" +
                    "where pe.fecha_entrega = :dateOrder")
                    .setParameter("dateOrder",dateOrder,TemporalType.DATE)
                    .getResultList();
            for(Object[] obj: datas)
            {
                OrderItem item = new OrderItem();
                item.setNameItem((String)obj[0]);
                item.setIdAccount((BigDecimal)obj[1]);
                item.setCodArt((String)obj[2]);
                item.setNoCia((String)obj[3]);
                orderItems.add(item);
            }

        }catch (NoResultException e)
        {
            return new ArrayList<OrderItem>();
        }
        return orderItems;
    }

    public class OrderItem
    {
        private String nameItem;
        private String CodArt;
        private BigDecimal IdAccount;
        private String NoCia;
        private int posX;
        private int posY;

        public String getNoCia() {
            return NoCia;
        }

        public void setNoCia(String noCia) {
            NoCia = noCia;
        }

        public String getNameItem() {
            return nameItem;
        }

        public void setNameItem(String nameItem) {
            this.nameItem = nameItem;
        }

        public String getCodArt() {
            return CodArt;
        }

        public void setCodArt(String codArt) {
            CodArt = codArt;
        }

        public BigDecimal getIdAccount() {
            return IdAccount;
        }

        public void setIdAccount(BigDecimal idAccount) {
            IdAccount = idAccount;
        }

        public int getPosX() {
            return posX;
        }

        public void setPosX(int posX) {
            this.posX = posX;
        }

        public int getPosY() {
            return posY;
        }

        public void setPosY(int posY) {
            this.posY = posY;
        }
    }

    public class OrderClient
    {
        private String name;

        private int posX;
        private int posY;
        private String idOrder;

        public String getIdOrder() {
            return idOrder;
        }

        public void setIdOrder(String idOrder) {
            this.idOrder = idOrder;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getPosX() {
            return posX;
        }

        public void setPosX(int posX) {
            this.posX = posX;
        }

        public int getPosY() {
            return posY;
        }

        public void setPosY(int posY) {
            this.posY = posY;
        }
    }
}
