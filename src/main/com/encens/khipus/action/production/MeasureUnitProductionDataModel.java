package main.com.encens.khipus.action.production;

import com.encens.hp90.framework.action.QueryDataModel;
import com.encens.hp90.model.production.MeasureUnit;
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
 * Time: 12:36 PM
 * To change this template use File | Settings | File Templates.
 */
@Name("measureUnitDataModel")
@Scope(ScopeType.PAGE)
public class MeasureUnitProductionDataModel extends QueryDataModel<Long, MeasureUnit> {

    private static final String[] RESTRICTIONS = {
            "lower(measureUnit.name) like concat(#{measureUnitDataModel.criteria.name}, '%')",
            "lower(measureUnit.description) like concat(#{measureUnitDataModel.criteria.description}, '%')"
    };

    @Create
    public void init() {
        sortProperty = "measureUnit.name";
    }

    @Override
    public String getEjbql() {
        String query =  "select measureUnit " +
                        "from MeasureUnit measureUnit ";
        return query;
    }

    @Override
    public List<String> getRestrictions() {
        return Arrays.asList(RESTRICTIONS);
    }
}
