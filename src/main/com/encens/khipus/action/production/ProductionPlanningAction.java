package com.encens.khipus.action.production;

import com.encens.khipus.action.SessionUser;
import com.encens.khipus.exception.ConcurrencyException;
import com.encens.khipus.exception.EntryDuplicatedException;
import com.encens.khipus.exception.EntryNotFoundException;
import com.encens.khipus.exception.ReferentialIntegrityException;
import com.encens.khipus.exception.finances.CompanyConfigurationNotFoundException;
import com.encens.khipus.exception.finances.FinancesCurrencyNotFoundException;
import com.encens.khipus.exception.finances.FinancesExchangeRateNotFoundException;
import com.encens.khipus.exception.production.ProductCompositionException;
import com.encens.khipus.exception.warehouse.*;
import com.encens.khipus.framework.action.GenericAction;
import com.encens.khipus.framework.action.Outcome;
import com.encens.khipus.framework.service.GenericService;
import com.encens.khipus.model.academics.ExecutorUnit;
import com.encens.khipus.model.admin.BusinessUnit;
import com.encens.khipus.model.admin.User;
import com.encens.khipus.model.finances.CashAccount;
import com.encens.khipus.model.production.*;
import com.encens.khipus.model.warehouse.*;
import com.encens.khipus.service.admin.BusinessUnitService;
import com.encens.khipus.service.employees.JobContractService;
import com.encens.khipus.service.finances.CostCenterService;
import com.encens.khipus.service.production.*;
import com.encens.khipus.service.warehouse.ApprovalWarehouseVoucherService;
import com.encens.khipus.service.warehouse.InventoryService;
import com.encens.khipus.service.warehouse.MovementDetailService;
import com.encens.khipus.service.warehouse.WarehouseService;
import com.encens.khipus.util.*;
import com.encens.khipus.util.query.QueryUtils;
import com.encens.khipus.util.warehouse.InventoryMessage;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.*;
import org.jboss.seam.international.StatusMessage;
import org.jboss.ws.metadata.wsdl.WSDLBindingOperationOutput;

import javax.faces.event.ActionEvent;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

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

    // this map stores the MovementDetails that are under the minimal stock and the unitaryBalance of the Inventory
    protected Map<MovementDetail, BigDecimal> movementDetailUnderMinimalStockMap = new HashMap<MovementDetail, BigDecimal>();
    // this map stores the MovementDetails that are over the maximum stock and the unitaryBalance of the Inventory
    protected Map<MovementDetail, BigDecimal> movementDetailOverMaximumStockMap = new HashMap<MovementDetail, BigDecimal>();
    // this list stores the MovementDetails that should not show warnings
    protected List<MovementDetail> movementDetailWithoutWarnings = new ArrayList<MovementDetail>();

    private FormulaState formulaState = FormulaState.NONE;

    private Boolean dispobleBalance = true;
    private Boolean addMaterial = false;
    private Boolean showMaterialDetail = false;
    private Boolean showInputDetail = false;
    private Boolean showDetailOrder = false;
    private Boolean showProductionOrders = true;

    private Double expendOld;
    private Double containerOld;
    private Integer codeOrder = 0;

    private List<MovementDetail> movementDetailSelect;

    private List<AccountOrderProduction> accountOrderProductions;

    private WarehouseVoucher warehouseVoucherSelect;

    private static final Integer SCALE = 6;

    @In
    private SessionUser sessionUser;
    @In
    User currentUser;
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
    @In
    protected WarehouseService warehouseService;
    @In
    protected CostCenterService costCenterService;
    @In
    protected BusinessUnitService businessUnitService;
    @In
    protected InventoryService inventoryService;
    @In
    protected JobContractService jobContractService;
    @In
    private ApprovalWarehouseVoucherService approvalWarehouseVoucherService;
    @In
    private MovementDetailService movementDetailService;

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

        if(getInstance().getId() == null && getInstance().getProductionOrderList().size() > 0 )
        {
            for (ProductionOrder order :getInstance().getProductionOrderList())
            {
                if(getCodeOrder(order.getCode()) > codeOrder)
                codeOrder = getCodeOrder(order.getCode());
            }
            String[] arr=productionOrderCodeGenerator.generateCode().split("\\-");
            String code = arr[0]+"-"+String.format("%04d", codeOrder+1);
            productionOrder.setCode(code);
        }
        else
        {
            codeOrder = getCodeOrder(productionOrderCodeGenerator.generateCode());
            productionOrder.setCode(productionOrderCodeGenerator.generateCode());
        }
        productionOrder.setExpendAmount(0.0);
    }

    private Integer getCodeOrder(String code)
    {
        String[] arr=code.split("\\-");
        Integer val = Integer.parseInt(arr[1]);
        return val;
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
                evaluatorMathematicalExpressionsService.excuteFormulate(order, order.getContainerWeight(), order.getExpendAmount());
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
            evaluatorMathematicalExpressionsService.excuteFormulate(productionOrder, productComposition.getContainerWeight(), productionOrder.getExpendAmount());
            setInputs(productionOrder.getProductComposition().getProductionIngredientList());
            dispobleBalance = true;

        } catch (Exception ex) {
            log.error("Exception caught", ex);
            facesMessages.addFromResourceBundle(ERROR, "Common.globalError.description");
        }
    }

    public Boolean verifAmount(ProductionIngredient ingredient) {
        Boolean band = true;
        if (!articleEstateService.existArticleEstate(ingredient.getMetaProduct().getProductItem()))
            if (ingredient.getMountWareHouse().doubleValue() < ingredient.getAmount()) {
                band = false;
                dispobleBalance = false;
            }

        return band;
    }

    public Boolean verifAmountInput(OrderInput orderInput) {
        Boolean band = true;
        if (!articleEstateService.existArticleEstate(orderInput.getProductItem()))
            if (orderInput.getAmountStock().doubleValue() < orderInput.getAmount()) {
                band = false;
                dispobleBalance = false;
            }

        return band;
    }

    public Boolean isParameterized(ProductItem productItem) {
        return articleEstateService.verifyEstate(productItem, "PARAMETRIZABLE");
    }

    public Boolean isNotCountAs(ProductItem productItem) {
        return articleEstateService.verifyEstate(productItem, "NOCONTABILLIZABLE");
    }

    public void addFormulation() {

        ProductionPlanning productionPlanning = getInstance();


        productionPlanning.getProductionOrderList().add(productionOrder);
        productionOrder.setProductionPlanning(productionPlanning);
        //productionOrder.setProducedAmount(productionOrder.getExpendAmount());
        if (productionOrder.getOrderInputs().size() == 0)
            setInputs(productionOrder.getProductComposition().getProductionIngredientList());

        if (productionPlanning.getId() != null && !verifySotck(productionOrder))
            if (update() != Outcome.SUCCESS) {
                return;
            }

        clearFormulation();
        disableEditingFormula();
        showProductionOrders = true;
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
        showProductionOrders = true;
    }

    private Boolean verifySotck(ProductionOrder order) {
        Boolean band = false;
        for (ProductionIngredient ingredient : order.getProductComposition().getProductionIngredientList()) {
            BigDecimal mountWareHouse = productionPlanningService.getMountInWarehouse(ingredient.getMetaProduct().getProductItem());
            if (!articleEstateService.existArticleEstate(ingredient.getMetaProduct().getProductItem()))
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
                if (!articleEstateService.existArticleEstate(consolidated.getProduct().getProductItem()))  //si lo encuentra en la lista no lo toma encuenta
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
            for (OrderInput input : productionOrder.getOrderInputs()) {
                BigDecimal mountWareHouse = productionPlanningService.getMountInWarehouse(input.getProductItem());
                if (!articleEstateService.existArticleEstate(input.getProductItem()))  //si lo encuentra en la lista no lo toma encuenta
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
    public void addMessageCreateWarehouseVoucherInput()
    {
        facesMessages.addFromResourceBundle(StatusMessage.Severity.INFO, "WarehouseVoucher.generateVoucher");
    }

    public void addMessageVerifyMaterial()
    {
        facesMessages.addFromResourceBundle(StatusMessage.Severity.INFO, "ProductionPlanning.material.size");
    }

    public void addMessageError(OrderInput input, Double mount) {
        facesMessages.addFromResourceBundle(StatusMessage.Severity.WARN, "Common.message.errorMountWarehouse", input.getProductItem().getName(), mount);
    }

    public void addMessageError(String name, Double mount) {
        facesMessages.addFromResourceBundle(StatusMessage.Severity.WARN, "Common.message.errorMountWarehouse", name, mount);
    }

    public void evaluateExpressionActionListener(ActionEvent e) {
        evaluateMathematicalExpression();
    }

    //public void evaluateParameterizedExpressionActionListener(ActionEvent e,OrderInput input) {
    public void evaluateParameterizedExpressionActionListener(OrderInput input) {
        try {

            if (expendOld == null)
                expendOld = productionOrder.getExpendAmount();
            if (containerOld == null)
                containerOld = productionOrder.getContainerWeight();

            Double container = evaluatorMathematicalExpressionsService.excuteParemeterized(input, productionOrder, productionOrder.getProductComposition().getContainerWeight(), productionOrder.getProductComposition().getSupposedAmount());
            //productionOrder.getProductComposition().setContainerWeight(container);
            productionOrder.setContainerWeight(container);
            //productionOrder.setExpendAmount(evaluatorMathematicalExpressionsService.getAmountExpected(expendOld,containerOld,container));
            productionOrder.setExpendAmount(evaluatorMathematicalExpressionsService.getAmountExpected(productionOrder.getProductComposition().getSupposedAmount(), productionOrder.getProductComposition().getContainerWeight(), container));

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
            setInputsParametrized(productionOrder.getProductComposition().getProductionIngredientList(), input);
            return true;
        } catch (Exception ex) {
            log.error("Exception caught", ex);
            facesMessages.addFromResourceBundle(ERROR, "Common.globalError.description");
            return false;
        }
    }

    private boolean evaluateMathematicalExpression() {
        try {
            if (containerOld != null) {
                productionOrder.setContainerWeight(containerOld);
                //  productionOrder.getProductComposition().setContainerWeight(containerOld);
            }

            evaluatorMathematicalExpressionsService.excuteFormulate(productionOrder, productionOrder.getProductComposition().getContainerWeight(), productionOrder.getProductComposition().getSupposedAmount());
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
        for (ProductionIngredient ingredient : productionOrder.getProductComposition().getProductionIngredientList()) {
            OrderInput input = new OrderInput();
            input.setProductItem(ingredient.getMetaProduct().getProductItem());
            input.setProductionOrder(productionOrder);
            input.setAmount(ingredient.getAmount());
            input.setAmountStock(ingredient.getMountWareHouse());
            input.setProductItemCode(ingredient.getMetaProduct().getProductItemCode());
            input.setCompanyNumber(ingredient.getMetaProduct().getCompanyNumber());
            input.setMathematicalFormula(ingredient.getMathematicalFormula());
            BigDecimal costUnit;

            if(articleEstateService.verifyEstate(ingredient.getMetaProduct().getProductItem(), Constants.ESTATE_ARTICLE_COMPOSITE))
                costUnit = getCostUnitProdComposite(ingredient);
            else
                costUnit = ingredient.getMetaProduct().getProductItem().getUnitCost();

            input.setCostUnit(costUnit);
            input.setCostTotal(new BigDecimal(ingredient.getAmount() * costUnit.doubleValue()));
            productionOrder.getOrderInputs().add(input);
        }
    }

    private void setInputsParametrized(List<ProductionIngredient> productionIngredientList, OrderInput inputParameterize) {
        productionOrder.getOrderInputs().clear();
        for (ProductionIngredient ingredient : productionOrder.getProductComposition().getProductionIngredientList()) {
            if (inputParameterize.getProductItem() != ingredient.getMetaProduct().getProductItem()) {
                OrderInput input = new OrderInput();
                input.setProductItem(ingredient.getMetaProduct().getProductItem());
                input.setProductionOrder(productionOrder);
                input.setAmount(ingredient.getAmount());
                input.setAmountStock(ingredient.getMountWareHouse());
                input.setProductItemCode(ingredient.getMetaProduct().getProductItemCode());
                input.setCompanyNumber(ingredient.getMetaProduct().getCompanyNumber());
                input.setMathematicalFormula(ingredient.getMathematicalFormula());
                BigDecimal costUnit;

                if(articleEstateService.verifyEstate(ingredient.getMetaProduct().getProductItem(), Constants.ESTATE_ARTICLE_COMPOSITE))
                costUnit = getCostUnitProdComposite(ingredient);
                else
                costUnit = ingredient.getMetaProduct().getProductItem().getUnitCost();

                input.setCostUnit(costUnit);
                input.setCostTotal(new BigDecimal(ingredient.getAmount() * costUnit.doubleValue()));
                productionOrder.getOrderInputs().add(input);
            } else {

                if(articleEstateService.verifyEstate(inputParameterize.getProductItem(), Constants.ESTATE_ARTICLE_COMPOSITE))
                    inputParameterize.setCostUnit(getCostUnitProdComposite(ingredient));

                inputParameterize.setCostTotal(new BigDecimal(inputParameterize.getCostUnit().doubleValue() * inputParameterize.getAmount()));
                productionOrder.getOrderInputs().add(inputParameterize);
            }
        }
    }

    private BigDecimal getCostUnitProdComposite(ProductionIngredient ingredient) {

        List<ProductionIngredient> aux = new ArrayList<ProductionIngredient>();

        ProductComposition composition = processedProductService.getProductComposite(ingredient.getMetaProduct().getId());
        aux = composition.getProductionIngredientList();
        try {
            evaluatorMathematicalExpressionsService.excuteFormulate(aux,ingredient.getAmount(),composition.getContainerWeight(),composition.getSupposedAmount());
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (ProductCompositionException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        Double totalcost = calculateCostTotal(aux);
        aux = null;
        return new BigDecimal(totalcost/ingredient.getAmount());
    }

    public WarehouseVoucher createWarehouseVoucherMaterial(){

        WarehouseVoucher warehouseVoucher =  new WarehouseVoucher();
        warehouseVoucher.setCompanyNumber(Constants.defaultCompanyNumber);
        warehouseVoucher.setDocumentCode("2");
        warehouseVoucher.setDate(new Date());
        warehouseVoucher.setState(WarehouseVoucherState.PEN);
        warehouseVoucher.setWarehouseCode("3");
        warehouseVoucher.setCostCenter(costCenterService.findCostCenterByCode("0111"));
        warehouseVoucher.setCostCenterCode("0111");
        warehouseVoucher.setWarehouse(warehouseService.findWarehouseByCode("3"));
        warehouseVoucher.setDocumentType(productionPlanningService.getDefaultDocumentType());
        warehouseVoucher.setResponsible(currentUser.getEmployee());
        warehouseVoucher.setExecutorUnit(businessUnitService.findBusinessUnitByExecutorUnitCode("01"));

        return warehouseVoucher;
    }

    public WarehouseVoucher createWarehouseVoucherInput(){
        /*Crear el vale*/
        WarehouseVoucher warehouseVoucher =  new WarehouseVoucher();
        warehouseVoucher.setCompanyNumber(Constants.defaultCompanyNumber);
        warehouseVoucher.setDocumentCode("2");
        warehouseVoucher.setDate(new Date());
        warehouseVoucher.setState(WarehouseVoucherState.PEN);
        warehouseVoucher.setWarehouseCode("1");
        warehouseVoucher.setCostCenter(costCenterService.findCostCenterByCode("0111"));
        warehouseVoucher.setCostCenterCode("0111");
        Warehouse warehouse = warehouseService.findWarehouseByCode("1");
        warehouseVoucher.setWarehouse(warehouse);
        warehouseVoucher.setDocumentType(productionPlanningService.getDefaultDocumentType());
        warehouseVoucher.setPetitionerJobContract(jobContractService.lastJobContractByEmployee(currentUser.getEmployee()));
        warehouseVoucher.setResponsible(warehouse.getResponsible());
        warehouseVoucher.setExecutorUnit(businessUnitService.findBusinessUnitByExecutorUnitCode("01"));

        return warehouseVoucher;
    }

    public WarehouseVoucher createWarehouseVoucherOrder(){

        WarehouseVoucher warehouseVoucher =  new WarehouseVoucher();
        warehouseVoucher.setCompanyNumber(Constants.defaultCompanyNumber);
        warehouseVoucher.setDocumentCode("1");
        warehouseVoucher.setDate(new Date());
        warehouseVoucher.setState(WarehouseVoucherState.PEN);
        warehouseVoucher.setWarehouseCode("2");
        warehouseVoucher.setCostCenter(costCenterService.findCostCenterByCode("0112"));
        warehouseVoucher.setCostCenterCode("0112");
        Warehouse warehouse = warehouseService.findWarehouseByCode("2");
        warehouseVoucher.setWarehouse(warehouse);
        warehouseVoucher.setDocumentType(productionPlanningService.getRecepcionDocumentType());
        warehouseVoucher.setResponsible(warehouse.getResponsible());
        warehouseVoucher.setExecutorUnit(businessUnitService.findBusinessUnitByExecutorUnitCode("01"));

        return warehouseVoucher;
    }

    public InventoryMovement createInventoryMovement(String description){
        InventoryMovement inventoryMovement = new InventoryMovement();
        inventoryMovement.setDescription(description);

        return inventoryMovement;
    }

    public void generateValeMaterial(){

        if(productionOrder.getOrderMaterials().size() == 0)
        {
            addMessageVerifyMaterial();
            return;
        }

        WarehouseVoucher warehouseVoucher = new WarehouseVoucher();
        warehouseVoucher = createWarehouseVoucherMaterial();
        InventoryMovement inventoryMovement = createInventoryMovement("egreso de materiales de prueba");
        List<MovementDetail> movementDetails = generateMovementDetailMaterial(productionOrder.getOrderMaterials());

        Map<MovementDetail, BigDecimal> movementDetailUnderMinimalStockMap = new HashMap<MovementDetail, BigDecimal>();
        Map<MovementDetail, BigDecimal> movementDetailOverMaximumStockMap = new HashMap<MovementDetail, BigDecimal>();
        List<MovementDetail> movementDetailWithoutWarnings = new ArrayList<MovementDetail>();
        try {
            warehouseService.saveWarehouseVoucher(warehouseVoucher, inventoryMovement, movementDetails,
                    movementDetailUnderMinimalStockMap,
                    movementDetailOverMaximumStockMap,
                    movementDetailWithoutWarnings);
            addMessageCreateWarehouseVoucherInput();
        } catch (InventoryException e) {
            addInventoryMessages(e.getInventoryMessages());
        } catch (ProductItemNotFoundException e) {
            addProductItemNotFoundMessage(e.getProductItem().getFullName());
        }

        addMaterial = false;
        showProductionOrders = true;
    }

    private String[] getGlossMessage(InventoryMovement inventoryMovement) {
        WarehouseVoucher warehouseVoucher = warehouseVoucherSelect;
        String gloss[] = new String[2];
        String dateString = DateUtils.format(warehouseVoucher.getDate(), MessageUtils.getMessage("patterns.date"));
        String productCodes = QueryUtils.toQueryParameter(movementDetailService.findDetailProductCodeByVoucher(warehouseVoucher));
        String documentName = warehouseVoucher.getDocumentType().getName();
        String sourceWarehouseName = warehouseVoucher.getWarehouse().getName();
        String movementDescription = inventoryMovement.getDescription();

        if (warehouseVoucher.isExecutorUnitTransfer()) {
            String targetWarehouseName = warehouseVoucher.getWarehouse().getName();
            gloss[0] = MessageUtils.getMessage("WarehouseVoucher.message.outTransferenceGloss", documentName, sourceWarehouseName, targetWarehouseName, productCodes, dateString, Constants.WAREHOUSEVOUCHER_NUMBER_PARAM, movementDescription);
            gloss[1] = MessageUtils.getMessage("WarehouseVoucher.message.inTransferenceGloss", documentName, sourceWarehouseName, targetWarehouseName, productCodes, dateString, Constants.WAREHOUSEVOUCHER_NUMBER_PARAM, movementDescription);
        } else {
            String voucherTypeName = messages.get(warehouseVoucher.getDocumentType().getWarehouseVoucherType().getResourceKey());
            gloss[0] = MessageUtils.getMessage("WarehouseVoucher.message.gloss", voucherTypeName, documentName, sourceWarehouseName, productCodes, dateString, Constants.WAREHOUSEVOUCHER_NUMBER_PARAM, movementDescription);
        }

        return gloss;

    }

    public String generateVoucherOrderProduction(){

        productionOrder.setEstateOrder(TABULATED);

        //productionOrder.setEstateOrder(INSTOCK);

        InventoryMovement inventoryMovement = createVale();
        approvalVoucher(inventoryMovement);

        closeDetail();
        showProductionOrders = true;
        update();
        return Outcome.SUCCESS;
    }

    private void addWarehouseVoucherApproveMessage() {
        facesMessages.addFromResourceBundle(StatusMessage.Severity.INFO,
                "WarehouseVoucher.message.approved");
    }

    /**
     * Shows the warnings attribute according to the Maps and List mappings
     */
    public void showMovementDetailWarningMessages() {
        for (Map.Entry<MovementDetail, BigDecimal> movementDetailBigDecimalEntry : movementDetailUnderMinimalStockMap.entrySet()) {
            MovementDetail movementDetail = movementDetailBigDecimalEntry.getKey();
            // if under minimal Stock
            facesMessages.addFromResourceBundle(StatusMessage.Severity.WARN,
                    "MovementDetail.warning.underMinimalStock",
                    movementDetail.getMovementType().equals(MovementDetailType.E) ? messages.get("Common.math.sum") : messages.get("Common.math.subtraction"),
                    FormatUtils.formatNumber(movementDetailBigDecimalEntry.getValue(), messages.get("patterns.decimal6FNumber"), sessionUser.getLocale()),
                    FormatUtils.formatNumber(movementDetail.getQuantity(), messages.get("patterns.decimal6FNumber"), sessionUser.getLocale()),
                    movementDetail.getProductItem().getFullName(),
                    FormatUtils.formatNumber(movementDetail.getProductItem().getMinimalStock(), messages.get("patterns.decimal6FNumber"), sessionUser.getLocale()),
                    movementDetail.getWarehouse().getFullName());
        }
        for (Map.Entry<MovementDetail, BigDecimal> movementDetailBigDecimalEntry : movementDetailOverMaximumStockMap.entrySet()) {
            MovementDetail movementDetail = movementDetailBigDecimalEntry.getKey();
            // if over maximumStock
            facesMessages.addFromResourceBundle(StatusMessage.Severity.WARN,
                    "MovementDetail.warning.overMaximumStock",
                    movementDetail.getMovementType().equals(MovementDetailType.E) ? messages.get("Common.math.sum") : messages.get("Common.math.subtraction"),
                    FormatUtils.formatNumber(movementDetailBigDecimalEntry.getValue(), messages.get("patterns.decimal6FNumber"), sessionUser.getLocale()),
                    FormatUtils.formatNumber(movementDetail.getQuantity(), messages.get("patterns.decimal6FNumber"), sessionUser.getLocale()),
                    movementDetail.getProductItem().getFullName(),
                    FormatUtils.formatNumber(movementDetail.getProductItem().getMaximumStock(), messages.get("patterns.decimal6FNumber"), sessionUser.getLocale()),
                    movementDetail.getWarehouse().getFullName());
        }
    }

    public void approvalVoucher(InventoryMovement inventoryMovement){

        try {
            for (MovementDetail movementDetail : movementDetailSelect) {
                buildValidateQuantityMappings(movementDetail);
            }
            approvalWarehouseVoucherService.approveWarehouseVoucherOrderProduction(
                    warehouseVoucherSelect.getId(),
                    getGlossMessage(inventoryMovement),
                    movementDetailUnderMinimalStockMap,
                    movementDetailOverMaximumStockMap,
                    movementDetailWithoutWarnings,
                    accountOrderProductions);
            addWarehouseVoucherApproveMessage();
            showMovementDetailWarningMessages();
        } catch (InventoryException e) {
            addInventoryMessages(e.getInventoryMessages());
            return ;
        } catch (WarehouseVoucherApprovedException e) {
            addWarehouseVoucherApprovedMessage();
            return ;
        } catch (WarehouseVoucherEmptyException e) {
            addWarehouseVoucherEmptyException();
            return ;
        } catch (WarehouseVoucherNotFoundException e) {
            addNotFoundMessage();
            return ;
        } catch (ProductItemAmountException e) {
            addNotEnoughAmountMessage(e.getProductItem(), e.getAvailableAmount());
            return ;
        } catch (InventoryUnitaryBalanceException e) {
            addInventoryUnitaryBalanceErrorMessage(e.getAvailableUnitaryBalance(), e.getProductItem());
            return ;
        } catch (InventoryProductItemNotFoundException e) {
            addInventoryProductItemNotFoundErrorMessage(e.getExecutorUnitCode(),
                    e.getProductItem(), e.getWarehouse());
            return ;
        } catch (CompanyConfigurationNotFoundException e) {
            addCompanyConfigurationNotFoundErrorMessage();
            return ;
        } catch (FinancesExchangeRateNotFoundException e) {
            addFinancesExchangeRateNotFoundExceptionMessage();
            return ;
        } catch (FinancesCurrencyNotFoundException e) {
            addFinancesExchangeRateNotFoundExceptionMessage();
            return ;
        } catch (ConcurrencyException e) {
            addUpdateConcurrencyMessage();
            return ;
        } catch (ReferentialIntegrityException e) {
            addDeleteReferentialIntegrityMessage();
            return ;
        } catch (ProductItemNotFoundException e) {
            addProductItemNotFoundMessage(e.getProductItem().getFullName());
            return ;
        }
    }

    public void buildValidateQuantityMappings(MovementDetail movementDetail) throws ProductItemNotFoundException {
        BigDecimal requiredQuantity = movementDetail.getQuantity();
        if (null != requiredQuantity) {
            ProductItem productItem = null;
            try {
                productItem = getService().findById(ProductItem.class, movementDetail.getProductItem().getId(), true);
            } catch (EntryNotFoundException e) {
                throw new ProductItemNotFoundException(productItem);
            }
            Warehouse warehouse = movementDetail.getWarehouse();
            BigDecimal minimalStock = productItem.getMinimalStock();
            BigDecimal maximumStock = productItem.getMaximumStock();
            BigDecimal unitaryBalance = inventoryService.findUnitaryBalanceByProductItemAndArticle(warehouse.getId(), productItem.getId());
            BigDecimal totalQuantity = movementDetail.getMovementType().equals(MovementDetailType.E) ?
                    BigDecimalUtil.sum(unitaryBalance, requiredQuantity, SCALE) :
                    BigDecimalUtil.subtract(unitaryBalance, requiredQuantity, SCALE);
            // by default does not show warning until is verified
            boolean showWarning = false;

            if (null != minimalStock) {
                // minimalStock is not null
                int minimalComparison = totalQuantity.compareTo(minimalStock);
                if (minimalComparison < 0) {
                    // if under minimalStock
                    this.movementDetailUnderMinimalStockMap.put(movementDetail, unitaryBalance);
                    showWarning = true;
                }
            }
            if (null != maximumStock) {
                // maximumStock is not null
                int maximumComparison = totalQuantity.compareTo(maximumStock);
                if (maximumComparison > 0) {
                    // if over maximumStock
                    this.movementDetailOverMaximumStockMap.put(movementDetail, unitaryBalance);
                    showWarning = true;
                }
            }
            if (!showWarning) {
                movementDetailWithoutWarnings.add(movementDetail);
            }
        }
    }

    protected void addFinancesExchangeRateNotFoundExceptionMessage() {
        facesMessages.addFromResourceBundle(StatusMessage.Severity.ERROR,
                "PurchaseOrder.financesExchangeRateNotFound");
    }

    protected void addInventoryProductItemNotFoundErrorMessage(String executorUnitCode,
                                                               ProductItem productItem,
                                                               Warehouse warehouse) {
        facesMessages.addFromResourceBundle(StatusMessage.Severity.WARN,
                "warehouseVoucher.approve.error.productItemNotFound",
                productItem.getName(),
                warehouse.getName(),
                executorUnitCode);
    }

    protected void addInventoryUnitaryBalanceErrorMessage(BigDecimal availableUnitaryBalance,
                                                          ProductItem productItem) {
        facesMessages.addFromResourceBundle(StatusMessage.Severity.ERROR,
                "warehouseVoucher.approve.error.notEnoughUnitaryBalance",
                productItem.getName(),
                availableUnitaryBalance);
    }

    public void addWarehouseVoucherApprovedMessage() {
        facesMessages.addFromResourceBundle(StatusMessage.Severity.ERROR,
                "WarehouseVoucher.error.approved");
    }

    protected void addWarehouseVoucherEmptyException() {
        facesMessages.addFromResourceBundle(StatusMessage.Severity.ERROR,
                "WarehouseVoucher.error.empty");
    }

    protected void addNotEnoughAmountMessage(ProductItem productItem,
                                             BigDecimal availableAmount) {
        facesMessages.addFromResourceBundle(StatusMessage.Severity.ERROR,
                "WarehouseVoucher.approve.error.notEnoughAmount",
                productItem.getName(),
                availableAmount);
    }

    public InventoryMovement createVale(){

        WarehouseVoucher warehouseVoucher = new WarehouseVoucher();
        warehouseVoucher = createWarehouseVoucherOrder();
        warehouseVoucherSelect = warehouseVoucher;
        InventoryMovement inventoryMovement = createInventoryMovement(MessageUtils.getMessage("Warehousevoucher.gloss.productionOrder", productionOrder.getCode()));
        List<MovementDetail> movementDetails = generateMovementDetailOrder(productionOrder);
        movementDetailSelect = movementDetails;
        accountOrderProductions = generateMovementDetailOrderProduction(productionOrder);
        Map<MovementDetail, BigDecimal> movementDetailUnderMinimalStockMap = new HashMap<MovementDetail, BigDecimal>();
        Map<MovementDetail, BigDecimal> movementDetailOverMaximumStockMap = new HashMap<MovementDetail, BigDecimal>();
        List<MovementDetail> movementDetailWithoutWarnings = new ArrayList<MovementDetail>();
        try {
            warehouseService.saveWarehouseVoucher(warehouseVoucher, inventoryMovement, movementDetails,
                    movementDetailUnderMinimalStockMap,
                    movementDetailOverMaximumStockMap,
                    movementDetailWithoutWarnings);
            addMessageCreateWarehouseVoucherInput();
        } catch (InventoryException e) {
            addInventoryMessages(e.getInventoryMessages());
            return null;
        } catch (ProductItemNotFoundException e) {
            addProductItemNotFoundMessage(e.getProductItem().getFullName());
            return null;
        }
        return inventoryMovement;
    }

    private List<AccountOrderProduction> generateMovementDetailOrderProduction(ProductionOrder order) {
        List<AccountOrderProduction> accountOrderProductions = new ArrayList<AccountOrderProduction>();
        accountOrderProductions.addAll(generateAccountOrderProductionsMaterial(order.getOrderMaterials()));
        accountOrderProductions.addAll(generateAccountOrderProductionsInput(order.getOrderInputs()));
        accountOrderProductions.addAll(generateAccountOrderProductionsIndirectCost(order.getIndirectCostses()));
        return accountOrderProductions;
    }

    private List<AccountOrderProduction> generateAccountOrderProductionsIndirectCost(List<IndirectCosts> indirectCostses) {
        List<AccountOrderProduction> accountOrderProductions = new ArrayList<AccountOrderProduction>();
        for(IndirectCosts costs: indirectCostses)
        {
            AccountOrderProduction accountOrderProduction = new AccountOrderProduction();
            accountOrderProduction.setCashAccount(costs.getCostsConifg().getCashAccount());
            accountOrderProduction.setCostCenterCode(warehouseVoucherSelect.getCostCenterCode());
            accountOrderProduction.setExecutorUnit(warehouseVoucherSelect.getExecutorUnit());
            accountOrderProduction.setVoucherAmount(costs.getAmountBs());
            accountOrderProductions.add(accountOrderProduction);
        }

        return accountOrderProductions;
    }

    private List<AccountOrderProduction> generateAccountOrderProductionsMaterial(List<OrderMaterial> materials) {
        List<AccountOrderProduction> accountOrderProductions = new ArrayList<AccountOrderProduction>();
        for(OrderMaterial material: materials)
        {
            AccountOrderProduction accountOrderProduction = new AccountOrderProduction();
            accountOrderProduction.setCashAccount(material.getProductItem().getCashAccount());
            accountOrderProduction.setCostCenterCode(warehouseVoucherSelect.getCostCenterCode());
            accountOrderProduction.setExecutorUnit(warehouseVoucherSelect.getExecutorUnit());
            accountOrderProduction.setVoucherAmount(material.getCostTotal());
            accountOrderProductions.add(accountOrderProduction);
        }

        return accountOrderProductions;
    }

    private List<AccountOrderProduction> generateAccountOrderProductionsInput(List<OrderInput> inputs) {
        List<AccountOrderProduction> accountOrderProductions = new ArrayList<AccountOrderProduction>();
        for(OrderInput orderInput: inputs)
        {
            AccountOrderProduction accountOrderProduction = new AccountOrderProduction();
            accountOrderProduction.setCashAccount(orderInput.getProductItem().getCashAccount());
            accountOrderProduction.setCostCenterCode(warehouseVoucherSelect.getCostCenterCode());
            accountOrderProduction.setExecutorUnit(warehouseVoucherSelect.getExecutorUnit());
            accountOrderProduction.setVoucherAmount(orderInput.getCostTotal());
            accountOrderProductions.add(accountOrderProduction);
        }

        return accountOrderProductions;
    }

    private List<MovementDetail> generateMovementDetailOrder(ProductionOrder productionOrder) {

        List<MovementDetail> movementDetails = new ArrayList<MovementDetail>();

        MovementDetail detail = new MovementDetail();
        ProductItem item = productionOrder.getProductComposition().getProcessedProduct().getProductItem();
        detail.setProductItemCode(item.getProductItemCode());
        detail.setQuantity(new BigDecimal(productionOrder.getProducedAmount()));
        detail.setProductItemAccount(item.getProductItemAccount());
        detail.setMeasureCode(item.getUsageMeasureUnit().getMeasureUnitCode());
        detail.setUnitCost(productionOrder.getUnitCost());
        detail.setAmount(new BigDecimal(productionOrder.getTotalCostProduction()));
        detail.setProductItem(item);
        detail.setWarehouse(warehouseService.findWarehouseByCode("2"));
        detail.setMeasureUnit(item.getUsageMeasureUnit());
        movementDetails.add(detail);

        return movementDetails;
    }

    public class AccountOrderProduction{

        private BusinessUnit executorUnit;
        private String costCenterCode;
        private CashAccount cashAccount;
        private BigDecimal voucherAmount;

        public BusinessUnit getExecutorUnit() {
            return executorUnit;
        }

        public void setExecutorUnit(BusinessUnit executorUnit) {
            this.executorUnit = executorUnit;
        }

        public String getCostCenterCode() {
            return costCenterCode;
        }

        public void setCostCenterCode(String costCenterCode) {
            this.costCenterCode = costCenterCode;
        }

        public CashAccount getCashAccount() {
            return cashAccount;
        }

        public void setCashAccount(CashAccount cashAccount) {
            this.cashAccount = cashAccount;
        }

        public BigDecimal getVoucherAmount() {
            return voucherAmount;
        }

        public void setVoucherAmount(BigDecimal voucherAmount) {
            this.voucherAmount = voucherAmount;
        }
    }

    public void generateValeInput(){

        WarehouseVoucher warehouseVoucher = new WarehouseVoucher();
        warehouseVoucher = createWarehouseVoucherInput();
        InventoryMovement inventoryMovement = createInventoryMovement("egreso de insumos de prueba");

        /*lista de insumos*/
        List<MovementDetail> movementDetails = generateMovementDetailInput(productionOrder.getOrderInputs());
        /*...*/
        Map<MovementDetail, BigDecimal> movementDetailUnderMinimalStockMap = new HashMap<MovementDetail, BigDecimal>();
        Map<MovementDetail, BigDecimal> movementDetailOverMaximumStockMap = new HashMap<MovementDetail, BigDecimal>();
        List<MovementDetail> movementDetailWithoutWarnings = new ArrayList<MovementDetail>();

        try {
            warehouseService.saveWarehouseVoucher(warehouseVoucher, inventoryMovement, movementDetails,
                    movementDetailUnderMinimalStockMap,
                    movementDetailOverMaximumStockMap,
                    movementDetailWithoutWarnings);
            addMessageCreateWarehouseVoucherInput();
        } catch (InventoryException e) {
            addInventoryMessages(e.getInventoryMessages());
            return;
        } catch (ProductItemNotFoundException e) {
            addProductItemNotFoundMessage(e.getProductItem().getFullName());
            return;
        }

        clearFormulation();
        disableEditingFormula();
        showProductionOrders = true;

    }

    public void addProductItemNotFoundMessage(String productItemName) {
        facesMessages.addFromResourceBundle(StatusMessage.Severity.WARN,
                "ProductItem.error.notFound", productItemName);
    }

    public void addInventoryMessages(List<InventoryMessage> messages) {
        for (InventoryMessage message : messages) {
            if (message.isNotFound()) {
                facesMessages.addFromResourceBundle(StatusMessage.Severity.ERROR,
                        "WarehouseVoucher.error.InventoryNotFound", message.getProductItem().getName());
                continue;
            }

            if (message.isNotEnough()) {
                facesMessages.addFromResourceBundle(StatusMessage.Severity.ERROR,
                        "WarehouseVoucher.error.InventoryUnavailableProductItems", message.getProductItem().getName(),
                        message.getAvailableQuantity());
            }
        }
    }
    //TODO: Revisar el almacen de productos
    private List<MovementDetail> generateMovementDetailMaterial(List<OrderMaterial> orderMaterials) {
        List<MovementDetail> movementDetails = new ArrayList<MovementDetail>();

        for(OrderMaterial material: orderMaterials)
        {
            if(!articleEstateService.verifyEstate(material.getProductItem(),Constants.ESTATE_ARTICLE_NOTVERIFY))
            {
                MovementDetail detail = new MovementDetail();
                detail.setProductItemCode(material.getProductItemCode());
                detail.setQuantity(new BigDecimal(material.getAmountUsed()));
                detail.setProductItemAccount(material.getProductItem().getProductItemAccount());
                detail.setMeasureCode(material.getProductItem().getUsageMeasureUnit().getMeasureUnitCode());
                detail.setUnitCost(material.getCostUnit());
                detail.setAmount(material.getCostTotal());
                detail.setProductItem(material.getProductItem());
                //detail.setWarehouse(inventoryService.findWarehouseByItemArticle(material.getProductItem()));
                detail.setWarehouse(warehouseService.findWarehouseByCode("2"));
                detail.setMeasureUnit(material.getProductItem().getUsageMeasureUnit());
                movementDetails.add(detail);
            }

        }

        return movementDetails;
    }

    private List<MovementDetail> generateMovementDetailInput(List<OrderInput> orderInputs) {
        List<MovementDetail> movementDetails = new ArrayList<MovementDetail>();
        for(OrderInput input: orderInputs)
        {
            if(!articleEstateService.verifyEstate(input.getProductItem(),Constants.ESTATE_ARTICLE_NOTVERIFY))
            {   /*if(!articleEstateService.verifyEstate(input.getProductItem(),Constants.ESTATE_ARTICLE_COMPOSITE))
                { */
                    MovementDetail detail = new MovementDetail();
                    detail.setProductItemCode(input.getProductItemCode());
                    detail.setQuantity(new BigDecimal(input.getAmount()));
                    detail.setProductItemAccount(input.getProductItem().getProductItemAccount());
                    detail.setMeasureCode(input.getProductItem().getUsageMeasureUnit().getMeasureUnitCode());
                    detail.setUnitCost(input.getCostUnit());
                    detail.setAmount(input.getCostTotal());
                    detail.setProductItem(input.getProductItem());
                    detail.setWarehouse(warehouseService.findWarehouseByCode("2"));
                    detail.setMeasureUnit(input.getProductItem().getUsageMeasureUnit());
                    movementDetails.add(detail);

                /*}else{
                    ProductComposition composition = processedProductService.getProductComposite(ingredient.getMetaProduct().getId());
                    movementDetails.addAll(generateMovementDetailInput(productionPlanningService. input.));
                } */
            }

        }

        return movementDetails;
    }

    private Double calculateCostTotal(List<ProductionIngredient> ingredients)
    {
        Double costTotal = 0.0;
        for(ProductionIngredient ingredient : ingredients){
            costTotal += ingredient.getAmount() * ingredient.getMetaProduct().getProductItem().getUnitCost().doubleValue();
        }

        return costTotal;
    }

    //@End
    //public String removeFormulation() {
    public void removeFormulation() {
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
        showProductionOrders = true;
        //return result;
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
        showProductionOrders = false;

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
        showProductionOrders = false;
    }

    public void selectMaterialDetail(ProductionOrder order) {
        disableEditingFormula();
        productionOrderMaterial = order;
        productionOrder = order;
        orderMaterials = new ArrayList<OrderMaterial>();
        orderMaterials.addAll(order.getOrderMaterials());

        showMaterialDetail = true;
        showProductionOrders = false;
    }

    public void selectDetail(ProductionOrder order) {
        cancelFormulation();
        addMaterial = false;
        //productionOrderMaterial = order;
        productionOrder = order;
        if (productionOrder.getId() != null) {
            setTotalsMaterials(productionOrder);
            setTotalsInputs(productionOrder);
            //setTotalHour(productionOrder);
            //setTotalIndiRectCost(productionOrder);
            setTotalCostProducticionAndUnitPrice(productionOrder);
        }
        //orderMaterials = new ArrayList<OrderMaterial>();
        //orderMaterials.addAll(order.getOrderMaterials());

        showDetailOrder = true;
        showProductionOrders = false;
    }

    //@End(ifOutcome = Outcome.SUCCESS)
    //public String makeExecutedOrder() {
    public void makeExecutedOrder() {
        //getInstance().setState(EXECUTED);
        productionOrder.setEstateOrder(EXECUTED);
        ProductionPlanning productionPlanning = getInstance();
        //for (ProductionOrder productionOrder : productionPlanning.getProductionOrderList()) {
            setTotalsMaterials(productionOrder);
            setTotalsInputs(productionOrder);
            setTotalIndiRectCost(productionOrder);
            //setTotalHour(productionOrder);
            setTotalCostProducticionAndUnitPrice(productionOrder);
        //}
        String outcome = update();

        if (outcome != Outcome.SUCCESS) {
            productionOrder.setEstateOrder(PENDING);
            //getInstance().setState(PENDING);
        }
        disableEditingFormula();
        //productionOrder = null;
        showDetailOrder = false;
        showProductionOrders = true;
        //return outcome;
    }

    //@End(ifOutcome = Outcome.SUCCESS)
    //public String makeFinalizedOrder() {
    public void makeFinalizedOrder() {
        //getInstance().setState(FINALIZED);
        productionOrder.setEstateOrder(FINALIZED);
        ProductionPlanning productionPlanning = getInstance();
        //for (ProductionOrder productionOrder : productionPlanning.getProductionOrderList()) {
            setTotalsMaterials(productionOrder);
            setTotalsInputs(productionOrder);
            setTotalIndiRectCost(productionOrder);
            //setTotalHour(productionOrder);
            setTotalCostProducticionAndUnitPrice(productionOrder);
        //}
        String outcome = update();

        if (outcome != Outcome.SUCCESS) {
            productionOrder.setEstateOrder(EXECUTED);
            //getInstance().setState(EXECUTED);
        }
        showProductionOrders = true;
        disableEditingFormula();
        //productionOrder = null;
        showDetailOrder = false;
    }

    public void closeDetail() {
        disableEditingFormula();
        //productionOrder = null;
        showDetailOrder = false;
        showProductionOrders = true;
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
                material.setAmountReturned(RoundUtil.getRoundValue(amountReturn, 2, RoundUtil.RoundMode.SYMMETRIC));
                material.setCostUnit(material.getProductItem().getUnitCost());
                material.setCostTotal(new BigDecimal(total));

            }
        }
        ProductionPlanning productionPlanning = getInstance();
        int position = productionPlanning.getProductionOrderList().indexOf(productionOrder);
        productionPlanning.getProductionOrderList().get(position).getOrderMaterials().clear();
        productionPlanning.getProductionOrderList().get(position).setOrderMaterials(orderMaterials);


        addMaterial = false;
        if (productionPlanning.getId() != null) {
            setTotalsMaterials(productionOrder);
            setTotalsInputs(productionOrder);
            //setTotalIndiRectCost(productionOrder);
            //setTotalHour(productionOrder);
            setTotalCostProducticionAndUnitPrice(productionOrder);

            if (update() != Outcome.SUCCESS) {
                return;
            }

        }
        orderMaterials.clear();
        selectedProductItems.clear();
        showProductionOrders = true;
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


    private Boolean verifySotckByProductionPlannig(ProductionPlanning planning) {
        Boolean band = true;
        for (ProductionOrder order : planning.getProductionOrderList()) {
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
        //setTotalIndiRectCost(productionOrder);
        //setTotalHour(productionOrder);
        setTotalCostProducticionAndUnitPrice(productionOrder);
        if (update() != Outcome.SUCCESS) {
            return;
        }

        existingFormulation = null;
        disableEditingFormula();
        showProductionOrders = true;
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
        showProductionOrders = true;
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
            //setTotalHour(productionOrder);
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
            //setTotalHour(productionOrder);
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
    //todo: metodo fuera de servicio
    public void setTotalHour(ProductionOrder productionOrder) {
        productionOrder.setTotalPriceJourney(((BigDecimal) (employeeTimeCardService.getCostProductionOrder(productionOrder, getInstance().getDate(), getTotalVolumProductionPlaning(productionOrder)))).doubleValue());
    }

    public void setTotalCostProducticionAndUnitPrice(ProductionOrder productionOrder) {
        Double total = productionOrder.getTotalPriceMaterial() + productionOrder.getTotalPriceInput() + productionOrder.getTotalPriceJourney() + productionOrder.getTotalIndirectCosts();
        productionOrder.setTotalCostProduction(total);
        Double priceUnit = 0.0;
        if (productionOrder.getProducedAmount() > 0.0)
            priceUnit = total / productionOrder.getProducedAmount();

        //productionOrder.getProductComposition().getProcessedProduct().getProductItem().setUnitCost(new BigDecimal(priceUnit));
        //Todo: Revisar
        productionOrder.setUnitCost(new BigDecimal(priceUnit));
    }

    public void setTotalIndiRectCost(ProductionOrder productionOrder) {
        if(productionOrder.getIndirectCostses() != null)
            productionOrder.getIndirectCostses().clear();

        productionOrder.getIndirectCostses().addAll(indirectCostsService.getCostTotalIndirect(
                                                                                     productionOrder,
                                                                                     getTotalVolumProductionPlaning(productionOrder),
                                                                                     getTotalVolumGeneralProductionPlaning(productionOrder),
                                                                                     indirectCostsService.getLastPeroidIndirectCost()
                                                                                     )
                                           );
        setTotalIndirectCosts(productionOrder);
        //productionOrder.setTotalIndirectCosts(indirectCostsService.getCostTotalIndirect(productionOrder, getTotalVolumProductionPlaning(productionOrder), getTotalVolumGeneralProductionPlaning(productionOrder)));
    }
    //todo: la mano de obra directa se tomara diretamente de la tabla costosindirectos para no cambiar mucho
    //se fijara directamente desde eseta tabla temporalmente
    private void setTotalIndirectCosts(ProductionOrder productionOrder) {
        Double total = 0.0;
        productionOrder.setTotalPriceJourney(0.0);
        for(IndirectCosts costs :productionOrder.getIndirectCostses())
        {
            if(costs.getCostsConifg().getEstate() != null)
            {if(costs.getCostsConifg().getEstate().compareTo(Constants.ESTATE_COSTCONFIG) == 0)
                productionOrder.setTotalPriceJourney(productionOrder.getTotalPriceJourney()+costs.getAmountBs().doubleValue());
            }
            else
            total = total + costs.getAmountBs().doubleValue();
        }
        productionOrder.setTotalIndirectCosts(total);
    }

    public void setTotalsInputs(ProductionOrder productionOrder) {
        Double totalInput = 0.0;

        for (OrderInput input : productionOrder.getOrderInputs()) {
            //totalInput += RoundUtil.getRoundValue((input.getProductItem().getUnitCost().doubleValue()) * input.getAmount(),2, RoundUtil.RoundMode.SYMMETRIC);
            if (!isNotCountAs(input.getProductItem()))
                totalInput += (input.getProductItem().getUnitCost().doubleValue()) * input.getAmount();
        }

        productionOrder.setTotalPriceInput(RoundUtil.getRoundValue(totalInput, 2, RoundUtil.RoundMode.SYMMETRIC));
    }

    public void setTotalsMaterials(ProductionOrder productionOrder) {
        Double totalMaterial = 0.0;
        for (OrderMaterial material : productionOrder.getOrderMaterials()) {
            totalMaterial += material.getCostTotal().doubleValue();
        }
        productionOrder.setTotalPriceMaterial(RoundUtil.getRoundValue(totalMaterial, 2, RoundUtil.RoundMode.SYMMETRIC));
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
        for (ProductionOrder order : getInstance().getProductionOrderList()) {
            total += employeeTimeCardService.getTotalVolumeOrder(order);
        }
        return total;
    }

    public Double getTotalVolumProductionPlaning(ProductionOrder productionOrder) {
        Double total = 0.0;
        for (ProductionOrder order : getInstance().getProductionOrderList()) {
            if (productionOrder.getProductComposition().getProcessedProduct().getProductItem().getSubGroup().getGroup() == order.getProductComposition().getProcessedProduct().getProductItem().getSubGroup().getGroup())
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
        showProductionOrders = true;
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

    public Boolean getShowProductionOrders() {
        return showProductionOrders;
    }

    public void setShowProductionOrders(Boolean showProductionOrders) {
        this.showProductionOrders = showProductionOrders;
    }
}
