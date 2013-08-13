package com.encens.khipus.service.customers;

import com.encens.khipus.exception.EntryDuplicatedException;
import com.encens.khipus.model.customers.Credit;
import com.encens.khipus.model.customers.Invoice;

/**
 * Credit transaction services interface
 *
 * @author:
 */
public interface CreditTransactionService {
    void create(Credit credit, Invoice invoice) throws EntryDuplicatedException;
}
