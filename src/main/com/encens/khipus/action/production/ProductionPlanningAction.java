package com.encens.khipus.action.production;

import com.encens.khipus.exception.ConcurrencyException;
import com.encens.khipus.exception.EntryDuplicatedException;
import com.encens.khipus.exception.ReferentialIntegrityException;
import com.encens.khipus.exception.production.ProductCompositionException;
import com.encens.khipus.framework.action.GenericAction;
import com.encens.khipus.framework.action.Outcome;
import com.encens.khipus.framework.service.GenericService;
import com.encens.khipus.model.production.*;
import com.encens.khipus.model.warehouse.ProductItem;
import com.encens.khipus.model.warehouse.ProductItemPK;
import com.encens.khipus.service.production.*;
import com.encens.khipus.util.RoundUtil;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.*;
import org.jboss.seam.international.StatusMessage;

import javax.faces.event.ActionEvent;
import java.io.IOException;
import java.math.BigDecimal;
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
    private ProductionOrder productionOrderMaterial;
    private ProductComposition productComposition;
    private ProductionOrder productionOrder;
    private Formulation existingFormulation;
    private OrderMaterial orderMaterial;
    private ProductItem productItem;
    private List<ProductItemPK> selectedProductItems = new ArrayList<ProductItemPK>();
    private List<OrderMaterial> orderMaterials = new ArrayList<OrderMaterial>();
    private List<ProductItem> productItems = new ArrayList<ProductItem>();

    private FormulaState formulaState = FormulaState.NONE;

    private Boolean dispobleBalance = true;
    private Boolean addMaterial = false;
    private Boolean showMaterialDetail = false;
    private Boolean showInputDetail = false;
    private Boolean showDetailOrder = false;

    private Double expendOld;
    private Double containerOld;

    @In
    private ProductionPlanningService productionPlanningService;
    @In
    private ProcessedProductService processedProductService;
    @In
    private EvaluatorMathematicalExpressionsService evaluatorMathematicalExpressionsService;
    @In
    private ProductionOrderCodeGenerator productionOrderCodeGenerator;
    @In
    private EmployeeTimeCardService employeeTimeCardService;
    @In
    private ArticleEstateService articleEstateService;
    @In
    private IndirectCostsService indirectCostsService;

    private ProductionOrder totalsMaterials;
    private ProductionPlanning producedAmountWithExpendAmoutn;
    private Double totalVolumProductionPlaning;

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

    @Factory(value = "productionOrderMaterialForPlanning", scope = ScopeType.STATELESS)
    public ProductionOrder initProductionOrderMaterial() {
        return productionOrderMaterial;
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
        productionOrder.setExpendAmount(0.0);
    }

    @Begin(ifOutcome = Outcome.SUCCESS, flushMode = FlushModeType.MANUAL)
    public String select(ProductionPlanning productionPlanning) {
        showDetailOrder = false;
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
                //evaluatorMathematicalExpressionsService.executeMathematicalFormulas(order);
                evaluatorMathematicalExpressionsService.excuteFormulate(order,order.getContainerWeight(),order.getExpendAmount());
                for (ProductionIngredient ingredient : order.getProductComposition().getProductionIngredientList()) {
                    Consolidated aux = consolidated.get(ingredient.getMetaProduct().getId());
                    if (aux == null) {
                        aux = new Consolidated();
                        aux.setProduct(ingredient.getMetaProduct());
                        aux.setIdMeta(ingredient.getMetaProduct().getId());
                        aux.setName(ingredient.getMetaProduct().getName());
                        aux.setCode(ingredient.getMetaProduct().getCode());
                        aux.setUnit(ingredient.getMetaProduct().getProductItem().getUsageMeasureCode());
                        aux.setAmountWarehouse(productionPlanningService.getMountInWarehouse(ingredient.getMetaProduct()));
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
            if (ingredient.getMetaProduct().getProductItem().getUsageMeasureCode().equals("GR"))
                total = total + ingredient.getAmount() / 1000.0;
            else
                total = total + ingredient.getAmount();

        }
        return total;
    }

    public void productCompositionSelected(ActionEvent e) {
        try {
            //productionOrder.setExpendAmount(productComposition.getProducingAmount());
            productionOrder.setExpendAmount(productComposition.getSupposedAmount());
            productionOrder.setContainerWeight(productComposition.getContainerWeight());
            //productionOrder.setProducedAmount(productComposition.getSupposedAmount());
            //productionOrder.setProducedAmount(productionOrder.getExpendAmount());
            productionOrder.setProductComposition(productComposition);
            evaluatorMathematicalExpressionsService.excuteFormulate(productionOrder,productComposition.getContainerWeight(),productionOrder.getExpendAmount());
            setInputs(productionOrder.getProductComposition().getProductionIngredientList());
            dispobleBalance = true;

        } catch (Exception ex) {
            log.error("Exception caught", ex);
            facesMessages.addFromResourceBundle(ERROR, "Common.globalError.description");
        }
    }

    public Boolean verifAmount(ProductionIngredient ingredient){
        Boolean band= true;
        if(!articleEstateService.existArticleEstate(ingredient.getMetaProduct().getProductItem()))
        if(ingredient.getMountWareHouse().doubleValue() < ingredient.getAmount())
        {
            band = false;
            dispobleBalance = false;
        }

        return band;
    }

    public Boolean verifAmountInput(OrderInput orderInput){
        Boolean band= true;
        if(!articleEstateService.existArticleEstate(orderInput.getProductItem()))
            if(orderInput.getAmountStock().doubleValue() < orderInput.getAmount())
            {
                band = false;
                dispobleBalance = false;
            }

        return band;
    }

    public Boolean isParameterized(ProductItem productItem){
        return articleEstateService.verifyEstate(productItem,"PARAMETRIZABLE");
    }

    public Boolean isNotCountAs(ProductItem productItem)
    {
        return  articleEstateService.verifyEstate(productItem,"NOCONTABILLIZABLE");
    }

    public void addFormulation() {

        ProductionPlanning productionPlanning = getInstance();


        productionPlanning.getProductionOrderList().add(productionOrder);
        productionOrder.setProductionPlanning(productionPlanning);
        //productionOrder.setProducedAmount(productionOrder.getExpendAmount());
        if(productionOrder.getOrderInputs().size() == 0)
        setInputs(productionOrder.getProductComposition().getProductionIngredientList());

        if(productionPlanning.getId() != null && !verifySotck(productionOrder))
        if (update() != Outcome.SUCCESS) {
            return;
        }

        clearFormulation();
        disableEditingFormula();
    }

    public void updateFormulation() {
        if (evaluateMathematicalExpression() == false) {
            return;
        }
        ProductionPlanning planning = getInstance();
        //es necesario fijar el valor de cantidad producida al mismo valor que cantidad desada
        //para que no afecte en el calculo de las formulas
        //setProducedAmountWithExpendAmount(planning);

        //if (planning.getId() != null && verifySotckByProductionPlannig(planning))
        setInputs(productionOrder.getProductComposition().getProductionIngredientList());
        if (planning.getId() != null && !verifySotck(productionOrder))
            if (update() != Outcome.SUCCESS) {
                return;
            }
        //setPriceCostInput();
        existingFormulation = null;
        disableEditingFormula();
    }

    private Boolean verifySotck(ProductionOrder order)
    { Boolean band = false;
        for (ProductionIngredient ingredient : order.getProductComposition().getProductionIngredientList()) {
            BigDecimal mountWareHouse = productionPlanningService.getMountInWarehouse(ingredient.getMetaProduct().getProductItem());
            if(!articleEstateService.existArticleEstate(ingredient.getMetaProduct().getProductItem()))
            if (ingredient.getAmount() > mountWareHouse.doubleValue()) {
                addMessageError(ingredient.getMetaProduct().getProductItem().getName(), mountWareHouse.doubleValue());
                band = true;
            }
        }
        return band;
    }

    @End
    public String create(List<Consolidated> consolidateds) {
        Boolean band = true;
        try {
            for (Consolidated consolidated : consolidateds) {
                BigDecimal mountWareHouse = productionPlanningService.getMountInWarehouse(consolidated.getProduct());
                if(!articleEstateService.existArticleEstate(consolidated.getProduct().getProductItem()))  //si lo encuentra en la lista no lo toma encuenta
                if (consolidated.getAmount() > mountWareHouse.doubleValue()) {
                    //addMessageError(consolidated, mountWareHouse.doubleValue());
                    band = false;
                }
            }

            if (band) {
                ProductionPlanning productionPlanning = getInstance();
                //setZeroProducedAmount(productionPlanning);
                getService().create(productionPlanning);
                addCreatedMessage();
                return Outcome.SUCCESS;
            } else {
                return Outcome.REDISPLAY;
            }

        } catch (EntryDuplicatedException e) {
            addDuplicatedMessage();
            return Outcome.REDISPLAY;
        }
    }

    @End
    public String createNewPlannig() {
        Boolean band = true;
        try {
            for (OrderInput input: productionOrder.getOrderInputs()) {
                BigDecimal mountWareHouse = productionPlanningService.getMountInWarehouse(input.getProductItem());
                if(!articleEstateService.existArticleEstate(input.getProductItem()))  //si lo encuentra en la lista no lo toma encuenta
                    if (input.getAmount() > mountWareHouse.doubleValue()) {
                        addMessageError(input, mountWareHouse.doubleValue());
                        band = false;
                    }
            }

            if (band) {
                ProductionPlanning productionPlanning = getInstance();
                getService().create(productionPlanning);
                addCreatedMessage();
                return Outcome.SUCCESS;
            } else {
                return Outcome.REDISPLAY;
            }

        } catch (EntryDuplicatedException e) {
            addDuplicatedMessage();
            return Outcome.REDISPLAY;
        }
    }

    /*private void setZeroProducedAmount(ProductionPlanning planning)
    {
        for(ProductionOrder productionOrder:planning.getProductionOrderList() )
        {
            productionOrder.setProducedAmount(0.0);
        }
    }*/

    public void addMessageError(OrderInput input, Double mount) {
        facesMessages.addFromResourceBundle(StatusMessage.Severity.WARN, "Common.message.errorMountWarehouse", input.getProductItem().getName(), mount);
    }

    public void addMessageError(String name , Double mount) {
        facesMessages.addFromResourceBundle(StatusMessage.Severity.WARN, "Common.message.errorMountWarehouse", name, mount);
    }

    public void evaluateExpressionActionListener(ActionEvent e) {
        evaluateMathematicalExpression();
    }

    //public void evaluateParameterizedExpressionActionListener(ActionEvent e,OrderInput input) {
    public void evaluateParameterizedExpressionActionListener(OrderInput input) {
        try {

            if(expendOld == null)
            expendOld = productionOrder.getExpendAmount();
            if(containerOld == null)
            containerOld = productionOrder.getContainerWeight();

            Double container = evaluatorMathematicalExpressionsService.excuteParemeterized(input,productionOrder, productionOrder.getProductComposition().getContainerWeight(), productionOrder.getProductComposition().getSupposedAmount());
            //productionOrder.getProductComposition().setContainerWeight(container);
            productionOrder.setContainerWeight(container);
            //productionOrder.setExpendAmount(evaluatorMathematicalExpressionsService.getAmountExpected(expendOld,containerOld,container));
            productionOrder.setExpendAmount(evaluatorMathematicalExpressionsService.getAmountExpected(productionOrder.getProductComposition().getSupposedAmount(),productionOrder.getProductComposition().getContainerWeight(),container));

        } catch (ProductCompositionException e1) {
            e1.printStackTrace();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        evaluateParameterizedExpression(input);
    }

    private boolean evaluateParameterizedExpression(OrderInput input) {
        try {
            evaluatorMathematicalExpressionsService.excuteParemeterizadFormulate(productionOrder, productionOrder.getProductComposition().getContainerWeight(), productionOrder.getProductComposition().getSupposedAmount());
            setInputsParametrized(productionOrder.getProductComposition().getProductionIngredientList(),input);
            return true;
        } catch (Exception ex) {
            log.error("Exception caught", ex);
            facesMessages.addFromResourceBundle(ERROR, "Common.globalError.description");
            return false;
        }
    }

    private boolean evaluateMathematicalExpression() {
        try {
            if(containerOld != null)
            {
                productionOrder.setContainerWeight(containerOld);
              //  productionOrder.getProductComposition().setContainerWeight(containerOld);
            }

            evaluatorMathematicalExpressionsService.excuteFormulate(productionOrder,productionOrder.getProductComposition().getContainerWeight(),productionOrder.getProductComposition().getSupposedAmount());
            setInputs(productionOrder.getProductComposition().getProductionIngredientList());
            return true;
        } catch (Exception ex) {
            log.error("Exception caught", ex);
            facesMessages.addFromResourceBundle(ERROR, "Common.globalError.description");
            return false;
        }
    }

    private void setInputs(List<ProductionIngredient> productionIngredientList) {

        productionOrder.getOrderInputs().clear();
        for(ProductionIngredient ingredient :productionOrder.getProductComposition().getProductionIngredientList())
        {
            OrderInput input = new OrderInput();
            input.setProductItem(ingredient.getMetaProduct().getProductItem());
            input.setProductionOrder(productionOrder);
            input.setAmount(ingredient.getAmount());
            input.setAmountStock(ingredient.getMountWareHouse());
            input.setProductItemCode(ingredient.getMetaProduct().getProductItemCode());
            input.setCompanyNumber(ingredient.getMetaProduct().getCompanyNumber());
            input.setMathematicalFormula(ingredient.getMathematicalFormula());
            BigDecimal costUnit = ingredient.getMetaProduct().getProductItem().getUnitCost();
            input.setCostUnit(costUnit);
            input.setCostTotal(new BigDecimal(ingredient.getAmount() * costUnit.doubleValue()));
            productionOrder.getOrderInputs().add(input);
        }
    }

    private void setInputsParametrized(List<ProductionIngredient> productionIngredientList, OrderInput inputParameterize)
    {
        productionOrder.getOrderInputs().clear();
        for(ProductionIngredient ingredient :productionOrder.getProductComposition().getProductionIngredientList())
        {
            if(inputParameterize.getProductItem() != ingredient.getMetaProduct().getProductItem())
            {
                OrderInput input = new OrderInput();
                input.setProductItem(ingredient.getMetaProduct().getProductItem());
                input.setProductionOrder(productionOrder);
                input.setAmount(ingredient.getAmount());
                input.setAmountStock(ingredient.getMountWareHouse());
                input.setProductItemCode(ingredient.getMetaProduct().getProductItemCode());
                input.setCompanyNumber(ingredient.getMetaProduct().getCompanyNumber());
                input.setMathematicalFormula(ingredient.getMathematicalFormula());
                BigDecimal costUnit = ingredient.getMetaProduct().getProductItem().getUnitCost();
                input.setCostUnit(costUnit);
                input.setCostTotal(new BigDecimal(ingredient.getAmount() * costUnit.doubleValue()));
                productionOrder.getOrderInputs().add(input);
            }else{
                inputParameterize.setCostTotal(new BigDecimal(inputParameterize.getCostUnit().doubleValue()*inputParameterize.getAmount()));
                productionOrder.getOrderInputs().add(inputParameterize);
            }
        }
    }

    @End
    public String removeFormulation() {
        ProductionPlanning productionPlanning = getInstance();
        //setPriceCostInput();
        String result = Outcome.FAIL;
        for (ProductionOrder po : productionPlanning.getProductionOrderList()) {
            if (po.getCode().equals(productionOrder.getCode())) {
                productionPlanning.getProductionOrderList().remove(po);
                result = deleteOrder(po);
                clearFormulation();
                break;
            }
        }
        disableEditingFormula();
        return result;
    }

    public String deleteOrder(ProductionOrder order) {
        try {
            getService().delete(order);
            addDeletedMessage();
        } catch (ConcurrencyException e) {
            entryNotFoundLog();
            addDeleteConcurrencyMessage();
        } catch (ReferentialIntegrityException e) {
            referentialIntegrityLog();
            addDeleteReferentialIntegrityMessage();
        }

        return Outcome.SUCCESS;
    }


    public void selectProcessedProduct(ProcessedProduct processedProduct) {
        dispobleBalance = true;
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
            //outputProductionVoucher.setProducedAmount(0.0);
            outputProductionVoucher.setProductionOrder(productionOrder);
            productionOrder.getOutputProductionVoucherList().add(outputProductionVoucher);
        } catch (Exception ex) {
            log.error(ex);
            facesMessages.addFromResourceBundle(ERROR, "Common.globalError.description");
        }
    }

    public void selectModifiIput(ProductionOrder productionOrder) {

        cancelFormulation();
        //dispobleBalance = false;
        existingFormulation = new Formulation();
        existingFormulation.producingAmount = productionOrder.getExpendAmount();
        existingFormulation.productComposition = productionOrder.getProductComposition();

        this.productionOrder = productionOrder;
        this.productComposition = productionOrder.getProductComposition();
        this.processedProduct = productComposition.getProcessedProduct();

        evaluateMathematicalExpression();
        formulaState = FormulaState.EDIT;

    }

    public void selectMaterial(ProductionOrder order) {

        //cancelFormulation();
        disableEditingFormula();
        showDetailOrder = false;
        productionOrder = order;
        this.productionOrderMaterial = productionOrder;
        this.productComposition = productionOrder.getProductComposition();
        this.processedProduct = productComposition.getProcessedProduct();
        /*
        this.productComposition = productionOrder.getProductComposition();
        this.processedProduct = productComposition.getProcessedProduct();

        if(orderMaterials.size() == 0)
        orderMaterials = order.getOrderMaterials();*/

        orderMaterials = new ArrayList<OrderMaterial>();
        orderMaterials.addAll(order.getOrderMaterials());

        addMaterial = true;
    }

    public void selectMaterialDetail(ProductionOrder order) {
        disableEditingFormula();
        productionOrderMaterial = order;
        productionOrder = order;
        orderMaterials = new ArrayList<OrderMaterial>();
        orderMaterials.addAll(order.getOrderMaterials());

        showMaterialDetail = true;
    }

    public void selectDetail(ProductionOrder order) {
        cancelFormulation();
        addMaterial = false;
        //productionOrderMaterial = order;
        productionOrder = order;
        if(productionOrder.getId() != null){
        setTotalsMaterials(productionOrder);
        setTotalsInputs(productionOrder);
        setTotalHour(productionOrder);
        setTotalIndiRectCost(productionOrder);
        setTotalCostProducticionAndUnitPrice(productionOrder);
        }
        //orderMaterials = new ArrayList<OrderMaterial>();
        //orderMaterials.addAll(order.getOrderMaterials());

        showDetailOrder = true;
    }

    @End(ifOutcome = Outcome.SUCCESS)
    public String makeExecutedOrder() {
        //getInstance().setState(EXECUTED);
        productionOrder.setEstateOrder(EXECUTED);
        ProductionPlanning productionPlanning = getInstance();
        for (ProductionOrder productionOrder : productionPlanning.getProductionOrderList()) {
            setTotalsMaterials(productionOrder);
            setTotalsInputs(productionOrder);
            setTotalIndiRectCost(productionOrder);
            setTotalHour(productionOrder);
            setTotalCostProducticionAndUnitPrice(productionOrder);
        }
        String outcome = update();

        if (outcome != Outcome.SUCCESS) {
            productionOrder.setEstateOrder(PENDING);
            //getInstance().setState(PENDING);
        }
        return outcome;
    }

    @End(ifOutcome = Outcome.SUCCESS)
    public String makeFinalizedOrder() {
        //getInstance().setState(FINALIZED);
        productionOrder.setEstateOrder(FINALIZED);
        ProductionPlanning productionPlanning = getInstance();
        for (ProductionOrder productionOrder : productionPlanning.getProductionOrderList()) {
            setTotalsMaterials(productionOrder);
            setTotalsInputs(productionOrder);
            setTotalIndiRectCost(productionOrder);
            setTotalHour(productionOrder);
            setTotalCostProducticionAndUnitPrice(productionOrder);
        }
        String outcome = update();

        if (outcome != Outcome.SUCCESS) {
            productionOrder.setEstateOrder(EXECUTED);
            //getInstance().setState(EXECUTED);
        }
        return outcome;
    }

    public void closeDetail() {
        disableEditingFormula();
        //productionOrder = null;
        showDetailOrder = false;
    }

    public void addProductItems(List<ProductItem> productItems) {

        if (selectedProductItems.size() == 0 && orderMaterials.size() > 0)
            for (OrderMaterial material : orderMaterials) {
                selectedProductItems.add(material.getProductItem().getId());
            }

        for (ProductItem productItem : productItems) {
            if (selectedProductItems.contains(productItem.getId())) {
                continue;
            }

            selectedProductItems.add(productItem.getId());

            OrderMaterial material = new OrderMaterial();
            material.setProductItem(productItem);
            material.setProductionOrder(productionOrder);
            material.setCompanyNumber(productItem.getCompanyNumber());
            orderMaterials.add(material);
        }
    }

    public void setProductItems(List<ProductItem> productItems) {
        this.productItems = productItems;
    }

    public void addOrderProduced() {
        for (OrderMaterial material : orderMaterials) {
            if (material.getAmountUsed() > 0) {
                Double amountReturn = material.getAmountRequired() - material.getAmountUsed();
                //el precio unitario sin redondear
                Double total = material.getAmountUsed() * ((BigDecimal) material.getProductItem().getUnitCost()).doubleValue();
                material.setAmountReturned(RoundUtil.getRoundValue(amountReturn,2, RoundUtil.RoundMode.SYMMETRIC));
                material.setCostUnit(material.getProductItem().getUnitCost());
                material.setCostTotal(new BigDecimal(total));

            }
        }
        ProductionPlanning productionPlanning = getInstance();
        int position = productionPlanning.getProductionOrderList().indexOf(productionOrder);
        productionPlanning.getProductionOrderList().get(position).getOrderMaterials().clear();
        productionPlanning.getProductionOrderList().get(position).setOrderMaterials(orderMaterials);



        addMaterial = false;
        if(productionPlanning.getId() != null)
        {
            setTotalsMaterials(productionOrder);
            setTotalsInputs(productionOrder);
            setTotalIndiRectCost(productionOrder);
            setTotalHour(productionOrder);
            setTotalCostProducticionAndUnitPrice(productionOrder);

        if (update() != Outcome.SUCCESS) {
            return;
        }

        }
        orderMaterials.clear();
        selectedProductItems.clear();
    }

    public void removeMaterial(OrderMaterial instance) {
        selectedProductItems.remove(instance.getProductItem().getId());
        orderMaterials.remove(instance);
    }

    public void clearFormulation() {
        processedProduct = new ProcessedProduct();
        productComposition = new ProductComposition();
        productionOrder = new ProductionOrder();
    }



    private Boolean verifySotckByProductionPlannig(ProductionPlanning planning){
        Boolean band = true;
            for(ProductionOrder order: planning.getProductionOrderList())
            {
                band = verifySotck(order);
            }
        return band;
    }

    public void updateProducedAmount() {
        /*Double totalProducer = 0.0;
        for(OutputProductionVoucher outputProductionVoucher:productionOrder.getOutputProductionVoucherList())
        {
            totalProducer += outputProductionVoucher.getProducedAmount();
        }
        productionOrder.setExpendAmount(totalProducer);*/
        setTotalsMaterials(productionOrder);
        setTotalsInputs(productionOrder);
        setTotalIndiRectCost(productionOrder);
        setTotalHour(productionOrder);
        setTotalCostProducticionAndUnitPrice(productionOrder);
        if (update() != Outcome.SUCCESS) {
            return;
        }

        existingFormulation = null;
        disableEditingFormula();
    }

    public void cancelFormulation() {
        if (existingFormulation != null) {
            productionOrder.setProductComposition(existingFormulation.productComposition);
            productionOrder.setExpendAmount(existingFormulation.producingAmount);
            existingFormulation = null;
        }
        disableEditingFormula();

        dispobleBalance = true;
        addMaterial = false;
        showInputDetail = false;
        showMaterialDetail = false;
        showDetailOrder = false;
    }

    private void disableEditingFormula() {
        formulaState = FormulaState.NONE;
    }

    @End(ifOutcome = Outcome.SUCCESS)
    public String makeExecuted() {
        //getInstance().setState(EXECUTED);
        productionOrder.setEstateOrder(EXECUTED);
        ProductionPlanning productionPlanning = getInstance();
        for (ProductionOrder productionOrder : productionPlanning.getProductionOrderList()) {
            setTotalsMaterials(productionOrder);
            setTotalsInputs(productionOrder);
            setTotalHour(productionOrder);
            setTotalIndiRectCost(productionOrder);
            setTotalCostProducticionAndUnitPrice(productionOrder);
        }
        String outcome = update();

        if (outcome != Outcome.SUCCESS) {
            productionOrder.setEstateOrder(PENDING);
            //getInstance().setState(PENDING);
        }
        return outcome;
    }

    @End(ifOutcome = Outcome.SUCCESS)
    public String makeFinalized() {
        //getInstance().setState(FINALIZED);
        productionOrder.setEstateOrder(FINALIZED);
        ProductionPlanning productionPlanning = getInstance();
        for (ProductionOrder productionOrder : productionPlanning.getProductionOrderList()) {
            setTotalsMaterials(productionOrder);
            setTotalsInputs(productionOrder);
            setTotalHour(productionOrder);
            setTotalIndiRectCost(productionOrder);
            setTotalCostProducticionAndUnitPrice(productionOrder);
        }
        String outcome = update();

        if (outcome != Outcome.SUCCESS) {
            productionOrder.setEstateOrder(EXECUTED);
            //getInstance().setState(EXECUTED);
        }
        return outcome;
    }

    public void setTotalHour(ProductionOrder productionOrder) {
        productionOrder.setTotalPriceJourney(((BigDecimal) (employeeTimeCardService.getCostProductionOrder(productionOrder, getInstance().getDate(), getTotalVolumProductionPlaning(productionOrder)))).doubleValue());
    }

    public void setTotalCostProducticionAndUnitPrice(ProductionOrder productionOrder) {
        Double total = productionOrder.getTotalPriceMaterial() + productionOrder.getTotalPriceInput() + productionOrder.getTotalPriceJourney() + productionOrder.getTotalIndirectCosts();
        productionOrder.setTotalCostProduction(total);
        Double priceUnit = 0.0;
        if (productionOrder.getProducedAmount() > 0.0)
            priceUnit = total / productionOrder.getProducedAmount();

        productionOrder.getProductComposition().getProcessedProduct().getProductItem().setUnitCost(new BigDecimal(priceUnit));
    }

    public void setTotalIndiRectCost(ProductionOrder productionOrder)
    {
      productionOrder.setTotalIndirectCosts(indirectCostsService.getCostTotalIndirect(productionOrder,getTotalVolumProductionPlaning(productionOrder),getTotalVolumGeneralProductionPlaning(productionOrder)));
    }
    public void setTotalsInputs(ProductionOrder productionOrder) {
        Double totalInput = 0.0;

        for (OrderInput input : productionOrder.getOrderInputs()) {
            //totalInput += RoundUtil.getRoundValue((input.getProductItem().getUnitCost().doubleValue()) * input.getAmount(),2, RoundUtil.RoundMode.SYMMETRIC);
            if(!isNotCountAs(input.getProductItem()))
            totalInput += (input.getProductItem().getUnitCost().doubleValue()) * input.getAmount();
        }

        productionOrder.setTotalPriceInput(RoundUtil.getRoundValue(totalInput,2, RoundUtil.RoundMode.SYMMETRIC));
    }

    public void setTotalsMaterials(ProductionOrder productionOrder) {
        Double totalMaterial = 0.0;
        for (OrderMaterial material : productionOrder.getOrderMaterials()) {
            totalMaterial += material.getCostTotal().doubleValue();
        }
        productionOrder.setTotalPriceMaterial(RoundUtil.getRoundValue(totalMaterial,2, RoundUtil.RoundMode.SYMMETRIC));
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

    public Double getTotalVolumGeneralProductionPlaning(ProductionOrder productionOrder) {
        Double total = 0.0;
        for(ProductionOrder order: getInstance().getProductionOrderList())
        {
                total += employeeTimeCardService.getTotalVolumeOrder(order);
        }
        return total;
    }

    public Double getTotalVolumProductionPlaning(ProductionOrder productionOrder) {
        Double total = 0.0;
        for(ProductionOrder order: getInstance().getProductionOrderList())
        {
            if(productionOrder.getProductComposition().getProcessedProduct().getProductItem().getSubGroup().getGroup() == order.getProductComposition().getProcessedProduct().getProductItem().getSubGroup().getGroup())
               total += employeeTimeCardService.getTotalVolumeOrder(order);
        }
        return total;
    }

    public static class Consolidated {
        private double amount;
        private MetaProduct product;
        private Long idMeta;
        private String name;
        private String code;
        private String unit;
        private BigDecimal amountWarehouse;
        private Boolean isVerifiably;

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

        public Long getIdMeta() {
            return idMeta;
        }

        public void setIdMeta(Long idMeta) {
            this.idMeta = idMeta;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getUnit() {
            return unit;
        }

        public void setUnit(String unit) {
            this.unit = unit;
        }

        public BigDecimal getAmountWarehouse() {
            return amountWarehouse;
        }

        public void setAmountWarehouse(BigDecimal amountWarehouse) {
            this.amountWarehouse = amountWarehouse;
        }

        public Boolean getVerifiably() {
            return isVerifiably;
        }

        public void setVerifiably(Boolean verifiably) {
            isVerifiably = verifiably;
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

    public Boolean getDispobleBalance() {
        return dispobleBalance;
    }

    public void setDispobleBalance(Boolean dispobleBalance) {
        this.dispobleBalance = dispobleBalance;
    }

    public Boolean getAddMaterial() {
        return addMaterial;
    }

    public void setAddMaterial(Boolean addMaterial) {
        this.addMaterial = addMaterial;
    }

    public void cancelMaterial() {
        selectedProductItems.clear();
        orderMaterials.clear();
        //productionOrder = null;
        addMaterial = false;
    }

    public OrderMaterial getOrderMaterial() {
        return orderMaterial;
    }

    public void setOrderMaterial(OrderMaterial orderMaterial) {
        this.orderMaterial = orderMaterial;
    }

    public ProductItem getProductItem() {
        return productItem;
    }

    public void setProductItem(ProductItem productItem) {
        this.productItem = productItem;
    }

    public List<OrderMaterial> getOrderMaterials() {
        return orderMaterials;
    }

    public void setOrderMaterials(List<OrderMaterial> orderMaterials) {
        this.orderMaterials = orderMaterials;
    }

    public Boolean getShowMaterialDetail() {
        return showMaterialDetail;
    }

    public void setShowMaterialDetail(Boolean showMaterialDetail) {
        this.showMaterialDetail = showMaterialDetail;
    }

    public Boolean getShowDetailOrder() {
        return showDetailOrder;
    }

    public void setShowDetailOrder(Boolean showDetailOrder) {
        this.showDetailOrder = showDetailOrder;
    }
}
