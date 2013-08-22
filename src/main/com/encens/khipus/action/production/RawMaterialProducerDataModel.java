package main.com.encens.khipus.action.production;

import com.encens.hp90.framework.action.QueryDataModel;
import com.encens.hp90.model.production.RawMaterialProducer;
import com.encens.hp90.model.production.ProductiveZone;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;

import java.util.Arrays;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: david                                                          ç
 * Date: 5/29/13
 * Time: 9:21 AM
 * To change this template use File | Settings | File Templates.
 */
@Name("rawMaterialProducerDataModel")
@Scope(ScopeType.PAGE)
public class RawMaterialProducerDataModel extends QueryDataModel<Long, RawMaterialProducer> {

    private static final String[] RESTRICTIONS = {
        "lower(rawMaterialProducer.firstName) like concat(#{rawMaterialProducerDataModel.criteria.firstName}, '%')",
        "lower(rawMaterialProducer.lastName) like concat(#{rawMaterialProducerDataModel.criteria.lastName}, '%')",
        "lower(rawMaterialProducer.maidenName) like concat(#{rawMaterialProducerDataModel.criteria.maidenName}, '%')",
        "lower(rawMaterialProducer.productiveZone.group) like concat(#{rawMaterialProducerDataModel.criteria.productiveZone.group}, '%')",
        "lower(rawMaterialProducer.productiveZone.name) like concat(#{rawMaterialProducerDataModel.criteria.productiveZone.name}, '%')",
        "lower(rawMaterialProducer.productiveZone.number) like concat(#{rawMaterialProducerDataModel.criteria.productiveZone.number}, '%')"
    };

    @Create
    public void init() {
        sortProperty = "rawMaterialProducer.lastName";
    }

    @Override
    public String getEjbql() {
        String query = "select rawMaterialProducer " +
                       "from RawMaterialProducer rawMaterialProducer " +
                       "left join fetch rawMaterialProducer.productiveZone ";
        return query;
    }

    @Override
    public List<String> getRestrictions() {
        return Arrays.asList(RESTRICTIONS);
    }

    @Override
    public RawMaterialProducer createInstance() {
        RawMaterialProducer rawMaterialProducer = super.createInstance();
        if (rawMaterialProducer.getProductiveZone() == null) {
            rawMaterialProducer.setProductiveZone(new ProductiveZone());
        }
        return rawMaterialProducer;
    }
}
