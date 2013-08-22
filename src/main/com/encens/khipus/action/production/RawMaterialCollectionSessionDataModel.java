package main.com.encens.khipus.action.production;

import com.encens.hp90.framework.action.QueryDataModel;
import com.encens.hp90.model.production.RawMaterialCollectionSession;
import com.encens.hp90.model.production.RawMaterialProducer;
import com.encens.hp90.model.production.ProductiveZone;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Name("rawMaterialCollectionSessionDataModel")
@Scope(ScopeType.PAGE)
public class RawMaterialCollectionSessionDataModel extends QueryDataModel<Long, RawMaterialCollectionSession> {
    private final static String[] RESTRICTIONS = {
            "session.date >= #{rawMaterialCollectionSessionDataModel.privateCriteria.startDate}",
            "session.date <= #{rawMaterialCollectionSessionDataModel.privateCriteria.endDate}",
            "productiveZone.name like concat(#{rawMaterialCollectionSessionDataModel.criteria.productiveZone.name}, '%')",
            "productiveZone.group like concat(#{rawMaterialCollectionSessionDataModel.criteria.productiveZone.group}, '%')",
            "productiveZone.number like concat(#{rawMaterialCollectionSessionDataModel.criteria.productiveZone.number}, '%')",
            "rawMaterialProducer.firstName like concat(#{rawMaterialCollectionSessionDataModel.privateCriteria.rawMaterialProducer.firstName}, '%')",
            "rawMaterialProducer.lastName like concat(#{rawMaterialCollectionSessionDataModel.privateCriteria.rawMaterialProducer.lastName}, '%')",
            "rawMaterialProducer.maidenName like concat(#{rawMaterialCollectionSessionDataModel.privateCriteria.rawMaterialProducer.maidenName}, '%')"
    };

    private PrivateCriteria privateCriteria;

    @Override
    public String getEjbql() {
        return "select distinct session " +
                "from RawMaterialCollectionSession session " +
                "left join fetch session.productiveZone productiveZone " +
                "left join session.collectedRawMaterialList collectedRawMaterial " +
                "left join collectedRawMaterial.rawMaterialProducer rawMaterialProducer";
    }

    @Override
    public String getSortProperty() {
        return "session.date";
    }

    @Override
    public RawMaterialCollectionSession createInstance() {
        RawMaterialCollectionSession session = super.createInstance();
        session.setProductiveZone(new ProductiveZone());
        return session;
    }

    @Override
    public List<String> getRestrictions() {
        return Arrays.asList(RESTRICTIONS);
    }

    public PrivateCriteria getPrivateCriteria() {
        if (privateCriteria == null) {
            privateCriteria = new PrivateCriteria();
        }
        return privateCriteria;
    }

    public static class PrivateCriteria {
        private Date startDate;
        private Date endDate;
        private RawMaterialProducer rawMaterialProducer = new RawMaterialProducer();

        public RawMaterialProducer getRawMaterialProducer() {
            return rawMaterialProducer;
        }

        public void setRawMaterialProducer(RawMaterialProducer rawMaterialProducer) {
            this.rawMaterialProducer = rawMaterialProducer;
        }

        public Date getStartDate() {
            return startDate;
        }

        public void setStartDate(Date startDate) {
            this.startDate = startDate;
        }

        public Date getEndDate() {
            return endDate;
        }

        public void setEndDate(Date endDate) {
            this.endDate = endDate;
        }
    }
}
