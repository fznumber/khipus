package com.encens.khipus.service.accouting;

import com.encens.khipus.framework.service.GenericService;
import com.encens.khipus.model.finances.Voucher;
import com.encens.khipus.model.finances.VoucherDetail;

import javax.ejb.Local;
import java.util.List;

/**
 * @author
 * @version 2.4
 */
@Local
public interface VoucherAccoutingService extends GenericService {

    List<VoucherDetail> getVoucherDetailList(String transactionNumber);
    public Voucher getVoucher(String transactionNumber);
}
