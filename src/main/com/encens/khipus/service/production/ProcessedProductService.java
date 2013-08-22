package main.com.encens.khipus.service.production;

import com.encens.hp90.model.production.ProcessedProduct;

import javax.ejb.Local;

/**
 * Created with IntelliJ IDEA.
 * User: david
 * Date: 6/24/13
 * Time: 10:16 AM
 * To change this template use File | Settings | File Templates.
 */
@Local
public interface ProcessedProductService {
    public ProcessedProduct find(long id);
}
