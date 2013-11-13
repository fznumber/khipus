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

    /*@Override
    public List<ArticleEstate> findArticleEstate(List<ArticleEstate> articleEstateList) {
        List<ArticleEstate> estateList = new ArrayList<ArticleEstate>();
        estateList = (List<ArticleEstate>)em.createQuery("SELECT articleEstate FROM ArticleEstate articleEstate WHERE ArticleEstate.productItemList = :articleEstateList")
                     .setParameter("articleEstateList",articleEstateList)
                     .getResultList();
        return estateList;
    }*/

    @Override
    public Boolean existArticleEstate(ProductItem productItem)
    {
        Boolean band = true;
        try{
        ArticleEstate estate = (ArticleEstate)em.createQuery("SELECT articleEstate FROM ArticleEstate articleEstate WHERE articleEstate.productItem = :productItem")
                                .setParameter("productItem",productItem)
                                .getSingleResult();
        }catch (NoResultException e)
        {
            return false;
        }
        return band;
    }
}
