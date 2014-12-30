package com.encens.khipus.action.customers;

import com.encens.khipus.framework.action.QueryDataModel;
import com.encens.khipus.model.customers.Credit;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.security.Restrict;

/**
 * Data model for Credit
 *
 * @author:
 */

@Name("creditDataModel")
@Scope(ScopeType.PAGE)
@Restrict("#{s:hasPermission('CREDIT','VIEW')}")
public class ClientOrderDataModel extends QueryDataModel<Long, Credit> {

    @Override
    public String getEjbql() {
        return "select credit from Credit credit";
    }

    /*@Override
    public List<String> getRestrictions() {
        return Arrays.asList(RESTRICTIONS);
    } */
}
