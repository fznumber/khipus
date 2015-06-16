package com.encens.khipus.action.accounting;

import com.encens.khipus.framework.action.QueryDataModel;
import com.encens.khipus.model.finances.Voucher;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;

import java.util.Arrays;
import java.util.List;

/**
 * VoucherDataModel
 *
 * @author
 * @version 2.26
 */
@Name("voucherDataModel")
@Scope(ScopeType.PAGE)
/*@Restrict("#{s:hasPermission('ORGANIZATION','VIEW')}")*/
public class VoucherDataModel extends QueryDataModel<Long, Voucher> {

    private static final String[] RESTRICTIONS =
            {"lower(voucher.transactionNumber) like concat('%', concat(lower(#{voucherDataModel.criteria.transactionNumber}), '%'))",
             "lower(voucher.gloss) like concat('%', concat(lower(#{voucherDataModel.criteria.gloss}), '%'))"};

    @Create
    public void init() {
        sortProperty = "voucher.transactionNumber";
        sortAsc = false;
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
