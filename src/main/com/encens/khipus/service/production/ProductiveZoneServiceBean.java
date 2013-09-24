package com.encens.khipus.service.production;

import com.encens.khipus.model.production.CollectedRawMaterial;
import com.encens.khipus.model.production.ProductiveZone;
import com.encens.khipus.model.production.RawMaterialCollectionSession;
import com.encens.khipus.model.production.RawMaterialProducer;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.TemporalType;
import java.util.Date;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: david
 * Date: 5/29/13
 * Time: 11:09 AM
 * To change this template use File | Settings | File Templates.
 */
@Stateless
@Name("productiveZoneService")
@AutoCreate
public class ProductiveZoneServiceBean implements ProductiveZoneService {

    @In(value = "#{entityManager}")
    private EntityManager em;

    @Override
    public List<ProductiveZone> findAll() {
        List<ProductiveZone> aux = em.createNamedQuery("ProductiveZone.findAll").getResultList();
        return aux;
    }

    @Override
    public List<ProductiveZone> findAllThatDoNotHaveCollectionForm(Date date) {
        return em.createNamedQuery("ProductiveZone.findAllThatDoNotHaveCollectionFormOnDate")
                 .setParameter("date", date)
                 .getResultList();
    }

    @Override
    public ProductiveZone find(long id) {
        return em.find(ProductiveZone.class, id);
    }

}
