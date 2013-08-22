package com.encens.khipus.action.production;

import com.encens.khipus.framework.action.QueryDataModel;
import com.encens.khipus.model.production.ProductionOrder;
import com.encens.khipus.model.warehouse.ProductionTransferLog;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Name("productionTransferLogDataModel")
@Scope(ScopeType.PAGE)
public class ProductionTransferLogDataModel extends QueryDataModel<Long, ProductionTransferLog> {
    private PrivateCriteria privateCriteria;

    private final static String[] RESTRICTIONS = {
        "productionOrder.code like concat(#{productionTransferLogDataModel.privateCriteria.productionOrder.code}}",
        "productionTransferLog.deliveredDate >= #{productionTransferLogDataModel.privateCriteria.startDate}",
        "productionTransferLog.deliveredDate <= #{productionTransferLogDataModel.privateCriteria.endDate}",
        "productionTransferLog.state = #{productionTransferLogDataModel.criteria.state}",
        "productionTransferLog.state <> #{productionTransferLogDataModel.privateCriteria.excludedState}"
    };

    @Create
    public void init() {
        sortProperty = "productionTransferLog.deliveredDate";
    }

    @Override
    public String getEjbql() {
        return "select distinct productionTransferLog " +
                "from ProductionTransferLog productionTransferLog " +
                "left join productionTransferLog.incomingProductionOrderList incomingProductionOrder " +
                "left join incomingProductionOrder.outputProductionVoucher outputProductionVoucher " +
                "left join outputProductionVoucher.productionOrder productionOrder ";
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
        private ProductionOrder productionOrder;
        private Date startDate;
        private Date endDate;
        private ProductionTransferLog excludedState;

        public PrivateCriteria() {
            productionOrder = new ProductionOrder();
        }

        public ProductionOrder getProductionOrder() {
            return productionOrder;
        }

        public void setProductionOrder(ProductionOrder productionOrder) {
            this.productionOrder = productionOrder;
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

        public ProductionTransferLog getExcludedState() {
            return excludedState;
        }

        public void setExcludedState(ProductionTransferLog excludedState) {
            this.excludedState = excludedState;
        }
    }
}
