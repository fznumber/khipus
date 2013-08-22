package com.encens.khipus.action.production;

import com.encens.khipus.framework.action.QueryDataModel;
import com.encens.khipus.model.production.ProcessedProduct;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;

import java.util.Arrays;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: david
 * Date: 6/5/13
 * Time: 10:41 AM
 * To change this template use File | Settings | File Templates.
 */
@Name("processedProductDataModel")
@Scope(ScopeType.PAGE)
public class ProcessedProductDataModel extends QueryDataModel<Long, ProcessedProduct> {
    private static final String[] RESTRICTIONS = {
            "lower(processedProduct.name) like concat(#{processedProductDataModel.criteria.name}, '%')",
            "lower(processedProduct.code) like concat(#{processedProductDataModel.criteria.code}, '%')",
            "lower(processedProduct.description) like concat(#{processedProductDataModel.criteria.description}, '%')"
    };

    @Create
    public void init() {
        sortProperty = "processedProduct.name";
    }

    @Override
    public String getEjbql() {
        return "select processedProduct from ProcessedProduct processedProduct";
    }

    @Override
    public List<String> getRestrictions() {
        return Arrays.asList(RESTRICTIONS);
    }
}
