package com.encens.khipus.service.warehouse;

import com.encens.khipus.framework.service.GenericService;
import com.encens.khipus.model.warehouse.SoldProduct;

import javax.ejb.Local;
import java.util.Date;
import java.util.List;

/**
 * @author
 * @version 2.4
 */
@Local
public interface SoldProductService extends GenericService {
    List<SoldProduct> getSoldProducts(String invoiceNumber, String companyNumber);

    public List<SoldProduct> getSoldProductsWithoutCutCheese(String invoiceNumber, String companyNumber);

    List<SoldProduct> getSoldProductsCashSale(String invoiceNumber, String companyNumber);

    List<SoldProduct> getSoldProductsCashOrder(String invoiceNumber, String companyNumber);

    public List<SoldProduct> getFindDelivery(String invoiceNumber, String companyNumber);

    Date getDateFromSoldProductOrder(SoldProduct soldProduct);
}
