package com.encens.khipus.service.finances;

import com.encens.khipus.framework.service.GenericService;

import javax.ejb.Local;

/**
 * @author
 * @version 2.0
 */
@Local
public interface FinancesPkGeneratorService extends GenericService {
    public enum NativeFunction {
        TRANSACTION_NUMBER("getNextSeq('VALE')"),
        TRANSACTION_NUMBER_TMPENC("next_tmpenc()"),
        CONCILIATION_NUMBER("sigte_conci()");
        private String function;

        private NativeFunction(String function) {
            this.function = function;
        }

        public String getFunction() {
            return function;
        }

    }

    String getNextPK();

    public void setNextPK(String nextNoTrans);

    String getNextTmpenc();

    String executeFunction(NativeFunction nativeFunction);
}
