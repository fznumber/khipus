package com.encens.khipus.service.warehouse;

import com.encens.khipus.framework.service.GenericServiceBean;
import com.encens.khipus.model.warehouse.SoldProduct;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.Name;

import javax.ejb.Stateless;
import java.util.ArrayList;
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

}
