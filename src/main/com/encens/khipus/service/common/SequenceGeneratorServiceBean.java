package com.encens.khipus.service.common;

import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;

import javax.ejb.EJBException;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.persistence.OptimisticLockException;

import static javax.ejb.TransactionAttributeType.REQUIRES_NEW;

/**
 * SequenceGeneratorServiceBean
 *
 * @author
 * @version 2.0
 */
@Name("sequenceGeneratorService")
@Stateless
@AutoCreate
public class SequenceGeneratorServiceBean implements SequenceGeneratorService {

    @In
    private SequenceService sequenceService;

    public long nextValue(String sequenceName) {
        int tries = 0;
        final int maxTries = 10;
        long nextValue = -1;
        do {
            try {
                nextValue = sequenceService.createOrUpdateNextSequenceValue(sequenceName);
            } catch (EJBException e) {
                if (e.getCausedByException() instanceof OptimisticLockException) {
                    tries++;
                } else {
                    throw e;
                }
            }
        } while (nextValue < 0 && tries < maxTries);

        if (nextValue < 0) {
            final String msg = "Exceeded maxTries=" + maxTries + " attempts";
            throw new EJBException(msg);
        }

        return nextValue;
    }

    @TransactionAttribute(REQUIRES_NEW)
    public long findNextSequenceValue(String sequenceName) {
        int tries = 0;
        final int maxTries = 10;
        long nextValue = -1;
        do {
            try {
                nextValue = sequenceService.findNextSequenceValue(sequenceName);
            } catch (EJBException e) {
                if (e.getCausedByException() instanceof OptimisticLockException) {
                    tries++;
                } else {
                    throw e;
                }
            }
        } while (nextValue < 0 && tries < maxTries);

        if (nextValue < 0) {
            final String msg = "Exceeded maxTries=" + maxTries + " attempts";
            throw new EJBException(msg);
        }

        return nextValue;
    }

    /* This method has declared the annotation @TransactionAttribute so when this method has called from somewhere,
    * a transaction will begin then the value of next sequence will be saved when the transaction is ended.
    */
    @TransactionAttribute(REQUIRES_NEW)
    public long forceNextValue(String sequenceName) {
        return nextValue(sequenceName);
    }
}
