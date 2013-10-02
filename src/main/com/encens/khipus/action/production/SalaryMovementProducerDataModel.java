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
import java.util.Date;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: david
 * Date: 5/29/13
 * Time: 9:21 AM
 * To change this template use File | Settings | File Templates.
 */
@Name("salaryMovementProducerDataModel")
@Scope(ScopeType.PAGE)
public class SalaryMovementProducerDataModel extends QueryDataModel<Long, SalaryMovementProducer> {

    private static final String[] RESTRICTIONS = {
            "upper(rawMaterialProducer.firstName) like concat(concat('%',upper(#{salaryMovementProducerDataModel.criteria.rawMaterialProducer.firstName})), '%')",
            "upper(rawMaterialProducer.lastName) like concat(concat('%',upper(#{salaryMovementProducerDataModel.criteria.rawMaterialProducer.lastName})), '%')",
            "upper(rawMaterialProducer.maidenName) like concat(concat('%',upper(#{salaryMovementProducerDataModel.criteria.rawMaterialProducer.maidenName})), '%')"
    };

    @Create
    public void init() {
        sortProperty = "salaryMovementProducer.date";
    }

    @Override
    public String getEjbql() {
        String query = " select salaryMovementProducer " +
                       " from SalaryMovementProducer salaryMovementProducer " +
                       " left join fetch salaryMovementProducer.rawMaterialProducer rawMaterialProducer";
        return query;
    }

    @Override
    public List<String> getRestrictions() {
        return Arrays.asList(RESTRICTIONS);
    }

    @Override
    public SalaryMovementProducer createInstance() {
        SalaryMovementProducer salaryMovementGAB = super.createInstance();
        salaryMovementGAB.setRawMaterialProducer(new RawMaterialProducer());
        return salaryMovementGAB;
    }
}
