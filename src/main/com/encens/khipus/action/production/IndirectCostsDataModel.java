package com.encens.khipus.action.production;

import com.encens.khipus.framework.action.QueryDataModel;
import com.encens.khipus.model.employees.Gestion;
import com.encens.khipus.model.employees.Month;
import com.encens.khipus.model.production.IndirectCosts;
import com.encens.khipus.model.production.IndirectCostsConfig;
import com.encens.khipus.service.employees.GestionService;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.security.Restrict;

import java.util.Calendar;

/**
 * Created by Diego on 08/07/2014.
 */
@Name("indirectCostsDataModel")
@Scope(ScopeType.PAGE)
public class IndirectCostsDataModel extends QueryDataModel<Long,IndirectCosts> {
    @In
    private GestionService gestionService;

    private Gestion gestion;

    private Integer monthInt;

    private Month month;

    private static final String[] RESTRICTIONS = {
             "indirectCosts.name = #{indirectCostsDataModel.criteria.name}"
            ,"indirectCosts.periodIndirectCost.gestion = #{indirectCostsDataModel.gestion}"
            ,"indirectCosts.periodIndirectCost.month = #{indirectCostsDataModel.monthInt}"
            ,"indirectCosts.productionOrder is null"
    };

    @Override
    public String getEjbql(){
        return "select indirectCosts from IndirectCosts indirectCosts ";
    }

    public Gestion getGestion() {
        if(gestion == null)
            gestion = gestionService.getLastGestion();

        return gestion;
    }

    public void setGestion(Gestion gestion) {
        this.gestion = gestion;
    }

    public void cleanGestionList() {
        setGestion(null);
    }

    public Integer getMonthInt() {
        return monthInt;
    }

    public void setMonthInt(Integer monthInt) {
        this.monthInt = monthInt;
    }

    public Month getMonth() {
        return month;
    }

    public void setMonth(Month month) {
        monthInt = month.getValue()+1;
        this.month = month;
    }
}
