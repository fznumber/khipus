package main.com.encens.khipus.action.production;

import com.encens.hp90.framework.action.GenericAction;
import com.encens.hp90.model.production.MeasureUnit;
import com.encens.hp90.model.production.ProcessedProduct;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;

/**
 * Created with IntelliJ IDEA.
 * User: david
 * Date: 6/5/13
 * Time: 12:35 PM
 * To change this template use File | Settings | File Templates.
 */
@Name("measureUnitAction")
@Scope(ScopeType.CONVERSATION)
public class MeasureUnitProductionAction extends GenericAction<MeasureUnit> {

    @Factory(value = "measureUnit", scope = ScopeType.STATELESS)
    public MeasureUnit initMeasureUnit() {
        return getInstance();
    }

    @Override
    protected String getDisplayNameProperty() {
        return "name";
    }
}
