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
import java.util.ArrayList;
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
    //todo: buscar a la leche cruda y usar su id
    public Double calculateTotalMilk(ProductionPlanning planning){
     BigDecimal total_order = BigDecimal.ZERO;
     BigDecimal total_repro = BigDecimal.ZERO;
        try {
            total_order = (BigDecimal)getEntityManager().createNativeQuery("select nvl(sum(oi.cantidad),0) from ordenproduccion op\n" +
                    "inner join ordeninsumo oi\n" +
                    "on oi.idordenproduccion = op.idordenproduccion\n" +
                    "where oi.cod_art = '26'\n" +
                    "and oi.no_cia = '01'\n" +
                    "and op.idplanificacionproduccion = :planning")
                    .setParameter("planning",planning)
                    .getSingleResult();
        }catch (NoResultException e){
            total_order = BigDecimal.ZERO;
        }

        try {
            total_repro = (BigDecimal)getEntityManager().createNativeQuery("select nvl(sum(oi.cantidad),0) from productobase pb\n" +
                    "inner join ordeninsumo oi\n" +
                    "on oi.idproductobase = pb.idproductobase\n" +
                    "where oi.cod_art = '26'\n" +
                    "and oi.no_cia = '01'\n" +
                    "and pb.idplanificacionproduccion= :planning")
                    .setParameter("planning",planning)
                    .getSingleResult();
        }catch (NoResultException e){
            total_repro = BigDecimal.ZERO;
        }

        return total_repro.doubleValue() + total_order.doubleValue();
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
        BigDecimal result = BigDecimal.ZERO;
        try{
            result = (BigDecimal) getEntityManager()
                .createQuery("SELECT sum(inventory.unitaryBalance) from Inventory inventory where inventory.productItem = :productItem")
                .setParameter("productItem", productItem)
                .getSingleResult();

        }catch(NoResultException e)
        {
            return new BigDecimal(0.0);
        }
        if(result == null)
            return BigDecimal.ZERO;

        return result;
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

    @Override
    public void deleteIndirectCost(SingleProduct singleProduct)
    {
        getEntityManager().createNativeQuery("delete from costosindirectos where IDPRODUCTOSIMPLE = :id")
                .setParameter("id",singleProduct.getId())
                .executeUpdate();
        //getEntityManager().remove(singleProduct.getIndirectCostses());
    }

    @Override
    public List<IndirectCosts> findIndirectCostFromSingle(SingleProduct singleProduct) {
        List<IndirectCosts> indirectCostses = new ArrayList<IndirectCosts>();

        try{
            indirectCostses = getEntityManager().createQuery("select indirectCosts from IndirectCosts indirectCosts where indirectCosts.singleProduct = :singleProduct")
                              .setParameter("singleProduct",singleProduct)
                              .getResultList();

        }catch( NoResultException e )
        {
            return new ArrayList<IndirectCosts>();
        }

        return indirectCostses;
    }

    @Override
    public void updateOrder(ProductionOrder order) {
        getEntityManager().createNativeQuery("update ORDENPRODUCCION set PORCENTAJEGRASA = :percentage where IDORDENPRODUCCION = :order")
                          .setParameter("percentage",order.getGreasePercentage())
                          .setParameter("order",order)
                          .executeUpdate();
    }

    @Override
    public void addIndirectCostToSingleProduct(IndirectCosts indirectCosts) {
        getEntityManager().persist(indirectCosts);
    }

}