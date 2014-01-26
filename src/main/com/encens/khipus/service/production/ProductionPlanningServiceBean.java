package com.encens.khipus.service.production;

import com.encens.khipus.exception.ConcurrencyException;
import com.encens.khipus.exception.EntryDuplicatedException;
import com.encens.khipus.exception.production.ProductCompositionException;
import com.encens.khipus.framework.service.ExtendedGenericServiceBean;
import com.encens.khipus.model.production.*;
import com.encens.khipus.model.warehouse.ProductItem;
import com.encens.khipus.model.warehouse.WarehouseDocumentType;
import com.encens.khipus.model.warehouse.WarehouseVoucherType;
import com.encens.khipus.util.Constants;
import com.encens.khipus.util.ValidatorUtil;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;

import javax.ejb.Stateless;
import javax.persistence.NoResultException;
import javax.persistence.OptimisticLockException;
import javax.persistence.PersistenceException;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

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
        ProductionPlanning pp = (ProductionPlanning) getEntityManager().createNamedQuery(query)
                .setParameter("id", id)
                .getSingleResult();
        return pp;
    }

    @Override
    protected Object preUpdate(Object entity) {
        try {
            ProductionPlanning planning = (ProductionPlanning) entity;
            executeMathematicalFormulas(planning);
            createVouchersForProductionInputs(planning);
            createVouchersForProductionOutputs(planning);
            return entity;
        } catch (Exception ex) {
            throw new PersistenceException(ex);
        }
    }

    private void executeMathematicalFormulas(ProductionPlanning planning) throws IOException, ProductCompositionException {
        for (ProductionOrder po : planning.getProductionOrderList()) {
            evaluatorMathematicalExpressionsService.executeMathematicalFormulas(po);
        }
    }

    private void createVouchersForProductionInputs(ProductionPlanning planning) throws IOException, ProductCompositionException {
        if (planning.getState() != EXECUTED)
            return;

        for (ProductionOrder po : planning.getProductionOrderList()) {
            if (po.getInputProductionVoucherList().size() > 0)
                continue;

            for (ProductionIngredient pi : po.getProductComposition().getProductionIngredientList()) {
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

        for (ProductionOrder po : planning.getProductionOrderList()) {
            if (po.getOutputProductionVoucherList().size() > 0)
                continue;

            OutputProductionVoucher voucher = new OutputProductionVoucher();
            voucher.setProductionOrder(po);
            voucher.setProducedAmount(po.getExpendAmount());
            voucher.setProcessedProduct(po.getProductComposition().getProcessedProduct());
            po.getOutputProductionVoucherList().add(voucher);
        }
    }

    public BigDecimal getMountInWarehouse(MetaProduct metaProduct) {
        try{
        return (BigDecimal) getEntityManager()
                .createQuery("SELECT sum(inventory.unitaryBalance) from Inventory inventory where inventory.productItem = :productItem")
                .setParameter("productItem", metaProduct.getProductItem())
                .getSingleResult();
        }catch (NoResultException e)
        {
            return new BigDecimal(0.0);
        }
    }

    public BigDecimal getMountInWarehouse(ProductItem productItem) {
        try{
        return (BigDecimal) getEntityManager()
                .createQuery("SELECT sum(inventory.unitaryBalance) from Inventory inventory where inventory.productItem = :productItem")
                .setParameter("productItem", productItem)
                .getSingleResult();
        }catch(NoResultException e)
        {
            return new BigDecimal(0.0);
        }
    }

    public BigDecimal getMountInWarehouse(Long id) {
        return (BigDecimal) getEntityManager()
                .createQuery("SELECT sum(inventory.unitaryBalance) from Inventory inventory where inventory.productItem.id = :id")
                .setParameter("id", id)
                .getSingleResult();
    }

    @Override
    public WarehouseDocumentType getDefaultDocumentType() {
        List<WarehouseDocumentType> warehouseDocumentTypeList = getEntityManager()
                .createNamedQuery("WarehouseDocumentType.findByType")
                .setParameter("companyNumber", Constants.defaultCompanyNumber)
                .setParameter("warehouseVoucherType", WarehouseVoucherType.C).getResultList();

        if (!ValidatorUtil.isEmptyOrNull(warehouseDocumentTypeList)) {
            return warehouseDocumentTypeList.get(0);
        }

        return null;
    }

    @Override
    public WarehouseDocumentType getRecepcionDocumentType() {
        List<WarehouseDocumentType> warehouseDocumentTypeList = getEntityManager()
                .createNamedQuery("WarehouseDocumentType.findByType")
                .setParameter("companyNumber", Constants.defaultCompanyNumber)
                .setParameter("warehouseVoucherType", WarehouseVoucherType.R).getResultList();

        if (!ValidatorUtil.isEmptyOrNull(warehouseDocumentTypeList)) {
            return warehouseDocumentTypeList.get(0);
        }

        return null;
    }

    @Override
    public void updateProductionPlanning(ProductionPlanning instance,ProductionOrder order) throws ConcurrencyException, EntryDuplicatedException {
        try {

            for(IndirectCosts costs:order.getIndirectCostses()){
                if(costs.getId() != null)
                {
                    getEntityManager().remove(costs);
                    //order.getIndirectCostses().remove(costs);
                }
            }
            getEntityManager().merge(instance);
            getEntityManager().flush();
        } catch (OptimisticLockException e) {
            throw new ConcurrencyException(e);
        } catch (PersistenceException ee) {
            throw new EntryDuplicatedException(ee);
        }
    }
    @Override
    public void deleteIndirectCost(ProductionOrder order)
    {
        getEntityManager().createNativeQuery("delete from costosindirectos where IDORDENPRODUCCION = :order")
                                            .setParameter("order",order)
                                            .executeUpdate();
    }

}