package com.encens.khipus.action.production;

import com.encens.khipus.framework.action.QueryDataModel;
import com.encens.khipus.model.production.ProductiveZone;
import com.encens.khipus.model.production.RawMaterialProducer;
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
@Name("rawMaterialProducerDataModel")
@Scope(ScopeType.PAGE)
public class RawMaterialProducerDataModel extends QueryDataModel<Long, RawMaterialProducer> {
    private RawMaterialProducer rawMaterialProducer;
    private static final String[] RESTRICTIONS =
            {"lower(rawMaterialProducer.lastName) like concat('%', concat(lower(#{rawMaterialProducerListModelPanel.criteria.lastName}), '%'))",
                    "lower(rawMaterialProducer.maidenName) like concat('%', concat(lower(#{rawMaterialProducerListModelPanel.criteria.maidenName}), '%'))",
                    "lower(rawMaterialProducer.firstName) like concat('%', concat(lower(#{rawMaterialProducerListModelPanel.criteria.firstName}), '%'))",
                    "rawMaterialProducer.idNumber like concat(#{rawMaterialProducerListModelPanel.criteria.idNumber}, '%')"};

    @Create
    public void init() {
        sortProperty = "rawMaterialProducer.lastName";
    }

    @Override
    public String getEjbql() {
        return "select rawMaterialProducer from RawMaterialProducer rawMaterialProducer";
    }

    @Override
    public List<String> getRestrictions() {
        return Arrays.asList(RESTRICTIONS);
    }

    public RawMaterialProducer getRawMaterialProducer() {
        return rawMaterialProducer;
    }

    public void setRawMaterialProducer(RawMaterialProducer rawMaterialProducer) {
        this.rawMaterialProducer = rawMaterialProducer;
    }
}
