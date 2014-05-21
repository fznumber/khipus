package com.encens.khipus.service.warehouse;

import com.encens.khipus.framework.service.GenericServiceBean;
import com.encens.khipus.model.warehouse.SoldProduct;
import com.encens.khipus.util.Constants;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.Name;

import javax.ejb.Stateless;
import javax.persistence.NoResultException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author
 * @version 2.4
 */
@Stateless
@Name("soldProductService")
@AutoCreate
public class SoldProductServiceBean extends GenericServiceBean implements SoldProductService {

    @SuppressWarnings(value = "unchecked")
    public List<SoldProduct> getSoldProducts(String invoiceNumber, String companyNumber) {
        List<SoldProduct> result = getEntityManager()
                .createNamedQuery("SoldProduct.findByInvoiceNumber")
                .setParameter("invoiceNumber", invoiceNumber)
                .setParameter("companyNumber", companyNumber)
                .getResultList();
        if (null == result) {
            result = new ArrayList<SoldProduct>();
        }

        return result;
    }

    @SuppressWarnings(value = "unchecked")
    public List<SoldProduct> getSoldProductsWithoutCutCheese(String invoiceNumber, String companyNumber) {
        List<SoldProduct> result = getEntityManager()
                .createNamedQuery("SoldProduct.findByInvoiceNumberWithoutCutCheese")
                .setParameter("codCutCheese", Constants.COD_CUT_CHEESE)
                .setParameter("invoiceNumber", invoiceNumber)
                .setParameter("companyNumber", companyNumber)
                .getResultList();
        if (null == result) {
            result = new ArrayList<SoldProduct>();
        }

        return result;
    }



    @SuppressWarnings(value = "unchecked")
    public List<SoldProduct> getSoldProductsCashSale(String invoiceNumber, String companyNumber) {
        List<SoldProduct> result = getEntityManager()
                .createNamedQuery("SoldProduct.findByCashSale")
                .setParameter("invoiceNumber", invoiceNumber)
                .setParameter("companyNumber", companyNumber)
                .getResultList();
        if (null == result) {
            result = new ArrayList<SoldProduct>();
        }
        return result;
    }

    public List<SoldProduct> getFindDelivery(String invoiceNumber, String companyNumber) {
        List<SoldProduct> result = getEntityManager()
                .createNamedQuery("SoldProduct.findDelivery")
                .setParameter("invoiceNumber", invoiceNumber)
                .setParameter("companyNumber", companyNumber)
                .getResultList();
        if (null == result) {
            result = new ArrayList<SoldProduct>();
        }
        return result;
    }

    @SuppressWarnings(value = "unchecked")
    public List<SoldProduct> getSoldProductsCashOrder(String invoiceNumber, String companyNumber) {
        List<SoldProduct> result = getEntityManager()
                .createNamedQuery("SoldProduct.findByCashOrder")
                .setParameter("invoiceNumber", invoiceNumber)
                .setParameter("companyNumber", companyNumber)
                .getResultList();
        if (null == result) {
            result = new ArrayList<SoldProduct>();
        }
        return result;
    }

    @Override
    public Date getDateFromSoldProductOrder(SoldProduct soldProduct) {
        Date result;
        try{
            result = (Date)getEntityManager().createNativeQuery("select pe.fecha_entrega from WISE.inv_ventart iv\n" +
                    "inner join USER01_DAF.pedidos pe\n" +
                    "on iv.pedido = pe.pedido\n" +
                    "where iv.id_mov = :idSoldProduct")
                    .setParameter("idSoldProduct",soldProduct.getId())
                    .getSingleResult();
        }catch (NoResultException e){
            result = (Date)getEntityManager().createNativeQuery("select mo.fecha from WISE.inv_ventart iv\n" +
                    "inner join USER01_DAF.b_movimientos mo\n" +
                    "on mo.nrofactura = iv.no_fact\n" +
                    "where iv.id_mov = :idSoldProduct")
                    .setParameter("idSoldProduct",soldProduct.getId())
                    .getSingleResult();
        }
        return result;
    }

}
