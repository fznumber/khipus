package com.encens.khipus.action.employees;

import com.encens.khipus.exception.EntryDuplicatedException;
import com.encens.khipus.framework.action.GenericAction;
import com.encens.khipus.framework.action.Outcome;
import com.encens.khipus.model.admin.Company;
import com.encens.khipus.model.employees.RHMark;
import com.encens.khipus.service.employees.RHMarkService;
import com.encens.khipus.util.Constants;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.*;
import org.jboss.seam.annotations.security.Restrict;
import org.jboss.seam.international.StatusMessage;
import org.jboss.seam.util.Reflections;


import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.List;

/**
 * Actions for RHMark
 *
 * @author
 */

@Name("rHMarkAction")
@Scope(ScopeType.CONVERSATION)
public class RHMarkAction extends GenericAction<RHMark> {

    @In("#{entityManager}")
    private EntityManager em;

    private Date dateRegister;
    private Object displayPropertyValueMarak;

    @Factory(value = "rHMark", scope = ScopeType.STATELESS)
    //@Restrict("#{s:hasPermission('RHMARK','VIEW')}")
    public RHMark initRHMark() {
        return getInstance();
    }

    @Override
    protected String getDisplayNameProperty() {
        return "marDate";
    }

    @Override
    @End(beforeRedirect=true)
    public String create() {
        try {
            RHMark rhMark = getInstance();

            List<Object[]> result = em.createQuery("select p from Person p where p.idNumber = :idPersona")
                    .setParameter("idPersona", rhMark.getMarPerId().toString()).getResultList();
            if(result.size()==0)
            {
                addNoFoundCIMessage();
                return Outcome.REDISPLAY;
            }
            rhMark.setCompany(new Company(Constants.defaultCompanyId, Constants.defaultCompanyName));
            rhMark.setSeat("Cochabamba");
            rhMark.setMarRefCard("referencia");
            rhMark.setMarIpPc("ip");
            getService().create(rhMark);
            addCreatedMessage();
            return Outcome.SUCCESS;
        } catch (NoResultException e) {
            addNoFoundCIMessage();
            return Outcome.REDISPLAY;
        } catch (EntryDuplicatedException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            return Outcome.REDISPLAY;
        }
    }

    protected void addNoFoundCIMessage() {
        facesMessages.addFromResourceBundle(StatusMessage.Severity.ERROR,
                "Common.message.idPerson", getDisplayPropertyValueMarak());
    }

    public Date getDateRegister() {
        Date date = new Date();
        if (null == dateRegister) {
            dateRegister = new Date();
            return dateRegister;
        }
        return dateRegister;
    }

    public void setDateRegister(Date dateRegister) {
        this.dateRegister = dateRegister;
    }

    public Object getDisplayPropertyValueMarak() {
        Object entity = getInstance();
        if (entity != null && getDisplayNameProperty() != null) {
            Method entityDisplayPropertyGetter = Reflections.getGetterMethod(entity.getClass(), getDisplayNameProperty());
            try {
                Object value = Reflections.invoke(entityDisplayPropertyGetter, entity);
                if (value != null) {
                    return value;
                } else {
                    return getDisplayNameMessage();
                }
            } catch (Exception e) {
                throw new RuntimeException("Error trying to recover the value of the entity for displayNameProperty");
            }
        } else {
            return getDisplayNameMessage();
        }
    }

}