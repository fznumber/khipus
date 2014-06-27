package com.encens.khipus.action.warehouse;

import com.encens.khipus.exception.ConcurrencyException;
import com.encens.khipus.exception.EntryDuplicatedException;
import com.encens.khipus.exception.ReferentialIntegrityException;
import com.encens.khipus.exception.finances.CompanyConfigurationNotFoundException;
import com.encens.khipus.exception.finances.FinancesCurrencyNotFoundException;
import com.encens.khipus.exception.finances.FinancesExchangeRateNotFoundException;
import com.encens.khipus.exception.warehouse.*;
import com.encens.khipus.framework.action.GenericAction;
import com.encens.khipus.framework.action.Outcome;
import com.encens.khipus.model.employees.Employee;
import com.encens.khipus.model.warehouse.*;
import com.encens.khipus.service.customers.AccountItemService;
import com.encens.khipus.service.customers.AccountItemServiceBean;
import com.encens.khipus.service.customers.OrderClient;
import com.encens.khipus.service.warehouse.ProductDeliveryService;
import com.encens.khipus.service.warehouse.SoldProductService;
import com.encens.khipus.util.Constants;
import com.encens.khipus.util.MessageUtils;
import com.encens.khipus.util.ValidatorUtil;
import com.encens.khipus.util.warehouse.InventoryMessage;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.*;
import org.jboss.seam.annotations.security.Restrict;
import org.jboss.seam.international.StatusMessage;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author
 * @version 3.0
 */
@Name("productDeliveryAction")
@Scope(ScopeType.CONVERSATION)
public class ProductDeliveryAction extends GenericAction<ProductDelivery> {

    private List<SoldProduct> soldProducts = new ArrayList<SoldProduct>();

    @In
    private SoldProductService soldProductService;

    @In
    private ProductDeliveryService productDeliveryService;

    @In
    private AccountItemService accountItemService;

    private String orderNumber;
    private String messageSearchOrder;

    private ProductDeliveryType productDeliveryType = ProductDeliveryType.CASH_ORDER;

    private Boolean showDeliveryOrder = true;
    private Date date;
    private List<OrderClient> orderClients = new ArrayList<OrderClient>();
    private List<AccountItemServiceBean.OrderItem> orderItems = new ArrayList<AccountItemServiceBean.OrderItem>();
    private List<String> numberInvoices = new ArrayList<String>();
    private List<BigDecimal> distributors = new ArrayList<BigDecimal>();
    private String product;
    private Employee distributor;

    @Factory(value = "productDelivery", scope = ScopeType.STATELESS)
    @Restrict("#{s:hasPermission('PRODUCTDELIVERY','VIEW')}")
    public ProductDelivery initProductDelivery() {
        return getInstance();
    }

    @Override
    @End
    @Restrict("#{s:hasPermission('PRODUCTDELIVERY','CREATE')}")
    public String create() {

        if(!showDeliveryOrder)
        return deliveryInCatch();
        else
        return deliveryOrder();

    }

    private String deliveryOrder() {
        if(productDeliveryService.verifyAmounts(numberInvoices))
          return Outcome.REDISPLAY;
        else
        {
            if(verifyWasDelivery())
            {
                return Outcome.REDISPLAY;
            }

            try {
                productDeliveryService.deliveryAll(numberInvoices);
                addAllDeliveryMessage();
                return Outcome.SUCCESS;
            } catch (InventoryException e) {
                addInventoryErrorMessages(e.getInventoryMessages());
                return Outcome.FAIL;
            } catch (ProductItemNotFoundException e) {
                addProductItemNotFoundMessage(e.getProductItem().getFullName());
                return Outcome.FAIL;
            } catch (ProductItemAmountException e) {
                addNotEnoughAmountMessage(e.getProductItem(), e.getAvailableAmount());
                return Outcome.FAIL;
            } catch (CompanyConfigurationNotFoundException e) {
                addCompanyConfigurationNotFoundErrorMessage();
                return Outcome.FAIL;
            } catch (FinancesExchangeRateNotFoundException e) {
                addFinancesExchangeRateNotFoundExceptionMessage();
                return Outcome.FAIL;
            } catch (FinancesCurrencyNotFoundException e) {
                addFinancesExchangeRateNotFoundExceptionMessage();
                return Outcome.FAIL;
            } catch (InventoryProductItemNotFoundException e) {
                addInventoryProductItemNotFoundErrorMessage(e.getExecutorUnitCode(),
                        e.getProductItem(), e.getWarehouse());
                return Outcome.FAIL;
            } catch (ReferentialIntegrityException e) {
                addDeleteReferentialIntegrityMessage();
                return Outcome.FAIL;
            } catch (ConcurrencyException e) {
                addUpdateConcurrencyMessage();
                return Outcome.FAIL;
            } catch (InventoryUnitaryBalanceException e) {
                addInventoryUnitaryBalanceErrorMessage(e.getAvailableUnitaryBalance(), e.getProductItem());
                return Outcome.FAIL;
            } catch (EntryDuplicatedException e) {
                addDuplicatedMessage();
                return Outcome.FAIL;
            }

        }
    }

    private String deliveryInCatch()
    {
        if (ValidatorUtil.isBlankOrNull(getInstance().getInvoiceNumber())) {
            addInvoiceNumberRequiredMessage();
            return Outcome.REDISPLAY;
        }
        try {
            ProductDelivery productDelivery = productDeliveryService.createAll(getInstance().getInvoiceNumber(),
                    MessageUtils.getMessage("ProductDelivery.warehouseVoucher.description", getInstance().getInvoiceNumber()));
            addSoldProductDeliveredInfoMessage();
            select(productDelivery);
            return Outcome.SUCCESS;
        } catch (SoldProductNotFoundException e) {
            addSoldProductNotFoundMessages();
            return Outcome.FAIL;
        } catch (InventoryException e) {
            addInventoryErrorMessages(e.getInventoryMessages());
            return Outcome.FAIL;
        } catch (PublicCostCenterNotFound publicCostCenterNotFound) {
            return Outcome.FAIL;
        } catch (WarehouseDocumentTypeNotFoundException e) {
            addWarehouseDocumentTypeErrorMessage();
            return Outcome.FAIL;
        } catch (ProductItemAmountException e) {
            addNotEnoughAmountMessage(e.getProductItem(), e.getAvailableAmount());
            return Outcome.FAIL;
        } catch (InventoryUnitaryBalanceException e) {
            addInventoryUnitaryBalanceErrorMessage(e.getAvailableUnitaryBalance(), e.getProductItem());
            return Outcome.FAIL;
        } catch (InventoryProductItemNotFoundException e) {
            addInventoryProductItemNotFoundErrorMessage(e.getExecutorUnitCode(),
                    e.getProductItem(), e.getWarehouse());
            return Outcome.FAIL;
        } catch (SoldProductDeliveredException e) {
            addSoldProductDeliveredErrorMessage();
            return Outcome.FAIL;
        } catch (CompanyConfigurationNotFoundException e) {
            addCompanyConfigurationNotFoundErrorMessage();
            return Outcome.FAIL;
        } catch (FinancesExchangeRateNotFoundException e) {
            addFinancesExchangeRateNotFoundExceptionMessage();
            return Outcome.FAIL;
        } catch (FinancesCurrencyNotFoundException e) {
            addFinancesExchangeRateNotFoundExceptionMessage();
            return Outcome.FAIL;
        } catch (ConcurrencyException e) {
            addUpdateConcurrencyMessage();
            return Outcome.FAIL;
        } catch (EntryDuplicatedException e) {
            addDuplicatedMessage();
            return Outcome.FAIL;
        } catch (ReferentialIntegrityException e) {
            addDeleteReferentialIntegrityMessage();
            return Outcome.FAIL;
        } catch (ProductItemNotFoundException e) {
            addProductItemNotFoundMessage(e.getProductItem().getFullName());
            return Outcome.FAIL;
        }
    }


    public void myCreate() {

        String numbers[] = {
                "26004386"
        };
        for(String number :numbers)
        {

            try {
                //for()
                System.out.println("NUMERO DE FACTURA -> "+number);
                ProductDelivery productDelivery = productDeliveryService.createAll(number,
                        MessageUtils.getMessage("ProductDelivery.warehouseVoucher.description", number));
                addSoldProductDeliveredInfoMessage();
                select(productDelivery);
                //update();
            } catch (SoldProductNotFoundException e) {
                addSoldProductNotFoundMessages();
                continue;
            } catch (InventoryException e) {
                addInventoryErrorMessages(e.getInventoryMessages());
                continue;
            } catch (PublicCostCenterNotFound publicCostCenterNotFound) {
                continue;
            } catch (WarehouseDocumentTypeNotFoundException e) {
                addWarehouseDocumentTypeErrorMessage();
                continue;
            } catch (ProductItemAmountException e) {
                addNotEnoughAmountMessage(e.getProductItem(), e.getAvailableAmount());
                continue;
            } catch (InventoryUnitaryBalanceException e) {
                addInventoryUnitaryBalanceErrorMessage(e.getAvailableUnitaryBalance(), e.getProductItem());
                continue;
            } catch (InventoryProductItemNotFoundException e) {
                addInventoryProductItemNotFoundErrorMessage(e.getExecutorUnitCode(),
                        e.getProductItem(), e.getWarehouse());
                continue;
            } catch (SoldProductDeliveredException e) {
                addSoldProductDeliveredErrorMessage();
                continue;
            } catch (CompanyConfigurationNotFoundException e) {
                addCompanyConfigurationNotFoundErrorMessage();
                continue;
            } catch (FinancesExchangeRateNotFoundException e) {
                addFinancesExchangeRateNotFoundExceptionMessage();
                continue;
            } catch (FinancesCurrencyNotFoundException e) {
                addFinancesExchangeRateNotFoundExceptionMessage();
                continue;
            } catch (ConcurrencyException e) {
                addUpdateConcurrencyMessage();
                continue;
            } catch (EntryDuplicatedException e) {
                addDuplicatedMessage();
                continue;
            } catch (ReferentialIntegrityException e) {
                addDeleteReferentialIntegrityMessage();
                continue;
            } catch (ProductItemNotFoundException e) {
                addProductItemNotFoundMessage(e.getProductItem().getFullName());
                continue;
            }
        }
    }

    @Override
    @Begin(ifOutcome = Outcome.SUCCESS, flushMode = FlushModeType.MANUAL)
    @Restrict("#{s:hasPermission('PRODUCTDELIVERY','VIEW')}")
    public String select(ProductDelivery instance) {
        setOp(OP_UPDATE);
        setInstance(productDeliveryService.select(instance));
        readSoldProducts();
        return Outcome.SUCCESS;
    }

    public void search() {
        if (productDeliveryType.equals(ProductDeliveryType.CASH_SALE))
            searchCashSale();
        if (productDeliveryType.equals(ProductDeliveryType.CASH_ORDER))
            searchCashOrder();


    }

    private void searchCashSale() {

        List<SoldProduct> soldProductList = soldProductService.getSoldProductsCashSale(orderNumber, Constants.defaultCompanyNumber);
        if (ValidatorUtil.isEmptyOrNull(soldProductList)) {
            setMessageSearchOrder(MessageUtils.getMessage("ProductDelivery.messageSearchOrderNotFound"));
            getInstance().setInvoiceNumber(null);
            soldProducts.clear();
        } else {
            setMessageSearchOrder(null);
            if (soldProductList.get(0).getState().equals(SoldProductState.DELIVERED)) {
                setMessageSearchOrder(MessageUtils.getMessage("ProductDelivery.messageSearchOrderDelivered"));
                assignNumberCashSale(soldProductList.get(0));
            } else
                assignNumberCashSale(soldProductList.get(0));
        }
    }

    private void searchCashOrder() {
        distributors = accountItemService.findDistributor(date);
        orderClients.clear();
        orderItems.clear();
        numberInvoices.clear();
        setMessageSearchOrder("");
        if(distributor != null)
        {
            orderClients.addAll(accountItemService.findClientsOrder(new BigDecimal(distributor.getId()),date));
            OrderClient distributorNew = new OrderClient();
            distributorNew.setName(accountItemService.getNameEmployeed(new BigDecimal(distributor.getId())));
            distributorNew.setIdDistributor(new BigDecimal(distributor.getId()));
            distributorNew.setType("DISTRIBUTOR");
            orderClients.add(distributorNew);
        }
        else
        for(BigDecimal idDistributor:distributors)
        {
            orderClients.addAll(accountItemService.findClientsOrder(idDistributor,date));
            OrderClient distributor = new OrderClient();
            distributor.setName(accountItemService.getNameEmployeed(idDistributor));
            distributor.setIdDistributor(idDistributor);
            distributor.setType("DISTRIBUTOR");
            orderClients.add(distributor);
        }

        orderItems = accountItemService.findOrderItemByState(date);
        orderItems.addAll(accountItemService.findOrderItemPackByState(date));
        if(distributor != null)
            numberInvoices = soldProductService.getSoldProductsCashOrder(date,new BigDecimal(distributor.getId()));
        else
            numberInvoices = soldProductService.getSoldProductsCashOrder(date);

        if(wasDelivery())
            setMessageSearchOrder(MessageUtils.getMessage("ProductDelivery.messageWasDelivery"));
        else
        if (ValidatorUtil.isEmptyOrNull(orderClients)) {
            setMessageSearchOrder(MessageUtils.getMessage("ProductDelivery.messageSearchOrderDateNotFound"));
            getInstance().setInvoiceNumber(null);
        }
    }

    private boolean verifyWasDelivery() {
        Boolean result = false;
        for(OrderClient orderClient: orderClients)
        {
            if(orderClient.getState()!= null)
                if(orderClient.getState().equals("ECH")) {
                    setMessageSearchOrder(MessageUtils.getMessage("ProductDelivery.messageWasDeliveryOrder",orderClient.getIdOrder()));
                    result = true;
                }
        }
        return result;
    }

    private boolean wasDelivery() {
        for(OrderClient orderClient: orderClients)
        {
            if(orderClient.getState()!= null)
            if(orderClient.getState().equals("ECH")) {
                return true;
            }
        }
        return false;
    }


    @Factory(value = "productDeliveryTypes", scope = ScopeType.STATELESS)
    public ProductDeliveryType[] initProductDeliveryTypes() {
        return ProductDeliveryType.values();
    }

    public List<SoldProduct> getSoldProducts() {
        return soldProducts;
    }

    public void setSoldProducts(List<SoldProduct> soldProducts) {
        this.soldProducts = soldProducts;
    }

    public void readSoldProducts() {
        setSoldProducts(
                soldProductService.getSoldProducts(getInstance().getInvoiceNumber(), Constants.defaultCompanyNumber));
    }

    public void readSoldProductsCashSale() {
        setSoldProducts(
                soldProductService.getSoldProductsCashSale(getInstance().getInvoiceNumber(), Constants.defaultCompanyNumber));
    }

    public void readSoldProductsCashOrder() {
        setSoldProducts(
                soldProductService.getSoldProductsCashOrder(getInstance().getInvoiceNumber(), Constants.defaultCompanyNumber));
    }

    public boolean isExistsSoldProducts() {
        return !soldProducts.isEmpty();
    }

    public SoldProduct getSoldProduct() {
        return soldProducts.get(0);
    }


    public void assignInvoiceNumber(SoldProduct soldProduct) {
        getInstance().setInvoiceNumber(soldProduct.getInvoiceNumber());
        readSoldProducts();
    }

    public void assignNumberCashSale(SoldProduct soldProduct) {
        getInstance().setInvoiceNumber(soldProduct.getInvoiceNumber());
        readSoldProductsCashSale();
    }

    public void assignNumberCashOrder(SoldProduct soldProduct) {
        getInstance().setInvoiceNumber(soldProduct.getInvoiceNumber());
        readSoldProductsCashOrder();
    }

    public void cleanInvoiceNumber() {
        getInstance().setInvoiceNumber(null);
        soldProducts.clear();
        setOrderNumber(null);
        setMessageSearchOrder(null);
        orderItems.clear();
        orderClients.clear();
        numberInvoices.clear();
        date = null;
        distributor = null;
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

    private void addInventoryUnitaryBalanceErrorMessage(BigDecimal availableUnitaryBalance,
                                                        ProductItem productItem) {
        facesMessages.addFromResourceBundle(StatusMessage.Severity.ERROR,
                "ProductDelivery.error.notEnoughUnitaryBalance",
                getInstance().getInvoiceNumber(),
                productItem.getName(),
                availableUnitaryBalance);
    }

    private void addNotEnoughAmountMessage(ProductItem productItem,
                                           BigDecimal availableAmount) {
        facesMessages.addFromResourceBundle(StatusMessage.Severity.ERROR,
                "ProductDelivery.error.notEnoughAmount",
                getInstance().getInvoiceNumber(),
                productItem.getName(),
                availableAmount);
    }

    private void addInventoryProductItemNotFoundErrorMessage(String executorUnitCode,
                                                             ProductItem productItem,
                                                             Warehouse warehouse) {
        facesMessages.addFromResourceBundle(StatusMessage.Severity.WARN,
                "ProductDelivery.error.productItemNotFound",
                getInstance().getInvoiceNumber(),
                productItem.getName(),
                warehouse.getName(),
                executorUnitCode);
    }

    private void addAllDeliveryMessage() {
        facesMessages.addFromResourceBundle(StatusMessage.Severity.INFO,
                "ProductDelivery.allDelive.description",date);
    }

    private void addWarehouseDocumentTypeErrorMessage() {
        facesMessages.addFromResourceBundle(StatusMessage.Severity.ERROR,
                "ProductDelivery.error.consumptionDocumentTypeNotFound", getInstance().getInvoiceNumber());
    }

    private void addSoldProductDeliveredErrorMessage() {
        facesMessages.addFromResourceBundle(StatusMessage.Severity.ERROR,
                "ProductDelivery.error.soldProductDelivered", getInstance().getInvoiceNumber());
    }

    private void addSoldProductDeliveredInfoMessage() {
        facesMessages.addFromResourceBundle(StatusMessage.Severity.INFO,
                "ProductDelivery.info.soldProductDelivered", getInstance().getInvoiceNumber());
    }

    private void addFinancesExchangeRateNotFoundExceptionMessage() {
        facesMessages.addFromResourceBundle(StatusMessage.Severity.ERROR,
                "PurchaseOrder.financesExchangeRateNotFound");
    }

    private void addInvoiceNumberRequiredMessage() {
        facesMessages.addFromResourceBundle(StatusMessage.Severity.ERROR,
                "Common.required", messages.get("ProductDelivery.invoiceNumber"));
    }

    public void addProductItemNotFoundMessage(String productItemName) {
        facesMessages.addFromResourceBundle(StatusMessage.Severity.WARN,
                "ProductItem.error.notFound", productItemName);
    }

    private void addSoldProductNotFoundMessages() {
        facesMessages.addFromResourceBundle(StatusMessage.Severity.WARN,
                "ProductDelivery.info.soldProductNotFoundMessages", getInstance().getInvoiceNumber());
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public String getMessageSearchOrder() {
        return messageSearchOrder;
    }

    public void setMessageSearchOrder(String messageSearchOrder) {
        this.messageSearchOrder = messageSearchOrder;
    }

    public ProductDeliveryType getProductDeliveryType() {
        return productDeliveryType;
    }

    public void setProductDeliveryType(ProductDeliveryType productDeliveryType) {
        this.productDeliveryType = productDeliveryType;
        showDeliveryOrder = productDeliveryType.getResourceKey().compareTo("ProductDeliveryType.cashOrder") == 0 ;
    }

    public Boolean getShowDeliveryOrder() {
        return showDeliveryOrder;
    }

    public void setShowDeliveryOrder(Boolean showDeliveryOrder) {
        this.showDeliveryOrder = showDeliveryOrder;
    }

    public String getProduct() {
        return product;
    }

    public List<OrderClient> getOrderClients() {
        return orderClients;
    }

    public void setOrderClients(List<OrderClient> orderClients) {
        this.orderClients = orderClients;
    }

    public List<AccountItemServiceBean.OrderItem> getOrderItems() {
        return orderItems;
    }

    public void setOrderItems(List<AccountItemServiceBean.OrderItem> orderItems) {
        this.orderItems = orderItems;
    }

    public Integer getAmountSoldProduct(OrderClient client,AccountItemServiceBean.OrderItem item){
        Integer val;
        if(item.getType() == "COMBO" )
            if(client.getType() != null) {
                val = accountItemService.getAmountCombo(item.getCodArt(), client.getIdDistributor(), date);
            }
            else
            val = accountItemService.getAmountCombo(item.getCodArt(),client.getIdOrder());
        else
            if(client.getType() != null) {
                val = accountItemService.getAmountByDateAndDistributorOrder(item.getCodArt(),client.getIdDistributor(),date);
                val += accountItemService.getAmountByDateAndDistributorInstitution(item.getCodArt(), client.getIdDistributor(), date);
            }
            else
            val = accountItemService.getAmount(item.getCodArt(),client.getIdOrder());

        return val;
    }

    public Integer getAmountSoldProductTotal(AccountItemServiceBean.OrderItem item){
        Integer val;

            val = accountItemService.getAmountByDateAndDistributorOrder(item.getCodArt(),date);
            val += accountItemService.getAmountByDateAndDistributorInstitution(item.getCodArt(), date);
            val += accountItemService.getAmountComboTotal(item.getCodArt(), date);
        return val;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Employee getDistributor() {
        return distributor;
    }

    public void setDistributor(Employee distributor) {
        this.distributor = distributor;
    }

    public void cleanDistributor(){
        setDistributor(null);
    }
}
