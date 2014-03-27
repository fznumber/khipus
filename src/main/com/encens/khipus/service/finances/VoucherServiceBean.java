package com.encens.khipus.service.finances;

import com.encens.khipus.interceptor.FinancesUser;
import com.encens.khipus.model.admin.User;
import com.encens.khipus.model.finances.Voucher;
import com.encens.khipus.model.finances.VoucherDetail;
import com.encens.khipus.util.BigDecimalUtil;
import com.encens.khipus.util.Constants;
import com.encens.khipus.util.ValidatorUtil;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import java.util.List;

/**
 * Creates integration vouchers in finances system
 *
 * @author
 * @version 1.0
 */

@Name("voucherService")
@Stateless
@AutoCreate
@FinancesUser
public class VoucherServiceBean implements VoucherService {

    @In(value = "#{entityManager}")
    private EntityManager em;

    @In
    private User currentUser;

    /**
     * Creates the voucher, then gets the transaction number, sets it to the detail and persist the detail list
     *
     * @param voucher the voucher to persist
     */
    public void create(Voucher voucher) {
        List<VoucherDetail> voucherDetailList = voucher.getDetails();
        voucher = createBody(voucher);
        createDetail(voucher, voucherDetailList);
    }

    /**
     * Creates the voucher body, then gets the transaction number
     *
     * @param voucher the voucher to persist
     */
    public Voucher createBody(Voucher voucher) {
        if (ValidatorUtil.isBlankOrNull(voucher.getUserNumber())) {
            voucher.setUserNumber(currentUser.getFinancesCode());
        }
        voucher.setPendantRegistry("SI");
        if (ValidatorUtil.isBlankOrNull(voucher.getTransactionNumber())) {
            em.persist(voucher);
        } else {
            voucher = em.merge(voucher);
        }
        em.flush();
        return voucher;
    }

    /**
     * Creates the voucher detail
     *
     * @param voucher           The voucher for change if is necessary
     * @param voucherDetailList The detail list that will be persisted
     */
    public void createDetail(Voucher voucher, List<VoucherDetail> voucherDetailList) {
        boolean isEmpty = true;
        for (VoucherDetail detail : voucherDetailList) {
            if (!BigDecimalUtil.isZeroOrNull(detail.getDebit()) || !BigDecimalUtil.isZeroOrNull(detail.getCredit())) {
                isEmpty = false;
                detail.setTransactionNumber(voucher.getTransactionNumber());
                em.persist(detail);
            }
        }
        em.flush();

        if (isEmpty) {
            /*todo fixed using enumeration, this change must be updated in everywhere */
            voucher.setState("ANL");
            em.merge(voucher);
            em.flush();
        }
    }

    public void deleteVoucher(Voucher voucher) {
        em.remove(voucher);
        em.flush();
    }
}
