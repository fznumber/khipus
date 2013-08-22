package main.com.encens.khipus.action.production;

import com.encens.hp90.framework.action.GenericAction;
import com.encens.hp90.model.production.ProcessedProduct;
import com.encens.hp90.model.production.ProductiveZone;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;

/**
 * Created with IntelliJ IDEA.
 * User: david
 * Date: 6/5/13
 * Time: 10:39 AM
 * To change this template use File | Settings | File Templates.
 */
@Name("processedProductAction")
@Scope(ScopeType.CONVERSATION)
public class ProcessedProductAction extends GenericAction<ProcessedProduct> {

    @Factory(value = "processedProduct", scope = ScopeType.STATELESS)
    public ProcessedProduct initProcessedProduct() {
        return getInstance();
    }

    @Override
    protected String getDisplayNameProperty() {
        return "name";
    }
}
