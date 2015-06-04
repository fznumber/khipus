package com.encens.khipus.action.accounting;

import com.encens.khipus.framework.action.QueryDataModel;
import com.encens.khipus.model.finances.Voucher;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.security.Restrict;

import java.util.Arrays;
import java.util.List;

/**
 * VoucherDataModel
 *
 * @author
 * @version 2.26
 */
@Name("vouherDataModel")
@Scope(ScopeType.PAGE)
@Restrict("#{s:hasPermission('ORGANIZATION','VIEW')}")  /* Change restrict */
public class VoucherDataModel extends QueryDataModel<Long, Voucher> {
    private static final String[] RESTRICTIONS =
            {"lower(voucher.transactionNumber) like concat('%', concat(lower(#{voucherDataModel.criteria.transactionNumber}), '%'))"};

    @Create
    public void init() {
        sortProperty = "voucher.transactionNumber";
    }

    @Override
    public String getEjbql() {
        return "select voucher from Voucher voucher";
    }

    @Override
    public List<String> getRestrictions() {
        return Arrays.asList(RESTRICTIONS);
    }
}
