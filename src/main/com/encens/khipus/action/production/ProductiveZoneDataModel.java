package com.encens.khipus.action.production;

import com.encens.khipus.framework.action.QueryDataModel;
import com.encens.khipus.model.production.ProductiveZone;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.*;

import java.util.Arrays;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: david
 * Date: 22-05-13
 * Time: 05:30 PM
 * To change this template use File | Settings | File Templates.
 */
@Name("productiveZoneDataModel")
@Scope(ScopeType.PAGE)
public class ProductiveZoneDataModel extends QueryDataModel<Long, ProductiveZone> {

    private static final String[] RESTRICTIONS = {
            "lower(productiveZone.name) like concat('%', concat(lower(#{productiveZoneDataModel.criteria.name}), '%'))",
            "lower(productiveZone.number) like concat('%',#{productiveZoneDataModel.criteria.number}, '%')",
            "lower(productiveZone.group) like concat(#{productiveZoneDataModel.criteria.group}, '%')"
    };

    @Create
    public void init() {
        sortProperty = "productiveZone.number";
    }

    @Override
    public String getEjbql() {
        return "select productiveZone from ProductiveZone productiveZone";
    }

    @Override
    public List<String> getRestrictions() {
        return Arrays.asList(RESTRICTIONS);
    }
}
