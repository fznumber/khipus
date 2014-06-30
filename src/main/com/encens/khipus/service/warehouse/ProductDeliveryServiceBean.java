package com.encens.khipus.service.warehouse;

import com.encens.khipus.exception.ConcurrencyException;
import com.encens.khipus.exception.EntryDuplicatedException;
import com.encens.khipus.exception.ReferentialIntegrityException;
import com.encens.khipus.exception.finances.CompanyConfigurationNotFoundException;
import com.encens.khipus.exception.finances.FinancesCurrencyNotFoundException;
import com.encens.khipus.exception.finances.FinancesExchangeRateNotFoundException;
import com.encens.khipus.exception.warehouse.*;
import com.encens.khipus.framework.service.GenericServiceBean;
import com.encens.khipus.model.employees.Employee;
import com.encens.khipus.model.finances.CostCenter;
import com.encens.khipus.model.finances.CostCenterPk;
import com.encens.khipus.model.products.Product;
import com.encens.khipus.model.warehouse.*;
import com.encens.khipus.util.Constants;
import com.encens.khipus.util.DateUtils;
import com.encens.khipus.util.MessageUtils;
import com.encens.khipus.util.ValidatorUtil;
import com.encens.khipus.util.query.QueryUtils;
import com.encens.khipus.util.warehouse.InventoryMessage;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.*;

/**
 * @author
 * @version 2.4
 */
@Stateless
@Name("productDeliveryService")
@AutoCreate
public class ProductDeliveryServiceBean extends GenericServiceBean implements ProductDeliveryService {

    @In
    private SoldProductService soldProductService;

    @In
    private ApprovalWarehouseVoucherService approvalWarehouseVoucherService;

    @In
    private WarehouseService warehouseService;

    @In
    private MovementDetailService movementDetailService;

    @In
    private ProductItemService productItemService;

    @In
    protected FacesMessages facesMessages;

    @In(value = "monthProcessService")
    private MonthProcessService monthProcessService;

    @In(value = "#{listEntityManager}")
    private EntityManager listEm;

    public ProductDelivery select(ProductDelivery entity) {
        return getEntityManager().find(ProductDelivery.class, entity.getId());
    }

    @Override
    public void updateOrderEstate(String invoiceNumber) {

        getEntityManager().createNativeQuery("update USER01_DAF.pedidos set estado_pedido = :state where pedido = :pedido")
                                           .setParameter("pedido",invoiceNumber)
                                           .setParameter("state",Constants.ESTATE_ORDER_DELIVERED)
                                           .executeUpdate();
    }

    public void updateOrderIncashEstate(String invoiceNumber) {

        getEntityManager().createNativeQuery("update WISE.inv_ventart set ESTADO = :state where no_fact = :pedido")
                .setParameter("pedido",invoiceNumber)
                .setParameter("state",Constants.ESTATE_ORDER_DELIVERED_INCASH)
                .executeUpdate();
    }
    @SuppressWarnings(value = "unchecked")
    public void deliveryAll(List<String> numberInvoices) throws InventoryException, ProductItemNotFoundException, ProductItemAmountException, CompanyConfigurationNotFoundException, FinancesExchangeRateNotFoundException, FinancesCurrencyNotFoundException, InventoryProductItemNotFoundException, ReferentialIntegrityException, ConcurrencyException, InventoryUnitaryBalanceException, EntryDuplicatedException {
        for(String invoiceNumber:numberInvoices)
        {
            String warehouseDescription = MessageUtils.getMessage("ProductDelivery.warehouseVoucher.description", invoiceNumber);
            List<SoldProduct> soldProducts = soldProductService.getSoldProductsWithoutCutCheese(invoiceNumber, Constants.defaultCompanyNumber);
            int pos = changeEdamToPressed(soldProducts);
            WarehouseDocumentType documentType = getFirstConsumptionType();
            //always exist almost one sold product that will be delivery
            SoldProduct firstSoldProduct = soldProducts.get(0);
            Warehouse warehouse = firstSoldProduct.getWarehouse();
            CostCenter costCenter = findPublicCostCenter(warehouse);
            Employee responsible = getEntityManager().find(Employee.class, warehouse.getResponsibleId());

            WarehouseVoucher warehouseVoucher = createWarehouseVoucherAll(documentType, warehouse, responsible, costCenter, warehouseDescription, soldProducts);

            Map<MovementDetail, BigDecimal> movementDetailUnderMinimalStockMap = new HashMap<MovementDetail, BigDecimal>();
            Map<MovementDetail, BigDecimal> movementDetailOverMaximumStockMap = new HashMap<MovementDetail, BigDecimal>();
            List<MovementDetail> movementDetailWithoutWarnings = new ArrayList<MovementDetail>();

            try {

                approvalWarehouseVoucherService.approveWarehouseVoucherFromDeliveryProduct(warehouseVoucher.getId(),
                        getGlossMessage(warehouseVoucher, warehouseDescription),
                        movementDetailUnderMinimalStockMap,
                        movementDetailOverMaximumStockMap,
                        movementDetailWithoutWarnings);

            } catch (WarehouseVoucherApprovedException e) {
                log.debug("This exception never happen because I create a pending WarehouseVoucher.");
            } catch (WarehouseVoucherNotFoundException e) {
                log.debug("This exception never happen because I create a new WarehouseVoucher.");
            } catch (WarehouseVoucherEmptyException e) {
                log.debug("This exception never happen because I create a WarehouseVoucher with details inside.");
            } catch (WarehouseAccountCashNotFoundException e) {
                e.printStackTrace();
            }

            ProductDelivery productDelivery = new ProductDelivery();
            productDelivery.setCompanyNumber(warehouse.getId().getCompanyNumber());
            productDelivery.setInvoiceNumber(firstSoldProduct.getInvoiceNumber());
            productDelivery.setWarehouseVoucher(warehouseVoucher);

            create(productDelivery);

            if(pos>-1)
            {
                resetChange(soldProducts.get(pos));
                approvalWarehouseVoucherService.resetChangeCheeseEdam(warehouseVoucher.getId(),pos);
                ProductItem cheesePressed = productItemService.findProductItemByCode(Constants.COD_CHEESE_PRESSED);
                ProductItem cheeseEDAM = productItemService.findProductItemByCode(Constants.COD_CHEESE_EDAM);
                cheeseEDAM.setUnitCost(cheesePressed.getUnitCost());
                try {
                    productItemService.updateProductItem(cheeseEDAM);
                } catch (ProductItemMinimalStockIsGreaterThanMaximumStockException e) {
                    e.printStackTrace();
                }
            }

            for (SoldProduct soldProduct : soldProducts) {
                soldProduct.setProductDelivery(productDelivery);
                soldProduct.setState(SoldProductState.DELIVERED);
                soldProduct.setNumberVoucher(warehouseVoucher.getNumber());
                update(soldProduct);
            }

            //update state of order
            if(firstSoldProduct.getOrderNumber() != null)
                updateOrderEstate(firstSoldProduct.getOrderNumber());

        }
    }

    @SuppressWarnings(value = "unchecked")
    public ProductDelivery createAll(String invoiceNumber, String warehouseDescription)
            throws InventoryException,
            WarehouseDocumentTypeNotFoundException,
            PublicCostCenterNotFound,
            ProductItemAmountException,
            InventoryUnitaryBalanceException,
            InventoryProductItemNotFoundException,
            SoldProductDeliveredException,
            CompanyConfigurationNotFoundException,
            FinancesCurrencyNotFoundException,
            FinancesExchangeRateNotFoundException,
            EntryDuplicatedException,
            ConcurrencyException,
            ReferentialIntegrityException,
            ProductItemNotFoundException, SoldProductNotFoundException {

        //check if the sold products keep the pending state
        soldProductStateChecker(invoiceNumber, Constants.defaultCompanyNumber);
        List<SoldProduct> soldProducts = soldProductService.getSoldProductsWithoutCutCheese(invoiceNumber, Constants.defaultCompanyNumber);
        //List<SoldProduct> soldProducts = soldProductService.getSoldProducts(invoiceNumber, Constants.defaultCompanyNumber);
        int pos = changeEdamToPressed(soldProducts);
        WarehouseDocumentType documentType = getFirstConsumptionType();

        if(soldProducts.size() == 0)
        {
            throw new SoldProductNotFoundException();
        }

        if (null == documentType) {
            throw new WarehouseDocumentTypeNotFoundException("Warehouse document consumption type not found");
        }

        //always exist almost one sold product that will be delivery
        SoldProduct firstSoldProduct = soldProducts.get(0);

        Warehouse warehouse = firstSoldProduct.getWarehouse();
        CostCenter costCenter = findPublicCostCenter(warehouse);

        if (null == costCenter) {
            throw new PublicCostCenterNotFound("Cannot find a public Cost Center to complete the delivery.");
        }

        productItemStockCheckerWithoutCutCheese(invoiceNumber, warehouse, costCenter);
        //productItemStockChecker(invoiceNumber, warehouse, costCenter);
        Employee responsible = getEntityManager().find(Employee.class, warehouse.getResponsibleId());

        WarehouseVoucher warehouseVoucher = createWarehouseVoucherAll(documentType, warehouse, responsible, costCenter, warehouseDescription, soldProducts);

        Map<MovementDetail, BigDecimal> movementDetailUnderMinimalStockMap = new HashMap<MovementDetail, BigDecimal>();
        Map<MovementDetail, BigDecimal> movementDetailOverMaximumStockMap = new HashMap<MovementDetail, BigDecimal>();
        List<MovementDetail> movementDetailWithoutWarnings = new ArrayList<MovementDetail>();

        /*for (SoldProduct soldProduct : soldProducts) {
            ProductItem productItem = getEntityManager().find(ProductItem.class, soldProduct.getProductItem().getId());
            movementDetailUnderMinimalStockMap.put(movementDetailTemp, productItem.getMinimalStock());
            movementDetailOverMaximumStockMap.put(movementDetailTemp, productItem.getMaximumStock());
            movementDetailWithoutWarnings.add(movementDetailTemp);
        }*/

        try {
            //approvalWarehouseVoucherService.approveWarehouseVoucher(warehouseVoucher.getId(), getGlossMessage(warehouseVoucher, warehouseDescription), null, null, null);
            /*approvalWarehouseVoucherService.approveWarehouseVoucher(warehouseVoucher.getId(),
                                                                    getGlossMessage(warehouseVoucher, warehouseDescription),
                                                                    movementDetailUnderMinimalStockMap,
                                                                    movementDetailOverMaximumStockMap,
                                                                    movementDetailWithoutWarnings);*/

            approvalWarehouseVoucherService.approveWarehouseVoucherFromDeliveryProduct(warehouseVoucher.getId(),
                    getGlossMessage(warehouseVoucher, warehouseDescription),
                    movementDetailUnderMinimalStockMap,
                    movementDetailOverMaximumStockMap,
                    movementDetailWithoutWarnings);

        } catch (WarehouseVoucherApprovedException e) {
            log.debug("This exception never happen because I create a pending WarehouseVoucher.");
        } catch (WarehouseVoucherNotFoundException e) {
            log.debug("This exception never happen because I create a new WarehouseVoucher.");
        } catch (WarehouseVoucherEmptyException e) {
            log.debug("This exception never happen because I create a WarehouseVoucher with details inside.");
        } catch (WarehouseAccountCashNotFoundException e) {
            e.printStackTrace();
        }

        ProductDelivery productDelivery = new ProductDelivery();
        productDelivery.setCompanyNumber(warehouse.getId().getCompanyNumber());
        productDelivery.setInvoiceNumber(firstSoldProduct.getInvoiceNumber());
        productDelivery.setWarehouseVoucher(warehouseVoucher);

        create(productDelivery);

        if(pos>-1)
        {
            resetChange(soldProducts.get(pos));
            approvalWarehouseVoucherService.resetChangeCheeseEdam(warehouseVoucher.getId(),pos);
            ProductItem cheesePressed = productItemService.findProductItemByCode(Constants.COD_CHEESE_PRESSED);
            ProductItem cheeseEDAM = productItemService.findProductItemByCode(Constants.COD_CHEESE_EDAM);
            cheeseEDAM.setUnitCost(cheesePressed.getUnitCost());
            try {
                productItemService.updateProductItem(cheeseEDAM);
            } catch (ProductItemMinimalStockIsGreaterThanMaximumStockException e) {
                e.printStackTrace();
            }
        }

        for (SoldProduct soldProduct : soldProducts) {
            soldProduct.setProductDelivery(productDelivery);
            soldProduct.setState(SoldProductState.DELIVERED);
            soldProduct.setNumberVoucher(warehouseVoucher.getNumber());
            update(soldProduct);
        }

        //update state of order
        if(firstSoldProduct.getOrderNumber() != null)
            updateOrderEstate(firstSoldProduct.getOrderNumber());

        return productDelivery;
    }

    public Boolean verifyAmounts(List<String> numberInvoices){
        Boolean result = false;
        for(String invoiceNumber :numberInvoices) {
            try {
                soldProductStateChecker(invoiceNumber, Constants.defaultCompanyNumber);
            } catch (SoldProductDeliveredException e) {
                addSoldProductDeliveredErrorMessage(invoiceNumber);
                result = true;
            }
            List<SoldProduct> soldProducts = soldProductService.getSoldProductsWithoutCutCheese(invoiceNumber, Constants.defaultCompanyNumber);
            WarehouseDocumentType documentType = getFirstConsumptionType();
            SoldProduct firstSoldProduct = soldProducts.get(0);

            Warehouse warehouse = firstSoldProduct.getWarehouse();
            CostCenter costCenter = findPublicCostCenter(warehouse);
            try {
                productItemStockCheckerWithoutCutCheese(invoiceNumber, warehouse, costCenter);
            } catch (InventoryException e) {
                addInventoryErrorMessages(e.getInventoryMessages());
            }

            if (soldProducts.size() == 0) {
                addSoldProductNotFoundMessages(invoiceNumber);
                result = true;
            }

            if (null == documentType) {
                addWarehouseDocumentTypeErrorMessage(invoiceNumber);
                result = true;
            }

            if (null == costCenter) {
                addcenterCostNotFoundErrorMessage();
                result = true;
            }
        }

        return result;
    }

    private void addInventoryErrorMessages(List<InventoryMessage> messages) {
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

    private void addcenterCostNotFoundErrorMessage() {
        facesMessages.addFromResourceBundle(StatusMessage.Severity.ERROR,
                "ProductDelivery.warehouseVoucher.centerCostNofound");
    }

    private void addWarehouseDocumentTypeErrorMessage(String invoiceNumber) {
        facesMessages.addFromResourceBundle(StatusMessage.Severity.ERROR,
                "ProductDelivery.error.consumptionDocumentTypeNotFound", invoiceNumber);
    }

    private void addSoldProductNotFoundMessages(String invoiceNumber) {
        facesMessages.addFromResourceBundle(StatusMessage.Severity.WARN,
                "ProductDelivery.info.soldProductNotFoundMessages", invoiceNumber);
    }

    private void addSoldProductDeliveredErrorMessage(String invoiceNumber) {
        facesMessages.addFromResourceBundle(StatusMessage.Severity.ERROR,
                "ProductDelivery.error.soldProductDelivered", invoiceNumber);
    }

    private void resetChange(SoldProduct soldProduct) {
          ProductItem item = productItemService.findProductItemByCode(Constants.COD_CHEESE_EDAM);
          soldProduct.setProductItemCode(item.getProductItemCode());
          soldProduct.setProductItem(item);
    }

    private int changeEdamToPressed(List<SoldProduct> soldProducts) {
        int pos = -1;
        int cont = 0;
        for(SoldProduct soldProduct:soldProducts)
        {
            if(soldProduct.getProductItemCode().compareTo(Constants.COD_CHEESE_EDAM) == 0)
            {
                ProductItem item = productItemService.findProductItemByCode(Constants.COD_CHEESE_PRESSED);
                soldProduct.setProductItemCode(item.getProductItemCode());
                soldProduct.setProductItem(item);
                pos = cont;
            }
            cont ++;
        }
        return pos;
    }

    @SuppressWarnings(value = "unchecked")
    public ProductDelivery create(String invoiceNumber, String warehouseDescription)
            throws InventoryException,
            WarehouseDocumentTypeNotFoundException,
            PublicCostCenterNotFound,
            ProductItemAmountException,
            InventoryUnitaryBalanceException,
            InventoryProductItemNotFoundException,
            SoldProductDeliveredException,
            CompanyConfigurationNotFoundException,
            FinancesCurrencyNotFoundException,
            FinancesExchangeRateNotFoundException,
            EntryDuplicatedException,
            ConcurrencyException,
            ReferentialIntegrityException,
            ProductItemNotFoundException, SoldProductNotFoundException {

        //check if the sold products keep the pending state
        soldProductStateChecker(invoiceNumber, Constants.defaultCompanyNumber);
        //List<SoldProduct> soldProducts = soldProductService.getSoldProducts(invoiceNumber, Constants.defaultCompanyNumber);
        List<SoldProduct> soldProducts = soldProductService.getSoldProductsWithoutCutCheese(invoiceNumber, Constants.defaultCompanyNumber);
        WarehouseDocumentType documentType = getFirstConsumptionType();

        if(soldProducts.size() == 0)
        {
            throw new SoldProductNotFoundException();
        }

        if (null == documentType) {
            throw new WarehouseDocumentTypeNotFoundException("Warehouse document consumption type not found");
        }

        //always exist almost one sold product that will be delivery
        SoldProduct firstSoldProduct = soldProducts.get(0);

        Warehouse warehouse = firstSoldProduct.getWarehouse();
        CostCenter costCenter = findPublicCostCenter(warehouse);
        //update state of order
        if(firstSoldProduct.getOrderNumber() != null)
        updateOrderEstate(firstSoldProduct.getOrderNumber());

        if (null == costCenter) {
            throw new PublicCostCenterNotFound("Cannot find a public Cost Center to complete the delivery.");
        }

        productItemStockCheckerWithoutCutCheese(invoiceNumber, warehouse, costCenter);
        //productItemStockChecker(invoiceNumber, warehouse, costCenter);
        Employee responsible = getEntityManager().find(Employee.class, warehouse.getResponsibleId());

        //WarehouseVoucher warehouseVoucher = createWarehouseVoucher(documentType, warehouse, responsible, costCenter, warehouseDescription, soldProducts);
        WarehouseVoucher warehouseVoucher = createWarehouseVoucherAll(documentType, warehouse, responsible, costCenter, warehouseDescription, soldProducts);

        Map<MovementDetail, BigDecimal> movementDetailUnderMinimalStockMap = new HashMap<MovementDetail, BigDecimal>();
        Map<MovementDetail, BigDecimal> movementDetailOverMaximumStockMap = new HashMap<MovementDetail, BigDecimal>();
        List<MovementDetail> movementDetailWithoutWarnings = new ArrayList<MovementDetail>();

        /*for (SoldProduct soldProduct : soldProducts) {
            ProductItem productItem = getEntityManager().find(ProductItem.class, soldProduct.getProductItem().getId());
            movementDetailUnderMinimalStockMap.put(movementDetailTemp, productItem.getMinimalStock());
            movementDetailOverMaximumStockMap.put(movementDetailTemp, productItem.getMaximumStock());
            movementDetailWithoutWarnings.add(movementDetailTemp);
        }*/

        try {
            //approvalWarehouseVoucherService.approveWarehouseVoucher(warehouseVoucher.getId(), getGlossMessage(warehouseVoucher, warehouseDescription), null, null, null);
            /*approvalWarehouseVoucherService.approveWarehouseVoucher(warehouseVoucher.getId(),
                                                                    getGlossMessage(warehouseVoucher, warehouseDescription),
                                                                    movementDetailUnderMinimalStockMap,
                                                                    movementDetailOverMaximumStockMap,
                                                                    movementDetailWithoutWarnings);*/

            approvalWarehouseVoucherService.approveWarehouseVoucherFromDeliveryProduct(warehouseVoucher.getId(),
                    getGlossMessage(warehouseVoucher, warehouseDescription),
                    movementDetailUnderMinimalStockMap,
                    movementDetailOverMaximumStockMap,
                    movementDetailWithoutWarnings);

        } catch (WarehouseVoucherApprovedException e) {
            log.debug("This exception never happen because I create a pending WarehouseVoucher.");
        } catch (WarehouseVoucherNotFoundException e) {
            log.debug("This exception never happen because I create a new WarehouseVoucher.");
        } catch (WarehouseVoucherEmptyException e) {
            log.debug("This exception never happen because I create a WarehouseVoucher with details inside.");
        } catch (WarehouseAccountCashNotFoundException e) {
            e.printStackTrace();
        }
        ProductDelivery productDelivery = new ProductDelivery();
        productDelivery.setCompanyNumber(warehouse.getId().getCompanyNumber());
        productDelivery.setInvoiceNumber(firstSoldProduct.getInvoiceNumber());
        productDelivery.setWarehouseVoucher(warehouseVoucher);

        create(productDelivery);


        for (SoldProduct soldProduct : soldProducts) {
            soldProduct.setProductDelivery(productDelivery);
            soldProduct.setState(SoldProductState.DELIVERED);
            update(soldProduct);
        }

        return productDelivery;
    }

    private WarehouseVoucher createWarehouseVoucher(WarehouseDocumentType warehouseDocumentType,
                                                    Warehouse warehouse,
                                                    Employee responsible,
                                                    CostCenter publicCostCenter,
                                                    String warehouseVoucherDescription,
                                                    List<SoldProduct> soldProducts)
            throws InventoryException, ProductItemNotFoundException {
        //Create the WarehouseVoucher
        WarehouseVoucher warehouseVoucher = new WarehouseVoucher();
        warehouseVoucher.setDocumentType(warehouseDocumentType);
        warehouseVoucher.setWarehouse(warehouse);
        warehouseVoucher.setDate(soldProductService.getDateFromSoldProductOrder(soldProducts.get(0)));
        //todo: cambiar el estado del vale
        warehouseVoucher.setState(WarehouseVoucherState.PEN);

        warehouseVoucher.setExecutorUnit(warehouse.getExecutorUnit());
        warehouseVoucher.setCostCenterCode(publicCostCenter.getId().getCode());
        warehouseVoucher.setResponsible(responsible);

        InventoryMovement inventoryMovement = new InventoryMovement();
        inventoryMovement.setDescription(warehouseVoucherDescription);

        warehouseService.createWarehouseVoucher(warehouseVoucher, inventoryMovement, null, null, null, null);

        //Create the MovementDetails
        for (SoldProduct soldProduct : soldProducts) {
            ProductItem productItem = getEntityManager()
                    .find(ProductItem.class, soldProduct.getProductItem().getId());

            MovementDetail movementDetailTemp = new MovementDetail();
            movementDetailTemp.setWarehouse(warehouse); //revisar
            movementDetailTemp.setProductItem(productItem);
            movementDetailTemp.setProductItemAccount(productItem.getProductItemAccount());
            movementDetailTemp.setQuantity(soldProduct.getQuantity());
            movementDetailTemp.setUnitCost(productItem.getUnitCost());
            movementDetailTemp.setAmount(null);
            movementDetailTemp.setExecutorUnit(warehouse.getExecutorUnit());
            movementDetailTemp.setCostCenterCode(publicCostCenter.getId().getCode());
            movementDetailTemp.setMeasureUnit(productItem.getUsageMeasureUnit());

            /* revisar */
            Map<MovementDetail, BigDecimal> movementDetailUnderMinimalStockMap = new HashMap<MovementDetail, BigDecimal>();
            movementDetailUnderMinimalStockMap.put(movementDetailTemp, productItem.getMinimalStock());

            Map<MovementDetail, BigDecimal> movementDetailOverMaximumStockMap = new HashMap<MovementDetail, BigDecimal>();
            movementDetailOverMaximumStockMap.put(movementDetailTemp, productItem.getMaximumStock());

            List<MovementDetail> movementDetailWithoutWarnings = new ArrayList<MovementDetail>();
            movementDetailWithoutWarnings.add(movementDetailTemp);
            /* revisar */

            try {
                //warehouseService.createMovementDetail(warehouseVoucher, movementDetailTemp, null, null, null); // revisar
                warehouseService.createMovementDetail(warehouseVoucher, movementDetailTemp, movementDetailUnderMinimalStockMap, movementDetailOverMaximumStockMap, movementDetailWithoutWarnings);
            } catch (WarehouseVoucherApprovedException e) {
                log.debug("This exception never happen because I just created a new WarehouseVoucher" +
                        " and his state is pending");
            } catch (WarehouseVoucherNotFoundException e) {
                log.debug("This exception never happen because I just created a new WarehouseVoucher");
            }
        }

        return warehouseVoucher;
    }

    private WarehouseVoucher createWarehouseVoucherAll(WarehouseDocumentType warehouseDocumentType,
                                                    Warehouse warehouse,
                                                    Employee responsible,
                                                    CostCenter publicCostCenter,
                                                    String warehouseVoucherDescription,
                                                    List<SoldProduct> soldProducts)
            throws InventoryException, ProductItemNotFoundException {
        //Create the WarehouseVoucher
        WarehouseVoucher warehouseVoucher = new WarehouseVoucher();
        warehouseVoucher.setDocumentType(warehouseDocumentType);
        warehouseVoucher.setWarehouse(warehouse);
        warehouseVoucher.setDate(soldProductService.getDateFromSoldProductOrder(soldProducts.get(0)));
        //todo: cambiar el estado del vale
        warehouseVoucher.setState(WarehouseVoucherState.PEN);

        warehouseVoucher.setExecutorUnit(warehouse.getExecutorUnit());
        warehouseVoucher.setCostCenterCode(publicCostCenter.getId().getCode());
        warehouseVoucher.setResponsible(responsible);

        InventoryMovement inventoryMovement = new InventoryMovement();
        inventoryMovement.setDescription(warehouseVoucherDescription);

        warehouseService.createWarehouseVoucher(warehouseVoucher, inventoryMovement, null, null, null, null);

        //Create the MovementDetails
        for (SoldProduct soldProduct : soldProducts) {
            ProductItem productItem = getEntityManager()
                    .find(ProductItem.class, soldProduct.getProductItem().getId());

            MovementDetail movementDetailTemp = new MovementDetail();
            movementDetailTemp.setWarehouse(warehouse); //revisar
            movementDetailTemp.setProductItem(productItem);
            movementDetailTemp.setProductItemAccount(productItem.getProductItemAccount());
            movementDetailTemp.setQuantity(soldProduct.getQuantity());
            movementDetailTemp.setUnitCost(productItem.getUnitCost());
            movementDetailTemp.setAmount(null);
            movementDetailTemp.setExecutorUnit(warehouse.getExecutorUnit());
            movementDetailTemp.setCostCenterCode(publicCostCenter.getId().getCode());
            movementDetailTemp.setMeasureUnit(productItem.getUsageMeasureUnit());

            /* revisar */
            Map<MovementDetail, BigDecimal> movementDetailUnderMinimalStockMap = new HashMap<MovementDetail, BigDecimal>();
            movementDetailUnderMinimalStockMap.put(movementDetailTemp, productItem.getMinimalStock());

            Map<MovementDetail, BigDecimal> movementDetailOverMaximumStockMap = new HashMap<MovementDetail, BigDecimal>();
            movementDetailOverMaximumStockMap.put(movementDetailTemp, productItem.getMaximumStock());

            List<MovementDetail> movementDetailWithoutWarnings = new ArrayList<MovementDetail>();
            movementDetailWithoutWarnings.add(movementDetailTemp);
            /* revisar */

            try {
                //warehouseService.createMovementDetail(warehouseVoucher, movementDetailTemp, null, null, null); // revisar
                warehouseService.createMovementDetail(warehouseVoucher, movementDetailTemp, movementDetailUnderMinimalStockMap, movementDetailOverMaximumStockMap, movementDetailWithoutWarnings);
            } catch (WarehouseVoucherApprovedException e) {
                log.debug("This exception never happen because I just created a new WarehouseVoucher" +
                        " and his state is pending");
            } catch (WarehouseVoucherNotFoundException e) {
                log.debug("This exception never happen because I just created a new WarehouseVoucher");
            }
        }

        return warehouseVoucher;
    }


    @SuppressWarnings(value = "unchecked")
    private void soldProductStateChecker(String invoiceNumber, String companyNumber) throws SoldProductDeliveredException {
        List<SoldProduct> soldProducts = listEm.createNamedQuery("SoldProduct.findByState")
                .setParameter("invoiceNumber", invoiceNumber)
                .setParameter("companyNumber", companyNumber)
                .setParameter("state", SoldProductState.DELIVERED).getResultList();
        if (null != soldProducts && !soldProducts.isEmpty()) {
            throw new SoldProductDeliveredException("The soldProduct was delivered by other user");
        }
    }

    @SuppressWarnings(value = "unchecked")
    private void productItemStockChecker(String invoiceNumber,
                                         Warehouse warehouse,
                                         CostCenter costCenter) throws InventoryException {
        List<ProductItem> productItems = getEntityManager()
                .createNamedQuery("SoldProduct.findByProductItem")
                .setParameter("invoiceNumber", invoiceNumber)
                .setParameter("companyNumber", warehouse.getId().getCompanyNumber())
                .getResultList();

        List<InventoryMessage> errorMessages = new ArrayList<InventoryMessage>();
        for (ProductItem productItem : productItems) {
            BigDecimal total = (BigDecimal) getEntityManager()
                    .createNamedQuery("SoldProduct.sunQuantitiesByProductItem")
                    .setParameter("invoiceNumber", invoiceNumber)
                    .setParameter("productItem", productItem)
                    .setParameter("companyNumber", warehouse.getId().getCompanyNumber()).getSingleResult();

            try {
                approvalWarehouseVoucherService.validateOutputQuantity(total,
                        warehouse,
                        productItem,
                        costCenter);
            } catch (InventoryException e) {
                errorMessages.addAll(e.getInventoryMessages());
            }
        }

        if (!errorMessages.isEmpty()) {
            throw new InventoryException(errorMessages);
        }
    }

    @SuppressWarnings(value = "unchecked")
    private void productItemStockCheckerWithoutCutCheese(String invoiceNumber,
                                         Warehouse warehouse,
                                         CostCenter costCenter) throws InventoryException {
        List<ProductItem> productItems = getEntityManager()
                .createNamedQuery("SoldProduct.findByProductItemWithouCutCheese")
                .setParameter("invoiceNumber", invoiceNumber)
                .setParameter("companyNumber", warehouse.getId().getCompanyNumber())
                .setParameter("codCutCheese",Constants.COD_CUT_CHEESE)
                .setParameter("codCheeseEDAM",Constants.COD_CHEESE_EDAM)
                .getResultList();

        List<InventoryMessage> errorMessages = new ArrayList<InventoryMessage>();
        for (ProductItem productItem : productItems) {
            BigDecimal total = (BigDecimal) getEntityManager()
                    .createNamedQuery("SoldProduct.sunQuantitiesByProductItem")
                    .setParameter("invoiceNumber", invoiceNumber)
                    .setParameter("productItem", productItem)
                    .setParameter("companyNumber", warehouse.getId().getCompanyNumber()).getSingleResult();

            try {
                approvalWarehouseVoucherService.validateOutputQuantity(total,
                        warehouse,
                        productItem,
                        costCenter);
            } catch (InventoryException e) {
                errorMessages.addAll(e.getInventoryMessages());
            }
        }

        if (!errorMessages.isEmpty()) {
            throw new InventoryException(errorMessages);
        }
    }

    @SuppressWarnings(value = "unchecked")
    private WarehouseDocumentType getFirstConsumptionType() {
        List<WarehouseDocumentType> warehouseDocumentTypeList = getEntityManager()
                .createNamedQuery("WarehouseDocumentType.findByType")
                .setParameter("companyNumber", Constants.defaultCompanyNumber)
                .setParameter("warehouseVoucherType", WarehouseVoucherType.C).getResultList();

        if (!ValidatorUtil.isEmptyOrNull(warehouseDocumentTypeList)) {
            return warehouseDocumentTypeList.get(0);
        }

        return null;
    }

    @SuppressWarnings(value = "unchecked")
    private CostCenter findPublicCostCenter(Warehouse warehouse) {
        List<InventoryDetail> inventoryDetails = getEntityManager()
                .createNamedQuery("InventoryDetail.findByWarehouseCode")
                .setParameter("warehouseCode", warehouse.getId().getWarehouseCode())
                .setParameter("executorUnit", warehouse.getExecutorUnit())
                .setParameter("companyNumber", warehouse.getId().getCompanyNumber()).getResultList();

        for (InventoryDetail inventoryDetail : inventoryDetails) {
            if (isPublicCostCenter(inventoryDetail.getCostCenterCode(), inventoryDetail.getCompanyNumber())) {
                return getEntityManager().find(CostCenter.class, inventoryDetail.getCostCenter().getId());
            }
        }

        return null;
    }

    private boolean isPublicCostCenter(String costCenterCode, String companyNumber) {
        CostCenterPk costCenterPk = new CostCenterPk(companyNumber, costCenterCode);
        CostCenter costCenter = getEntityManager().find(CostCenter.class, costCenterPk);
        return !costCenter.getExclusiveConsumption();
    }

    private String[] getGlossMessage(WarehouseVoucher warehouseVoucher, String movementDescription) {
        String gloss[] = new String[2];
        String dateString = DateUtils.format(warehouseVoucher.getDate(), MessageUtils.getMessage("patterns.dateTime"));
        String productCodes = QueryUtils.toQueryParameter(movementDetailService.findDetailProductCodeByVoucher(warehouseVoucher));
        String voucherTypeName = MessageUtils.getMessage(warehouseVoucher.getDocumentType().getWarehouseVoucherType().getResourceKey());
        String documentName = warehouseVoucher.getDocumentType().getName();
        String sourceWarehouseName = warehouseVoucher.getWarehouse().getName();
        gloss[0] = MessageUtils.getMessage("WarehouseVoucher.message.gloss", voucherTypeName, documentName, sourceWarehouseName, productCodes, dateString, Constants.WAREHOUSEVOUCHER_NUMBER_PARAM, movementDescription);
        System.out.println(warehouseVoucher.getNumber() + "----->>>>>>>>>>> gloss: " + gloss[0]);
        return gloss;

    }
}