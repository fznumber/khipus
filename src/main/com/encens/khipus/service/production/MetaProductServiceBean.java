package com.encens.khipus.service.production;

import com.encens.khipus.framework.service.GenericServiceBean;
import com.encens.khipus.model.production.MetaProduct;
import com.encens.khipus.model.production.ProcessedProduct;
import com.encens.khipus.model.warehouse.ProductItem;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: david
 * Date: 6/24/13
 * Time: 10:17 AM
 * To change this template use File | Settings | File Templates.
 */
@Name("metaProductService")
@Stateless
@AutoCreate
public class MetaProductServiceBean extends GenericServiceBean implements MetaProductService {

    @In(value = "#{entityManager}")
    private EntityManager em;

    public MetaProduct find(long id) {
        MetaProduct metaProduct = (MetaProduct)em.createQuery("SELECT metaProduct from MetaProduct metaProduct where metaProduct.id = :id")
                .setParameter("id", id)
                .getSingleResult();
        return metaProduct;
    }

}


