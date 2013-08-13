package com.encens.khipus.service.customers;

import com.encens.khipus.exception.EntryDuplicatedException;
import com.encens.khipus.model.customers.Credit;
import com.encens.khipus.model.customers.CreditTransaction;
import com.encens.khipus.model.customers.Invoice;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;

import javax.ejb.Stateless;
import javax.persistence.EntityExistsException;
import javax.persistence.EntityManager;

/**
 * Credit transaction service implementation class
 *
 * @author
 */


@Stateless
@Name("creditTransactionService")
@AutoCreate
public class CreditTransactionServiceBean implements CreditTransactionService {


    @In(value = "#{entityManager}")
    private EntityManager em;

    public void create(Credit credit, Invoice invoice) throws EntryDuplicatedException {
        try {

            CreditTransaction transaction = new CreditTransaction();
            transaction.setCredit(credit);
            transaction.setInvoice(invoice);
            transaction.setAmount(invoice.getTotalDiscount() != null ? invoice.getTotalAmount().subtract(invoice.getTotalDiscount()) : invoice.getTotalAmount());
            em.persist(transaction);
            em.flush();

        } catch (EntityExistsException e) {
            throw new EntryDuplicatedException();
        }
    }
}
