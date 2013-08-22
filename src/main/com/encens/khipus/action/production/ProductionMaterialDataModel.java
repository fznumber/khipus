package main.com.encens.khipus.action.production;

import com.encens.hp90.framework.action.QueryDataModel;
import com.encens.hp90.model.production.ProductionMaterial;
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
 * Time: 4:07 PM
 * To change this template use File | Settings | File Templates.
 */
@Name("productionMaterialDataModel")
@Scope(ScopeType.PAGE)
public class ProductionMaterialDataModel extends QueryDataModel<Long, ProductionMaterial> {

    private static final String[] RESTRICTIONS = {
            "lower(productionMaterial.name) like concat(#{productionMaterialDataModel.criteria.name}, '%')",
            "lower(productionMaterial.code) like concat(#{productionMaterialDataModel.criteria.code}, '%')",
            "lower(productionMaterial.description) like concat(#{productionMaterialDataModel.criteria.description}, '%')"
    };

    @Create
    public void init() {
        sortProperty = "productionMaterial.name";
    }

    @Override
    public String getEjbql() {
        return "select productionMaterial " +
                "from ProductionMaterial productionMaterial " +
                "left join fetch productionMaterial.measureUnit";
    }

    @Override
    public List<String> getRestrictions() {
        return Arrays.asList(RESTRICTIONS);
    }
}
