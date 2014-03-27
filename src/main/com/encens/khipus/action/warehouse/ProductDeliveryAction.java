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
/*                "1040",
                "1094",
                "1096",
                "1097",
                "1098",
                "1099",
                "1110",
                "1111",
                "1112",
                "333",
                "334",
                "335",
                "337",
                "338",
                "339",
                "340",
                "341",
                "342",
                "343",
                "344",
                "345",
                "346",
                "347",
                "348",
                "349",
                "350",
                "351",
                "352",
                "353",
                "355",
                "356",
                "357",
                "358",
                "359",
                "360",
                "361",
                "362",
                "363",
                "364",
                "365",
                "366",
                "367",
                "368",
                "369",
                "370",
                "371",
                "372",
                "377",
                "378",
                "379",
                "380",
                "381",
                "382",
                "383",
                "384",
                "385",
                "386",
                "387",
                "388",
                "389",
                "390",
                "391",
                "392",
                "394",
                "395",
                "396",
                "397",
                "398",
                "399",
                "401",
                "402",
                "403",
                "404",
                "405",
                "406",
                "407",
                "408",
                "411",
                "412",
                "413",
                "414",
                "415",
                "416",
                "417",
                "418",
                "419",
                "420",
                "421",
                "422",
                "423",
                "424",
                "425",
                "426",
                "427",
                "428",
                "429",
                "430",
                "431",
                "432",
                "433",
                "434",
                "435",
                "436",
                "437",
                "439",
                "440",
                "441",
                "442",
                "443",
                "444",
                "445",
                "447",
                "448",
                "449",
                "450",
                "452",
                "453",
                "454",
                "455",
                "458",
                "460",
                "461",
                "462",
                "463",
                "464",
                "465",
                "466",
                "468",
                "469",
                "470", //falla
                "471",
                "472",
                "473",
                "474",
                "475",
                "477",
                "478",
                "479",
                "480",
                "482",
                "483",
                "484",
                "485",
                "486",
                "487",
                "488",
                "489",
                "490",
                "492",
                "493",
                "494",
                "495",
                "496",
                "497",
                "498",
                "499",
                "500",
                "501",
                "502",
                "503",
                "504",
                "505",
                "506",
                "507",
                "508",
                "509",
                "510",
                "512",
                "513",
                "514",
                "515",
                "516",
                "517", //falla
                "518",
                "519",
                "521",
                "522",
                "523",
                "524",
                "525",//falla
                "526",
                "527",//falla
                "528",
                "529",
                "530",
                "531",
                "533",
                "534",
                "535",
                "536",
                "537",
                "538",
                "539",
                "540",
                "541",
                "542",//falla
                "543",
                "544",
                "545",
                "546",
                "547",
                "548",
                "549",
                "550",
                "551",
                "552",
                "553",
                "554",
                "555",
                "556",//falla
                "557",
                "558",
                "560",
                "561",
                "562",
                "564",
                "565",//falla
                "566",
                "567",
                "568",
                "569",
                "570",
                "571",
                "572",
                "573",
                "574",
                "575",
                "576",
                "577",
                "578",
                "579",
                "580",
                "582",//falla
                "583",
                "584",
                "585",
                "586",
                "588",
                "589",
                "590",
                "591",
                "592",
                "593",
                "594",
                "596",
                "597",
                "598",
                "599",
                "600",
                "601",
                "602",//falla
                "603",
                "604",
                "605",
                "606",
                "607",
                "608",//falla
                "609", //falla
                "610",//falla
                "611",//falla
                "618",//falla
                "619",//falla
                "620",
                "622",//falla
                "624",//falla
                "625",
                "626",
                "627",
                "628",
                "629",
                "630",
                "631",
                "632",
                "633",
                "634",
                "635",
                "636",//falla
                "637",//falla
                "638",
                "639",//falla
                "640",
                "641",
                "642",
                "643",//falla
                "644",//falla
                "645",
                "646",
                "647",//falla
                "648",
                "649",
                "650",//falla
                "651",//falla
                "652",
                "653",
                "654",
                "655",
                "657",
                "658",
                "659",
                "660",
                "661",
                "662",
                "663",
                "664",
                "665",
                "666",//falla
                "667",
                "668",
                "669",
                "670",
                "671",
                "672",
                "673",//falla
                "674",
                "675",
                "676",
                "677",
                "678",//falla
                "679",
                "680",
                "683",//falla
                "684",
                "685",
                "686",
                "687",
                "688",
                "689",
                "690",
                "692",//falla
                "693",
                "694",//falla
                "695",//falla
                "696",//falla
                "697",
                "698",
                "699",//falla
                "700",
                "701",
                "703",
                "704",//falla
                "705",//falla
                "706",
                "707",//falla
                "708",
                "709",//falla
                "710",//falla
                "711",//falla
                "731",
                "732",
                "733",
                "734",
                "759",
                "760",//falla
                "761",
                "762",
                "763",
                "764",
                "794",
                "795",
                "796",
                "797",
                "798",
                "799",
//al contado
                "26000299",
                "26000300",
                "26000301",
                "26000302",
                "26000303",
                "26000304",
                "26000305",
                "26000306",
                "26000307",
                "26000319",
                "26000320",
                "26000322",
                "26000323",
                "26000324",
                "26000325",
                "26000326",
                "26000335",//fallo
                "26000337",
                "26000339",
                "26000345",
                "26000346",
                "26000347",
                "26000348",
                "26000351",
                "26000352",
                "26000354",
                "26000355",
                "26000356",
                "26000357",
                "26000358",
                "26000359",
                "26000360",
                "26000361",
                "26000362",
                "26000366",
                "26000367",
                "26000378",
                "26000379",
                "26000380",//fallo
                "26000381",//fallo
                "26000382",
                "26000383",
                "26000384",//fallo
                "26000386",
                "26000387",
                "26000388",
                "26000390",
                "26000399",
                "26000400",
                "26000401",//fallo
                "26000402",
                "26000403",
                "26000404",
                "26000405",
                "26000406",
                "26000407",//fallo
                "26000408",
                "26000409",
                "26000410",
                "26000411",
                "26000412",
                "26000413",
                "26000414",
                "26000415",
                "26000437",
                "26000438",//fallo
                "26000439",
                "26000440",
                "26000442",
                "26000443",//fallo
                "26000444",
                "26000445",//fallo
                "26000446",
                "26000447",
                "26000448",
                "26000450",
                "26000451",
                "26000452",//fallo
                "26000453",
                "26000454",
                "26000455",
                "26000456",
                "26000457",
                "26000458",//fallo
                "26000462",
                "26000463",
                "26000464",
                "26000465",
                "26000466",
                "26000467",
                "26000470",
                "26000471",
                "26000472",
                "26000473",
                "26000487",
                "26000488",
                "26000489",
                "26000490",
                "26000491",
                "26000492",
                "26000493",
                "26000494",
                "26000498",
                "26000499",
                "26000500",//fallo
                "26000501",
                "26000502",
                "26000503",
                "26000504",
                "26000505",
                "26000506",
                "26000507",
                "26000509",
                "26000510",
                "26000511",
                "26000512",//fallo
                "26000515",
                "26000521",
                "26000522",
                "26000523",
                "26000524",
                "26000525",
                "26000526",
                "26000527",
                "26000528",
                "26000529",
                "26000530",
                "26000537",
                "26000540",
                "26000541",
                "26000542",
                "26000543",
                "26000545",
                "26000546",
                "26000548",
                "26000549",//fallo
                "26000550",
                "26000551",
                "26000552",
                "26000553",
                "26000554",
                "26000555",
                "26000556",
                "26000557",
                "26000558",
                "26000559",
                "26000564",
                "26000575",
                "26000576",
                "26000577",
                "26000579",
                "26000580",
                "26000581",//fallo
                "26000582",
                "26000588",
                "26000589",//fallo
                "26000590",
                "26000592",
                "26000593",
                "26000594",
                "26000595",
                "26000596",
                "26000597",
                "26000598",
                "26000600",
                "26000601",
                "26000602",//fallo
                "26000603",
                "26000618",
                "26000619",
                "26000620",
                "26000623",
                "26000624",
                "26000625",
                "26000626",
                "26000627",
                "26000628",
                "26000629",
                "26000630",
                "26000643",
                "26000644",
                "26000645",
                "26000646",
                "26000647",*/
                //"26000648",no existe!!!
                "26000649",
                "26000650",
                "26000651",
                "26000652",
                "26000653",
                "26000666",//fallo
                "26000669",
                "26000670",//fallo
                "26000671",
                "26000672",
                "26000673",//fallo
                "26000674",
                "26000675",
                "26000679",//fallo
                "26000680",
                "26000681",//fallo
                "26000682",//fallo
                "26000683",
                "26000684"

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
