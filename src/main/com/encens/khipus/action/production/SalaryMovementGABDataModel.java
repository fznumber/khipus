package com.encens.khipus.action.production;

import com.encens.khipus.framework.action.QueryDataModel;
import com.encens.khipus.model.production.ProductiveZone;
import com.encens.khipus.model.production.RawMaterialProducer;
import com.encens.khipus.model.production.SalaryMovementGAB;
import com.encens.khipus.model.production.SalaryMovementProducer;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;

import java.util.Arrays;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: david
 * Date: 5/29/13
 * Time: 9:21 AM
 * To change this template use File | Settings | File Templates.
 */
@Name("salaryMovementGABDataModel")
@Scope(ScopeType.PAGE)
public class SalaryMovementGABDataModel extends QueryDataModel<Long, SalaryMovementGAB> {

    private static final String[] RESTRICTIONS = {
            "upper(productiveZone.number) like concat(upper(concat('%',#{salaryMovementGABDataModel.criteria.productiveZone.number})), '%')",
            "upper(productiveZone.group) like concat(upper(concat('%',#{salaryMovementGABDataModel.criteria.productiveZone.group})), '%')",
            "upper(productiveZone.name) like concat(upper(concat('%',#{salaryMovementGABDataModel.criteria.productiveZone.name})), '%')",
    };

    @Create
    public void init() {
        sortProperty = "salaryMovementGAB.date";
    }

    @Override
    public String getEjbql() {
        String query = " select salaryMovementGAB " +
                       " from SalaryMovementGAB salaryMovementGAB " +
                       " left join fetch salaryMovementGAB.productiveZone productiveZone";
        return query;
    }

    @Override
    public SalaryMovementGAB createInstance() {
        SalaryMovementGAB salaryMovementGAB = super.createInstance();
        salaryMovementGAB.setProductiveZone(new ProductiveZone());
        return salaryMovementGAB;
    }

    @Override
    public List<String> getRestrictions() {
        return Arrays.asList(RESTRICTIONS);
    }

}
