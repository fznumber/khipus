package com.encens.khipus.action.production;

import com.encens.khipus.framework.action.QueryDataModel;
import com.encens.khipus.model.production.ProductiveZone;
import com.encens.khipus.model.production.RawMaterialProducer;
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
@Name("salaryMovementProducerDataModel")
@Scope(ScopeType.PAGE)
public class SalaryMovementProducerDataModel extends QueryDataModel<Long, SalaryMovementProducer> {

    private static final String[] RESTRICTIONS = {
            "lower(salaryMovementProducer.rawMaterialProducer.firstName) like concat('%',concat(#{salaryMovementProducerDataModel.criteria.rawMaterialProducer.name}, '%'))",
            "lower(salaryMovementProducer.rawMaterialProducer.lastName) like concat(#{salaryMovementProducerDataModel.criteria.rawMaterialProducer.lastName}, '%')",
            "lower(salaryMovementProducer.rawMaterialProducer.maidenName) like concat(#{salaryMovementProducerDataModel.criteria.rawMaterialProducer.maidenName}, '%')"
    };

    @Create
    public void init() {
        sortProperty = "salaryMovementProducer.date";
    }

    @Override
    public String getEjbql() {
        String query = " select salaryMovementProducer " +
                       " from SalaryMovementProducer salaryMovementProducer " +
                       " join salaryMovementProducer.rawMaterialProducer ";
        return query;
    }

    @Override
    public List<String> getRestrictions() {
        return Arrays.asList(RESTRICTIONS);
    }
}
