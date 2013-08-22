package com.encens.khipus.action.production;

import com.encens.khipus.framework.action.QueryDataModel;
import com.encens.khipus.model.production.ProductiveZone;
import com.encens.khipus.model.production.RawMaterialProducer;
import com.encens.khipus.model.production.RawMaterialRejectionNote;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;

import java.util.Arrays;
import java.util.List;

@Name("rawMaterialRejectionNoteDataModel")
@Scope(ScopeType.PAGE)
public class RawMaterialRejectionNoteDataModel extends QueryDataModel<Long, RawMaterialRejectionNote> {
    private final static String[] RESTRICTIONS = {
            "lower(rawMaterialProducer.firstName) like concat(#{rawMaterialProducerForSearch.firstName}, '%')",
            "lower(rawMaterialProducer.lastName) like concat(#{rawMaterialProducerForSearch.lastName}, '%')",
            "lower(rawMaterialProducer.maidenName) like concat(#{rawMaterialProducerForSearch.maidenName}, '%')",
            "lower(productiveZone.name) like concat(#{productiveZoneForSearch.name}, '%')",
            "lower(productiveZone.group) like concat(#{productiveZoneForSearch.group}, '%')",
            "lower(productiveZone.number) like concat(#{productiveZoneForSearch.number}, '%')"
    };

    @Factory(value = "productiveZoneForSearch", scope = ScopeType.PAGE)
    public ProductiveZone initProductiveZone() {
        return new ProductiveZone();
    }

    @Factory(value = "rawMaterialProducerForSearch", scope = ScopeType.PAGE)
    public RawMaterialProducer initRawMaterialProducer() {
        return new RawMaterialProducer();
    }

    @Create
    public void init() {
        sortProperty = "rawMaterialRejectionNote.rawMaterialProducer.lastName";
    }

    @Override
    public String getEjbql() {
        return "select rawMaterialRejectionNote " +
                "from RawMaterialRejectionNote rawMaterialRejectionNote " +
                "left join fetch rawMaterialRejectionNote.rawMaterialProducer rawMaterialProducer " +
                "left join fetch rawMaterialProducer.productiveZone productiveZone";
    }

    @Override
    public List<String> getRestrictions() {
        return Arrays.asList(RESTRICTIONS);
    }
}
