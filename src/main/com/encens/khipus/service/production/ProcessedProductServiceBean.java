package main.com.encens.khipus.service.production;

import com.encens.hp90.model.production.ProcessedProduct;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;

/**
 * Created with IntelliJ IDEA.
 * User: david
 * Date: 6/24/13
 * Time: 10:17 AM
 * To change this template use File | Settings | File Templates.
 */
@Name("processedProductService")
@Stateless
@AutoCreate
public class ProcessedProductServiceBean implements ProcessedProductService {

    @In(value = "#{entityManager}")
    private EntityManager em;

    @Override
    public ProcessedProduct find(long id) {
        return (ProcessedProduct)em.createNamedQuery("ProcessedProduct.withProductCompositionFind")
                                   .setParameter("id", id)
                                   .getSingleResult();
    }
}


