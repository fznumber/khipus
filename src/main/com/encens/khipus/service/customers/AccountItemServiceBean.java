package com.encens.khipus.service.customers;

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
    public List<ArticleReport> findAccountItem() {
        List<ArticleReport> accountItems = new ArrayList<ArticleReport>();
        try{

            List<Object[]> datas = em.createNativeQuery("select ia.descri, ca.id_cuenta, ca.cod_art, ca.no_cia from USER01_DAF.cuentas_art_wise  ca\n" +
                                                      "inner join WISE.inv_articulos ia\n" +
                                                      "on ia.cod_art = ca.cod_art")
                                          .getResultList();

            for(Object[] obj: datas)
            {


                AccountItem accountItem = new AccountItem();
                AccountItemPK accountItemPK = new AccountItemPK();
                accountItemPK.setIdAccount((Integer)obj[1]);
                accountItemPK.setCodArt((String)obj[2]);
                accountItemPK.setCompanyNumber((String)obj[3]);
                accountItem.setId(accountItemPK);

                ArticleReport articleReport = new ArticleReport();
                articleReport.setName((String)obj[0]);
                articleReport.setAccountItem(accountItem);
                accountItems.add(articleReport);
            }

        }catch (NoResultException e)
        {
            return new ArrayList<ArticleReport>();
        }
        return accountItems;
    }

    public class ArticleReport
    {
        private String name;
        private AccountItem accountItem;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public AccountItem getAccountItem() {
            return accountItem;
        }

        public void setAccountItem(AccountItem accountItem) {
            this.accountItem = accountItem;
        }
    }
}
