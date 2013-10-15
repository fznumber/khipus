package com.encens.khipus.action.production;

import com.encens.khipus.framework.action.QueryDataModel;
import com.encens.khipus.model.production.ProductiveZone;
import com.encens.khipus.model.production.RawMaterialPayRoll;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Name("rawMaterialPayRollDataModel")
@Scope(ScopeType.PAGE)
public class RawMaterialPayRollDataModel extends QueryDataModel<Long, RawMaterialPayRoll> {

    private PrivateCriteria privateCriteria;

    private static final String[] RESTRICTIONS = {
            "upper(productiveZone.name) like concat(concat('%',upper(#{rawMaterialPayRollDataModel.privateCriteria.productiveZone.name})), '%')",
            "upper(productiveZone.group) like concat(concat('%',upper(#{rawMaterialPayRollDataModel.privateCriteria.productiveZone.group})), '%')",
            "productiveZone.number like concat(#{rawMaterialPayRollDataModel.privateCriteria.productiveZone.number}, '%')",
            "rawMaterialPayRoll.startDate >= #{rawMaterialPayRollDataModel.privateCriteria.startDate}",
            "rawMaterialPayRoll.endDate <= #{rawMaterialPayRollDataModel.privateCriteria.endDate}"
    };

    @Create
    public void init() {
        sortProperty = "rawMaterialPayRoll.startDate";
    }

    @Override
    public String getEjbql() {
        String query = "select rawMaterialPayRoll " +
                " from RawMaterialPayRoll rawMaterialPayRoll " +
                " join fetch rawMaterialPayRoll.productiveZone productiveZone ";
        return query;
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
        private ProductiveZone productiveZone = new ProductiveZone();

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

        public ProductiveZone getProductiveZone() {
            return productiveZone;
        }

        public void setProductiveZone(ProductiveZone productiveZone) {
            this.productiveZone = productiveZone;
        }
    }
}