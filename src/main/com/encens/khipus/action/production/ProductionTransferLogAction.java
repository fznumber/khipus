package com.encens.khipus.action.production;

import com.encens.khipus.framework.action.GenericAction;
import com.encens.khipus.framework.action.Outcome;
import com.encens.khipus.framework.service.GenericService;
import com.encens.khipus.model.production.MetaProduct;
import com.encens.khipus.model.production.OutputProductionVoucher;
import com.encens.khipus.model.warehouse.*;
import com.encens.khipus.service.warehouse.ProductionTransferLogService;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.*;

import java.util.Calendar;
import java.util.List;

import static com.encens.khipus.model.warehouse.ProductionTransferLogState.DELIVERED;
import static com.encens.khipus.model.warehouse.ProductionTransferLogState.RECEIVED;
import static org.jboss.seam.international.StatusMessage.Severity.ERROR;
import static org.jboss.seam.international.StatusMessage.Severity.INFO;

@Name("productionTransferLogAction")
@Scope(ScopeType.CONVERSATION)
public class ProductionTransferLogAction extends GenericAction<ProductionTransferLog> {

    @In
    private ProductionTransferLogService productionTransferLogService;

    @Override
    protected GenericService getService() {
        return productionTransferLogService;
    }

    @Factory(value = "productionTransferLog", scope = ScopeType.STATELESS)
    public ProductionTransferLog initProductionTransferLog() {
        return getInstance();
    }

    @Override
    public ProductionTransferLog createInstance() {
        ProductionTransferLog anInstance = super.createInstance();
        anInstance.setState(ProductionTransferLogState.DRAFT);
        anInstance.setDeliveredDate(Calendar.getInstance().getTime());
        return anInstance;
    }

    @Override
    protected String getDisplayNameProperty() {
        return "deliveredDate";
    }

    public ProductionTransferLogState[] getStatesForDelivering() {
        return new ProductionTransferLogState[] { ProductionTransferLogState.DRAFT, DELIVERED };
    }

    public ProductionTransferLogState[] getStatesForReceiving() {
        return new ProductionTransferLogState[] { DELIVERED, ProductionTransferLogState.RECEIVED };
    }

    @Begin(flushMode = FlushModeType.MANUAL, ifOutcome = Outcome.SUCCESS)
    public String selectForDelivering(ProductionTransferLog instance) {
        String outcome = super.select(instance);
        if (outcome == Outcome.SUCCESS && getInstance().getState() == ProductionTransferLogState.DRAFT) {
            getInstance().setDeliveredDate(Calendar.getInstance().getTime());
        }
        return outcome;
    }

    @Begin(flushMode = FlushModeType.MANUAL, ifOutcome = Outcome.SUCCESS)
    public String selectForReceiving(ProductionTransferLog instance) {
        String outcome = super.select(instance);
        if (outcome == Outcome.SUCCESS && getInstance().getState() == DELIVERED) {
            getInstance().setReceivedDate(Calendar.getInstance().getTime());
            productionTransferLogService.prepareForReceiving(getInstance());
        }
        return outcome;
    }

    @Begin(flushMode = FlushModeType.MANUAL, ifOutcome = Outcome.SUCCESS)
    public String createNew() {
        getInstance().setDeliveredDate(Calendar.getInstance().getTime());
        getInstance().setState(DELIVERED);
        return Outcome.SUCCESS;
    }

    @Override
    @End(ifOutcome = Outcome.SUCCESS)
    public String update() {
        ProductionTransferLog transferLog = getInstance();
        transferLog.setState(RECEIVED);
        for(IncomingProductionOrder ipo : transferLog.getIncomingProductionOrderList()) {
            ipo.getFinishedGoodsInventory().setDate(transferLog.getReceivedDate());
        }
        return super.update();
    }

    public List<WarehouseSlot> findAvailableWarehouseSlots(MetaProduct product) {
        return productionTransferLogService.findWarehouseSlots(product);
    }

    public void selectOutputProductionVoucher(OutputProductionVoucher outputProductionVoucher) {
        try {
            if (thereIs(outputProductionVoucher)) {
                facesMessages.addFromResourceBundle(INFO, "ProductionTransferLog.info.theProductionOrderIsCurrentlyAdded");
                return;
            }

            outputProductionVoucher = getService().findById(OutputProductionVoucher.class, outputProductionVoucher.getId());
            IncomingProductionOrder incomingProductionOrder = new IncomingProductionOrder();
            incomingProductionOrder.setDeliveredAmount(outputProductionVoucher.getProducedAmount());
            incomingProductionOrder.setOutputProductionVoucher(outputProductionVoucher);
            outputProductionVoucher.setIncomingProductionOrder(incomingProductionOrder);
            incomingProductionOrder.setProductionTransferLog(getInstance());
            getInstance().getIncomingProductionOrderList().add(incomingProductionOrder);
        } catch (Exception other) {
            facesMessages.addFromResourceBundle(ERROR, "Common.globalError.description");
        }
    }

    private boolean thereIs(OutputProductionVoucher outputProductionVoucher) {
        for(IncomingProductionOrder po : getInstance().getIncomingProductionOrderList()) {
            if (outputProductionVoucher.getId().equals(po.getOutputProductionVoucher().getId())) {
                return true;
            }
        }
        return false;
    }

    public void remove(IncomingProductionOrder incomingProductionOrder) {
        getInstance().getIncomingProductionOrderList().remove(incomingProductionOrder);
    }
}
