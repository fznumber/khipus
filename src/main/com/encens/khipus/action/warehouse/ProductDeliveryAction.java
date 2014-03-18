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
import com.encens.khipus.model.warehouse.*;
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

    private String orderNumber;
    private String messageSearchOrder;

    private ProductDeliveryType productDeliveryType = ProductDeliveryType.CASH_ORDER;

    @Factory(value = "productDelivery", scope = ScopeType.STATELESS)
    @Restrict("#{s:hasPermission('PRODUCTDELIVERY','VIEW')}")
    public ProductDelivery initProductDelivery() {
        return getInstance();
    }

    @Override
    @End
    @Restrict("#{s:hasPermission('PRODUCTDELIVERY','CREATE')}")
    public String create() {
        System.out.println("...Entregar pedido.... " + getInstance().getInvoiceNumber());
        System.out.println("......ValidatorUtil.isBlankOrNull.... " + ValidatorUtil.isBlankOrNull(getInstance().getInvoiceNumber()));

        if (ValidatorUtil.isBlankOrNull(getInstance().getInvoiceNumber())) {
            addInvoiceNumberRequiredMessage();
            System.out.println("....Outcome.REDISPLAY: " + Outcome.REDISPLAY);
            return Outcome.REDISPLAY;
        }
        try {
            ProductDelivery productDelivery = productDeliveryService.create(getInstance().getInvoiceNumber(),
                    MessageUtils.getMessage("ProductDelivery.warehouseVoucher.description", getInstance().getInvoiceNumber()));
            addSoldProductDeliveredInfoMessage();
            select(productDelivery);
            return Outcome.SUCCESS;
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
                /*"100",
                "101",
                "103",
                "104",
                "105",
                "106",
                "109",
                "110",
                "1101",
                "1102",
                "1103",
                "1104",
                "1105",
                "1106",
                "1107",
                "1108",
                "1109",
                "111",
                "112",
                "113",
                "114",
                "115",
                "116",
                "117",
                "118",
                "119",
                "120",
                "121",
                "122",
                "127",
                "128",
                "129",
                "130"
                "131",
                "132",
                "133",
                "134",
                "135",
                "136",
                "137",
                "138",
                "139",
                "140",
                "141",
                "142",
                "143",
                "144",
                "145",
                "1458",
                "146",
                "147",
                "148",
                "149",
                "150",
                "151",
                "152",
                "153",
                "154",
                "155",
                "156",
                "157",
                "158",
                "159",
                "160",
                "161",
                "162",
                "163",
                "165",
                "166",
                "167",
                "168",
                "169",
                "170",
                "171",
                "172",
                "173",
                "174",
                "175",
                "176",
                "177",
                "1773",
                "1774",
                "178",
                "179",
                "180",
                "181",
                "182",
                "183",
                "184",
                "185",
                "186",
                "187",
                "188",
                "189",
                "190",
                "191",
                "195",
                "198",
                "199",
                "200",
                "201",
                "202",
                "203",
                "204",
                "206",
                "207",
                "208",
                "209",
                "210",
                "211",
                "212",
                "214",
                "215",
                "236",
                "237",
                "238",
                "239",
                "240",
                "25",
                "251",
                "252",
                "253",
                "254",
                "255",
                "256",
                "257",
                "258",
                "259",
                "26",
                "260",
                "261",
                "263",
                "264",
                "266",
                "267",
                "268",
                "27",
                "270",
                "271",
                "272",
                "273",
                "274",
                "275",
                "276",
                "277",
                "28",
                "280",
                "281",
                "282",
                "283",
                "284",
                "285",
                "286",
                "287",
                "288",
                "289",
                "29",
                "290",
                "291",
                "292",
                "293",
                "294",
                "295",
                "296",
                "297",
                "298",
                "299",
                "30",
                "300",
                "301",
                "302",
                "303",
                "304",
                "306",
                "307",
                "308",
                "309",
                "31",
                "310",
                "311",
                "312",
                "313",
                "314",
                "315",
                "316",
                "317",
                "318",
                "319",
                "32",
                "320",
                "321",
                "322",
                "323",
                "324",
                "325",
                "326",
                "327",
                "328",
                "329",
                "33",
                "330",
                "332",
                "34",
                "35",
                "354",
                "36",
                "37",
                "39",
                "40",//FALLO REVISADO
                "41",
                "42",
                "43",
                "44",
                "446",
                "45",
                "451",
                "46",
                "467",
                "48",//FALLO
                "49",
                "53",
                "54",
                "55",
                "56",
                "57",
                "58",
                "59",
                "60",
                "61",
                "612",
                "613",
                "614",
                "615",
                "617",
                "62",
                "63",
                "64",
                "65",
                "656",
                "66",
                "67",
                "68",
                "713",
                "714",
                "716",
                "717",
                "718",
                "72",
                "722",
                "723",
                "724",
                "725",
                "726",
                "727",
                "728",
                "729",
                "73",
                "730",
                "74",
                "75",
                "76",
                "765",
                "766",
                "767",
                "768",
                "769",
                "77",
                "78",
                "788",
                "789",
                "790",
                "791",
                "792",
                "793",
                "80",
                "81",
                "82",
                "83",
                "84",
                "85",
                "86",
                "866",
                "87",
                "88",
                "90",
                "91",
                "92",
                "93",
                "94",
                "95",
                "96",
                "97",
                "98",
                "99",*/
//contado
                /*"26000017",
                "26000018",
                "26000019",
                "26000020",
                "26000021",
                "26000023",
                "26000024",
                "26000025",
                "26000026",
                "26000027",
                "26000028",
                "26000029",
                "26000030",
                "26000049",//fallo
                "26000059",//fallo
                "26000060",
                "26000061",
                "26000062",
                "26000064",
                "26000065",
                "26000066",
                "26000067",
                "26000068",
                "26000069",
                "26000070",//fallo
                "26000071",
                "26000072",
                "26000080",
                "26000081",
                "26000083",
                "26000085",
                "26000086",
                "26000087",
                "26000088",
                "26000089",
                "26000090",
                "26000091",
                "26000092",
                "26000093",
                "26000094",
                "26000095",
                "26000096",
                "26000097",
                "26000098",
                "26000099",*/
                "26000100",
                "26000116",
                "26000117",
                "26000118",
                "26000119",
                "26000120",
                "26000121",
                "26000122",
                "26000123",
                "26000137",
                "26000138",
                "26000139",
                "26000140",
                "26000141",
                "26000142",
                "26000143",
                "26000144",
                "26000145",
                "26000146",//fallo
                "26000147",//fallo
                "26000148",
                "26000149",
                "26000164",
                "26000165",
                "26000166",
                "26000167",//fallo
                "26000168",
                "26000169",
                "26000170",
                "26000179",
                "26000183",
                "26000184",
                "26000185",
                "26000186",
                "26000187",
                "26000188",
                "26000189",
                "26000190",
                "26000194",
                "26000195",
                "26000196",
                "26000197",
                "26000198",
                "26000199",
                "26000200",
                "26000203",
                "26000204",
                "26000205",
                "26000208",
                "26000209",
                "26000214",
                "26000218",
                "26000219",
                "26000220",
                "26000221",
                "26000222",
                "26000223",
                "26000224",
                "26000225",
                "26000226",
                "26000227",
                "26000228",
                "26000229",
                "26000230",
                "26000231",
                "26000232",
                "26000233",
                "26000238",
                "26000239",
                "26000240",
                "26000241",
                "26000242",
                "26000243",//fallo
                "26000244",
                "26000245",
                "26000246",
                "26000247",
                "26000248",
                "26000249",
                "26000250",
                "26000264",
                "26000265",
                "26000266",
                "26000269",
                "26000270",
                "26000271",
                "26000272",
                "26000273",
                "26000274",
                "26000275",
                "26000276",
                "26000277",
                "26000278",
                "26000279",
                "26000280",
                "26000296",//fallo
                "26000297",
                "26000298"


        };
        for(String number :numbers)
        {

        try {
            //for()
            System.out.println("NUMERO DE FACTURA -> "+number);
            ProductDelivery productDelivery = productDeliveryService.create(number,
                    MessageUtils.getMessage("ProductDelivery.warehouseVoucher.description", number));
            addSoldProductDeliveredInfoMessage();
            select(productDelivery);
            //update();

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

        List<SoldProduct> soldProductList = soldProductService.getSoldProductsCashOrder(orderNumber, Constants.defaultCompanyNumber);
        if (ValidatorUtil.isEmptyOrNull(soldProductList)) {
            setMessageSearchOrder(MessageUtils.getMessage("ProductDelivery.messageSearchOrderNotFound"));
            getInstance().setInvoiceNumber(null);
            soldProducts.clear();
        } else {
            setMessageSearchOrder(null);
            if (soldProductList.get(0).getState().equals(SoldProductState.DELIVERED)) {
                setMessageSearchOrder(MessageUtils.getMessage("ProductDelivery.messageSearchOrderDelivered"));
                assignNumberCashOrder(soldProductList.get(0));
            } else
                assignNumberCashOrder(soldProductList.get(0));
        }
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
    //todo: esto es solo para prueba este metodo debe ser borrado luego
    //@End
    public void generateAll()
    {
        String numbers[] = {"67","73"};
        for(String number :numbers)
        {
            getInstance().setInvoiceNumber(number);
            myCreate();
        }
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
        System.out.println("___________SET ProductDeliveryType: " + this.productDeliveryType);
    }
}
