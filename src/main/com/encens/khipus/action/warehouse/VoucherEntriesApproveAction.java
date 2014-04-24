package com.encens.khipus.action.warehouse;

import com.encens.khipus.exception.finances.CompanyConfigurationNotFoundException;
import com.encens.khipus.framework.action.GenericAction;
import com.encens.khipus.model.admin.BusinessUnit;
import com.encens.khipus.model.finances.FinanceUser;
import com.encens.khipus.model.finances.FinancesModule;
import com.encens.khipus.model.finances.Voucher;
import com.encens.khipus.service.finances.VoucherService;
import com.encens.khipus.service.finances.VoucherServiceBean;
import com.encens.khipus.util.Constants;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;

import java.util.Date;

/**
 * @author
 * @version 3.0
 */
@Name("voucherEntriesApproveAction")
@Scope(ScopeType.CONVERSATION)
public class VoucherEntriesApproveAction extends GenericAction<Voucher> {

       private BusinessUnit businessUnit;
       private FinancesModule financesModule;
       private FinanceUser financeUser;

       @In
       private VoucherService voucherService;

       public void approvedAllVoucherEntries(Date startDate, Date endDate,String numberTransction){
           try {
               voucherService.approvedAllVoucherEntries(Constants.defaultCompanyNumber,businessUnit,startDate,endDate,numberTransction,financeUser,financesModule);
           } catch (CompanyConfigurationNotFoundException e) {
               e.printStackTrace();
           }
       }

    public BusinessUnit getBusinessUnit() {
        return businessUnit;
    }

    public void setBusinessUnit(BusinessUnit businessUnit) {
        this.businessUnit = businessUnit;
    }

    public FinancesModule getFinancesModule() {
        return financesModule;
    }

    public void setFinancesModule(FinancesModule financesModule) {
        this.financesModule = financesModule;
    }

    public FinanceUser getFinanceUser() {
        return financeUser;
    }

    public void setFinanceUser(FinanceUser financeUser) {
        this.financeUser = financeUser;
    }

    public void cleanFinanceUser(){
        this.financeUser = null;
    }
}
