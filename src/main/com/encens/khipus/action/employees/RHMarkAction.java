package com.encens.khipus.action.employees;

import com.encens.khipus.framework.action.GenericAction;
import com.encens.khipus.model.employees.RHMark;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.security.Restrict;

/**
 * Actions for RHMark
 *
 * @author
 */

@Name("rHMarkAction")
@Scope(ScopeType.CONVERSATION)
public class RHMarkAction extends GenericAction<RHMark> {

    @Factory(value = "rHMark", scope = ScopeType.STATELESS)
    @Restrict("#{s:hasPermission('RHMARK','VIEW')}")
    public RHMark initRHMark() {
        return getInstance();
    }

    @Override
    protected String getDisplayNameProperty() {
        return "marDate";
    }
}