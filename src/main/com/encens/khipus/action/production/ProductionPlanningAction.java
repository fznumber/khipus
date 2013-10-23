package com.encens.khipus.action.production;

import com.encens.khipus.action.production.reports.ProductionPlanningReportAction;
import com.encens.khipus.action.reports.GenericReportAction;
import com.encens.khipus.framework.action.GenericAction;
import com.encens.khipus.framework.action.Outcome;
import com.encens.khipus.framework.service.GenericService;
import com.encens.khipus.model.production.*;
import com.encens.khipus.service.production.EvaluatorMathematicalExpressionsService;
import com.encens.khipus.service.production.ProcessedProductService;
import com.encens.khipus.service.production.ProductionPlanningService;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.*;

import javax.faces.event.ActionEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.encens.khipus.model.production.ProductionPlanningState.*;
import static org.jboss.seam.international.StatusMessage.Severity.ERROR;


@Name("productionPlanningAction")
@Scope(ScopeType.CONVERSATION)
public class ProductionPlanningAction extends GenericAction<ProductionPlanning> {

    private ProcessedProduct processedProduct;
    private ProductComposition productComposition;
    private ProductionOrder productionOrder;
    private Formulation existingFormulation;

    private FormulaState formulaState = FormulaState.NONE;

    @In
    private ProductionPlanningService productionPlanningService;
    @In
    private ProcessedProductService processedProductService;
    @In
    private EvaluatorMathematicalExpressionsService evaluatorMathematicalExpressionsService;
    @In
    private ProductionOrderCodeGenerator productionOrderCodeGenerator;

    @Override
    protected GenericService getService() {
        return productionPlanningService;
    }

    public FormulaState getFormulaState() {
        return formulaState;
    }

    @Create
    public void createdComponent() {
        clearFormulation();
    }

    @Factory(value = "productionPlanning", scope = ScopeType.STATELESS)
    public ProductionPlanning initProductionPanning() {
        return getInstance();
    }

    @Factory(value = "productCompositionForPlanning", scope = ScopeType.STATELESS)
    public ProductComposition initProductComposition() {
        return productComposition;
    }

    @Factory(value = "productionOrderForPlanning", scope = ScopeType.STATELESS)
    public ProductionOrder initProductionOrder() {
        return productionOrder;
    }

    @Factory(value = "processedProductForPlanning", scope = ScopeType.STATELESS)
    public ProcessedProduct initProcessedProduct() {
        return processedProduct;
    }

    @Begin(ifOutcome = Outcome.SUCCESS, flushMode = FlushModeType.MANUAL)
    public String createNew() {
        return Outcome.SUCCESS;
    }

    public void initEditFormula() {
        clearFormulation();
        formulaState = FormulaState.NEW;
        productionOrder.setCode(productionOrderCodeGenerator.generateCode());
        productionOrder.setProducingAmount(0.0);
    }

    @Begin(ifOutcome = Outcome.SUCCESS, flushMode = FlushModeType.MANUAL)
    public String select(ProductionPlanning productionPlanning) {
        try {
            ProductionPlanning aux = productionPlanningService.find(productionPlanning.getId());
            setInstance(aux);
            setOp(OP_UPDATE);
            return Outcome.SUCCESS;
        } catch (Exception ex) {
            log.error(ex);
            facesMessages.addFromResourceBundle(ERROR, "Common.globalError.description");
            return Outcome.REDISPLAY;
        }
    }

    public List<Consolidated> getConsolidatedInputs() {
        try {
            ProductionPlanning productionPlanning = getInstance();
            Map<Long, Consolidated> consolidated = new HashMap<Long, Consolidated>();
            for (ProductionOrder order : productionPlanning.getProductionOrderList()) {
                evaluatorMathematicalExpressionsService.executeMathematicalFormulas(order);
                for (ProductionIngredient ingredient : order.getProductComposition().getProductionIngredientList()) {
                    Consolidated aux = consolidated.get(ingredient.getMetaProduct().getId());
                    if (aux == null) {
                        aux = new Consolidated();
                        aux.setProduct(ingredient.getMetaProduct());
                        consolidated.put(ingredient.getMetaProduct().getId(), aux);
                    }

                    aux.setAmount(aux.getAmount() + ingredient.getAmount());
                }
            }
            return new ArrayList<Consolidated>(consolidated.values());
        } catch (Exception ex) {
            log.error("Exception caught", ex);
            facesMessages.addFromResourceBundle(ERROR, "Common.globalError.description");
            return new ArrayList<Consolidated>();
        }
    }

    public double getTotalWeight() {
        if (productionOrder.getProductComposition() == null) return 0.0;

        double total = 0.0;
        for (ProductionIngredient ingredient : productionOrder.getProductComposition().getProductionIngredientList()) {
            //total += ingredient.getAmount();
            if (ingredient.getMetaProduct().getProductItem().getUsageMeasureCode().equals("GR"))
                total = total + ingredient.getAmount() / 1000.0;
            else
                total = total + ingredient.getAmount();
        }
        return total;
    }

    public void productCompositionSelected(ActionEvent e) {
        try {
            productionOrder.setProducingAmount(productComposition.getProducingAmount());
            productionOrder.setContainerWeight(productComposition.getContainerWeight());
            productionOrder.setSupposedAmount(productComposition.getSupposedAmount());
            productionOrder.setProductComposition(productComposition);
            evaluatorMathematicalExpressionsService.executeMathematicalFormulas(productionOrder);
        } catch (Exception ex) {
            log.error("Exception caught", ex);
            facesMessages.addFromResourceBundle(ERROR, "Common.globalError.description");
        }
    }

    public void addFormulation() {
        ProductionPlanning productionPlanning = getInstance();
        productionPlanning.getProductionOrderList().add(productionOrder);
        productionOrder.setProductionPlanning(productionPlanning);

        clearFormulation();
        disableEditingFormula();
    }

    public void evaluateExpressionActionListener(ActionEvent e) {
        evaluateMathematicalExpression();
    }

    private boolean evaluateMathematicalExpression() {
        try {
            evaluatorMathematicalExpressionsService.executeMathematicalFormulas(productionOrder);
            return true;
        } catch (Exception ex) {
            log.error("Exception caught", ex);
            facesMessages.addFromResourceBundle(ERROR, "Common.globalError.description");
            return false;
        }
    }

    public void removeFormulation() {
        ProductionPlanning productionPlanning = getInstance();
        for (ProductionOrder po : productionPlanning.getProductionOrderList()) {
            if (po.getCode().equals(productionOrder.getCode())) {
                productionPlanning.getProductionOrderList().remove(po);
                clearFormulation();
                break;
            }
        }

        disableEditingFormula();
    }

    public void selectProcessedProduct(ProcessedProduct processedProduct) {
        try {
            this.processedProduct = processedProductService.find(processedProduct.getId());
            this.productComposition = new ProductComposition();
        } catch (Exception ex) {
            log.error(ex);
            facesMessages.addFromResourceBundle(ERROR, "Common.globalError.description");
        }
    }

    public void selectResultProcessedProduct(ProcessedProduct processedProduct) {
        try {
            processedProduct = getService().findById(ProcessedProduct.class, processedProduct.getId());

            OutputProductionVoucher outputProductionVoucher = new OutputProductionVoucher();
            outputProductionVoucher.setProcessedProduct(processedProduct);
            outputProductionVoucher.setProducedAmount(0.0);
            outputProductionVoucher.setProductionOrder(productionOrder);
            productionOrder.getOutputProductionVoucherList().add(outputProductionVoucher);
        } catch (Exception ex) {
            log.error(ex);
            facesMessages.addFromResourceBundle(ERROR, "Common.globalError.description");
        }
    }

    public void select(ProductionOrder productionOrder) {

        cancelFormulation();

        existingFormulation = new Formulation();
        existingFormulation.producingAmount = productionOrder.getProducingAmount();
        existingFormulation.productComposition = productionOrder.getProductComposition();

        this.productionOrder = productionOrder;
        this.productComposition = productionOrder.getProductComposition();
        this.processedProduct = productComposition.getProcessedProduct();

        evaluateMathematicalExpression();
        formulaState = FormulaState.EDIT;
    }

    public void clearFormulation() {
        processedProduct = new ProcessedProduct();
        productComposition = new ProductComposition();
        productionOrder = new ProductionOrder();
    }

    public void updateFormulation() {
        if (evaluateMathematicalExpression() == false) {
            return;
        }

        existingFormulation = null;
        disableEditingFormula();
    }

    public void updateProducedAmount() {
        if (update() != Outcome.SUCCESS) {
            return;
        }

        existingFormulation = null;
        disableEditingFormula();
    }

    public void cancelFormulation() {
        if (existingFormulation != null) {
            productionOrder.setProductComposition(existingFormulation.productComposition);
            productionOrder.setProducingAmount(existingFormulation.producingAmount);
            existingFormulation = null;
        }

        disableEditingFormula();

        if (productionOrder.getId() != null) {

            List<OutputProductionVoucher> fakeVouchers = new ArrayList<OutputProductionVoucher>();
            for (OutputProductionVoucher voucher : productionOrder.getOutputProductionVoucherList()) {
                if (voucher.getId() == null) {
                    fakeVouchers.add(voucher);
                }
            }

            for (OutputProductionVoucher voucher : fakeVouchers) {
                productionOrder.getOutputProductionVoucherList().remove(voucher);
            }

            productionPlanningService.refresh(productionOrder);
        }
    }

    private void disableEditingFormula() {
        formulaState = FormulaState.NONE;
    }

    @End(ifOutcome = Outcome.SUCCESS)
    public String makeExecuted() {
        getInstance().setState(EXECUTED);
        String outcome = update();

        if (outcome != Outcome.SUCCESS) {
            getInstance().setState(PENDING);
        }
        return outcome;
    }

    @End(ifOutcome = Outcome.SUCCESS)
    public String makeFinalized() {
        getInstance().setState(FINALIZED);
        String outcome = update();

        if (outcome != Outcome.SUCCESS) {
            getInstance().setState(EXECUTED);
        }
        return outcome;
    }

    public ProductComposition getProductComposition() {
        return productComposition;
    }

    public void setProductComposition(ProductComposition productComposition) {
        this.productComposition = productComposition;
    }

    public List<ProductComposition> getProductCompositionList() {
        List<ProductComposition> productCompositionList = new ArrayList<ProductComposition>();
        if (processedProduct.getProductCompositionList().size() == 0) {
            return productCompositionList;
        }

        for (ProductComposition pc : processedProduct.getProductCompositionList()) {
            if (Boolean.TRUE.equals(pc.getActive())) {
                productCompositionList.add(pc);
            }
        }
        return productCompositionList;
    }

    public void removeOutputProductionVoucher(OutputProductionVoucher outputProductionVoucher) {
        OutputProductionVoucher outputForRemove = null;

        for (OutputProductionVoucher output : productionOrder.getOutputProductionVoucherList()) {
            if (output.getId().equals(outputProductionVoucher.getId())) {
                outputForRemove = output;
                break;
            }
        }

        if (outputForRemove != null) {
            productionOrder.getOutputProductionVoucherList().remove(outputForRemove);
        }
    }

    public static class Consolidated {
        private double amount;
        private MetaProduct product;

        public Consolidated(double amount, MetaProduct product) {
            this.amount = amount;
            this.product = product;
        }

        public Consolidated() {
        }

        public double getAmount() {
            return amount;
        }

        public void setAmount(double amount) {
            this.amount = amount;
        }

        public MetaProduct getProduct() {
            return product;
        }

        public void setProduct(MetaProduct product) {
            this.product = product;
        }
    }

    private static class Formulation {
        public ProductComposition productComposition;
        public Double producingAmount;
    }

    public static enum FormulaState {
        NONE, NEW, EDIT
    }

    public ProductionOrder getProductionOrder() {
        return productionOrder;
    }

    public void setProductionOrder(ProductionOrder productionOrder) {
        this.productionOrder = productionOrder;
    }
}
