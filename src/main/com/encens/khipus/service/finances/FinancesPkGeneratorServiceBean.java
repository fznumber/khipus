package com.encens.khipus.service.finances;

import com.encens.khipus.framework.service.GenericServiceBean;
import com.encens.khipus.util.finances.FinancesUtil;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.Name;

import javax.ejb.Stateless;

/**
 * @author
 * @version 2.0
 */
@Stateless
@Name("financesPkGeneratorService")
@AutoCreate
public class FinancesPkGeneratorServiceBean extends GenericServiceBean implements FinancesPkGeneratorService {

    public String getNextPK() {
        return executeFunction(NativeFunction.TRANSACTION_NUMBER);
    }

    @Override
    public void setNextPK(String nextNoTrans) {
        getEntityManager().createNativeQuery("call sp_setSeqVal('VALE',:nextNoTrans)")
                .setParameter("nextNoTrans",nextNoTrans)
                .executeUpdate();
    }

    public String getNextTmpenc() {
        return executeFunction(NativeFunction.TRANSACTION_NUMBER_TMPENC);
    }

    public String executeFunction(NativeFunction nativeFunction) {
        return (String) getEntityManager().createNativeQuery("select " + FinancesUtil.addSchema(nativeFunction.getFunction()) + " from dual").getSingleResult();
    }
}
