package com.encens.khipus.service.production;

import com.encens.khipus.framework.service.ExtendedGenericServiceBean;
import com.encens.khipus.model.production.ArticleEstate;
import com.encens.khipus.model.warehouse.ProductItem;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Diego
 * Date: 12/11/13
 * Time: 19:23
 * To change this template use File | Settings | File Templates.
 */
@Name("articleEstateService")
@AutoCreate
@Stateless
public class ArticleEstateServiceBean extends ExtendedGenericServiceBean implements ArticleEstateService {

    @In(value = "#{entityManager}")
    private EntityManager em;

    @Override
    public Boolean existArticleEstate(ProductItem productItem)
    {
        Boolean band = true;
        try{
        ArticleEstate estate = (ArticleEstate)em.createQuery("SELECT articleEstate FROM ArticleEstate articleEstate " +
                                                " WHERE articleEstate.productItem = :productItem " +
                                                " and articleEstate.estate = :estate")
                                .setParameter("productItem",productItem)
                                .setParameter("estate","NOVERIFICABLE")
                                .getSingleResult();
            band = estate.getEstate().equals("NOVERIFICABLE") ;

        }catch (NoResultException e)
        {
            return false;
        }
        return band;
    }

    @Override
    public Boolean verifyEstate(ProductItem productItem, String compare )
    {
        Boolean band = true;
        try{
            ArticleEstate estate = (ArticleEstate)em.createQuery("SELECT articleEstate FROM ArticleEstate articleEstate " +
                                    "WHERE articleEstate.productItem = :productItem " +
                                    "AND articleEstate.estate = :estate ")
                    .setParameter("productItem",productItem)
                    .setParameter("estate",compare)
                    .getSingleResult();
            band =  estate.getEstate().equals(compare) ;
        }catch (NoResultException e)
        {
            return false;
        }
        return band;
    }
}
