package com.encens.khipus.service.production;

import com.encens.khipus.exception.production.ProductCompositionException;
import com.encens.khipus.framework.service.ExtendedGenericServiceBean;
import com.encens.khipus.model.production.*;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;

import javax.ejb.Stateless;
import javax.persistence.PersistenceException;
import java.io.IOException;
import java.math.BigDecimal;

import static com.encens.khipus.model.production.ProductionPlanningState.EXECUTED;


@Stateless
@Name("productionPlanningService")
@AutoCreate
public class ProductionPlanningServiceBean extends ExtendedGenericServiceBean implements ProductionPlanningService {

    @In
    private EvaluatorMathematicalExpressionsService evaluatorMathematicalExpressionsService;

    @Override
    public void refresh(Object entity) {
        getEntityManager().refresh(entity);
    }

    @Override
    public ProductionPlanning find(long id) {
        String query = "ProductionPlanning.widthProductionOrderAndProductCompositionAndProcessedProductFind";
        ProductionPlanning pp = (ProductionPlanning)getEntityManager().createNamedQuery(query)
                                                                       .setParameter("id", id)
                                                                       .getSingleResult();
        return pp;
    }

    @Override
    protected Object preUpdate(Object entity) {
        try {
            ProductionPlanning planning = (ProductionPlanning)entity;
            executeMathematicalFormulas(planning);
            createVouchersForProductionInputs(planning);
            createVouchersForProductionOutputs(planning);
            return entity;
        } catch (Exception ex) {
            throw new PersistenceException(ex);
        }
    }

    private void executeMathematicalFormulas(ProductionPlanning planning) throws IOException, ProductCompositionException {
        for(ProductionOrder po : planning.getProductionOrderList()) {
            evaluatorMathematicalExpressionsService.executeMathematicalFormulas(po);
        }
    }

    private void createVouchersForProductionInputs(ProductionPlanning planning) throws IOException, ProductCompositionException {
        if (planning.getState() != EXECUTED)
            return;

        for(ProductionOrder po : planning.getProductionOrderList()) {
            if (po.getInputProductionVoucherList().size() > 0)
                continue;

            for(ProductionIngredient pi : po.getProductComposition().getProductionIngredientList()) {
                InputProductionVoucher voucher = new InputProductionVoucher();
                voucher.setProductionOrder(po);
                voucher.setMetaProduct(pi.getMetaProduct());
                voucher.setAmount(pi.getAmount());
                po.getInputProductionVoucherList().add(voucher);
            }
        }
    }

    private void createVouchersForProductionOutputs(ProductionPlanning planning) {
        if (planning.getState() != EXECUTED)
            return;

        for(ProductionOrder po : planning.getProductionOrderList()) {
            if (po.getOutputProductionVoucherList().size() > 0)
                continue;

            OutputProductionVoucher voucher = new OutputProductionVoucher();
            voucher.setProductionOrder(po);
            voucher.setProducedAmount(po.getProducingAmount());
            voucher.setProcessedProduct(po.getProductComposition().getProcessedProduct());
            po.getOutputProductionVoucherList().add(voucher);
        }
    }

    public BigDecimal getMountInWarehouse(MetaProduct metaProduct)
    {
        return   (BigDecimal)getEntityManager()
                           .createQuery("SELECT inventory.unitaryBalance from Inventory inventory where inventory.productItem = :productItem")
                           .setParameter("productItem", metaProduct.getProductItem())
                           .getSingleResult();
    }
}