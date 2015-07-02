package com.encens.khipus.service.accouting;

import com.encens.khipus.framework.service.GenericServiceBean;
import com.encens.khipus.model.finances.Voucher;
import com.encens.khipus.model.finances.VoucherDetail;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author
 * @version 2.4
 */
@Stateless
@Name("voucherAccoutingService")
@AutoCreate
public class VoucherAccoutingServiceBean extends GenericServiceBean implements VoucherAccoutingService {

    @In(value = "#{entityManager}")
    private EntityManager em;

    @Override
    public List<VoucherDetail> getVoucherDetailList(String transactionNumber){

        List<VoucherDetail> voucherDetails = new ArrayList<VoucherDetail>();

        try {
            voucherDetails = (List<VoucherDetail>) em.createQuery("select voucherDetail from VoucherDetail voucherDetail " +
                    " where voucherDetail.transactionNumber = :transactionNumber ")
                    .setParameter("transactionNumber", transactionNumber)
                    .getResultList();
            /*voucherDetails = (List<VoucherDetail>) em.createNativeQuery("select * from sf_tmpdet where no_trans = :transactionNumber")
                    .setParameter("transactionNumber", transactionNumber).getResultList();*/
        }catch (NoResultException e){
            return null;
        }
        return voucherDetails;
    }

    @Override
    public Voucher getVoucher(String transactionNumber) {

        Voucher voucher = null;
        try {
            voucher = (Voucher) em.createQuery("select voucher from Voucher voucher " +
                    " where voucher.transactionNumber = :transactionNumber ")
                    .setParameter("transactionNumber", transactionNumber)
                    .getSingleResult();
        }catch (NoResultException e){
            return null;
        }
        return voucher;
    }


}
