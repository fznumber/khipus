package com.encens.khipus.action.customers.reports;

import com.encens.khipus.action.customers.PrintInvoiceDataModel;
import com.encens.khipus.action.reports.GenericReportAction;
import com.encens.khipus.action.reports.PageFormat;
import com.encens.khipus.action.reports.PageOrientation;
import com.encens.khipus.action.reports.ReportFormat;
import com.encens.khipus.exception.EntryDuplicatedException;
import com.encens.khipus.exception.EntryNotFoundException;
import com.encens.khipus.model.admin.User;
import com.encens.khipus.model.customers.*;
import com.encens.khipus.model.warehouse.*;
import com.encens.khipus.reports.GenerationReportData;
import com.encens.khipus.service.customers.DosageSevice;
import com.encens.khipus.service.customers.MovementService;
import com.encens.khipus.service.customers.RePrintsService;
import com.encens.khipus.service.warehouse.WarehouseService;
import com.encens.khipus.util.FileCacheLoader;
import com.encens.khipus.util.MessageUtils;
import com.encens.khipus.util.barcode.BarcodeRenderer;
import com.jatun.titus.reportgenerator.util.TypedReportData;
import net.sf.jasperreports.engine.JRPrintPage;
import org.apache.taglibs.standard.extra.spath.RelativePath;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.*;
import org.jboss.seam.annotations.security.Restrict;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

import com.encens.khipus.util.MoneyUtil;

/**
 * Encens S.R.L.
 * Action to generate product delivery receipt report
 *
 * @author
 * @version $Id: PrintInvoiceReportAction.java  23-sep-2010 18:25:14$
 */
@Name("printInvoiceReportAction")
@Scope(ScopeType.PAGE)
public class PrintInvoiceReportAction extends GenericReportAction {

    @In
    private User currentUser;

    @In
    private WarehouseService warehouseService;
    /*@In(create = true)
    private DosageSevice dosageSevice;*/
    @In(create = true)
    private PrintInvoiceDataModel printInvoiceDataModel;

    @In
    private RePrintsService rePrintsService;

    @In
    private MovementService movementService;

    private Dosage dosage;
    private CustomerOrder customerOrder;
    private MoneyUtil moneyUtil;
    private BarcodeRenderer barcodeRenderer;
    private InvoicePrintType invoicePrintType;
    private Boolean imprimirCopia = false;
    private List<Movement> movements = new ArrayList<Movement>();
    private List<CustomerOrder> customerOrders = new ArrayList<CustomerOrder>();
    private Date date;

    @Restrict("#{s:hasPermission('PRINTINVOICE','VIEW')}")
    public void generateReport(CustomerOrder order) {
        log.debug("Generate PrintInvoiceReportAction......");
        try {
            dosage = warehouseService.findById(Dosage.class,new Long(110));
        } catch (EntryNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        moneyUtil = new MoneyUtil();
        barcodeRenderer = new BarcodeRenderer();
        customerOrder = order;
        setReportFormat(ReportFormat.PDF);

        Map params = new HashMap();

        addVoucherMovementDetailSubReport(params);
        String etiqueta;

        if(imprimirCopia)
            etiqueta = "COPIA";
        else
            etiqueta = "ORIGINAL";

        params.putAll(getReportParams(dosage.getNumberCurrent().intValue(),etiqueta));
        super.generateReport("productDeliveryReceiptReport",
                            "/customers/reports/invoiceReceptionReport.jrxml",
                            PageFormat.LEGAL,
                            PageOrientation.PORTRAIT,
                            MessageUtils.getMessage("Reports.productDeliveryReceipt.title"),
                            params);
    }

    @Restrict("#{s:hasPermission('PRINTINVOICE','VIEW')}")
    public void generateReport() {
        log.debug("Generate PrintInvoiceReportAction......");
        try {
            dosage = warehouseService.findById(Dosage.class,new Long(110));
        } catch (EntryNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        moneyUtil = new MoneyUtil();
        barcodeRenderer = new BarcodeRenderer();

        TypedReportData typedReportData;
        setReportFormat(ReportFormat.PDF);

        Map params = new HashMap();

        addVoucherMovementDetailSubReport(params);
        customerOrder = customerOrders.get(0);
        typedReportData = super.getReport("printInvoiceReceiptReport"
                , "/customers/reports/invoiceReceptionReport.jrxml"
                , "FACTURA"
                , params);
        if(!imprimirCopia)
        {
            int numberInvoice = dosage.getNumberCurrent().intValue();
            for(CustomerOrder order:customerOrders){
                customerOrder = order;
                params.putAll(getReportParams(numberInvoice,"ORIGINAL"));

                TypedReportData reportData = super.getReport("printInvoiceReceiptReport"
                        , "/customers/reports/invoiceReceptionReport.jrxml"
                        , "FACTURA"
                        , params);


                    for(JRPrintPage page:(List<JRPrintPage>)reportData.getJasperPrint().getPages())
                    typedReportData.getJasperPrint().addPage(page);

                numberInvoice ++;
            }
            try {
                GenerationReportData generationReportData = new GenerationReportData(typedReportData);
                generationReportData.exportReport();
            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }


                for(CustomerOrder order:customerOrders){
                    for(ArticleOrder articleOrder:order.getArticleOrders()){
                        createArticleOrder(articleOrder,order);
                    }
                }
                int aux = dosage.getNumberCurrent().intValue() +1;
                dosage.setNumberCurrent(new BigDecimal(aux));
        }else{

        }

    }

    private void createArticleOrder(ArticleOrder articleOrder,CustomerOrder order)
    {
        Movement movement = new Movement();
        movement.setDate(order.getDateDelicery());
        movement.setGloss("nombre");//nombre
        movement.setType("V");
        movement.setCaseEspecial("N");
        movement.setMountTeso(articleOrder.getPrice());
        movement.setMountCust(articleOrder.getTotal());
        movement.setAccountID(articleOrder.getId().getIdAccount());
        movement.setUsrID(currentUser.getId());
        movement.setEstCod("2009");
        movement.setCoin("B");
        movement.setPiID(articleOrder.getCustomerOrder().getId());
        //movement.setNumberPrePrint();verificar
        movement.setNumberInvoice(dosage.getNumberCurrent().longValue());
        movement.setCodControl(moneyUtil.getCodigoDeControl(dosage.getKey()));
        movement.setDosage(dosage);
        movement.setNit(order.getClientOrder().getNumberDoc());
        movement.setMount(articleOrder.getAmount());
        movement.setTypePay("1");
        movement.setTypeChange(6.96);
        movement.setTotalInvoice(order.getTotal().doubleValue());
        //movement.setDescrOrder();verificar
        movement.setCustomerOrder(order);
        try {
            movementService.create(movement);
        } catch (EntryDuplicatedException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    @Override
    protected String getEjbql() {
        return "";
    }

    @Create
    public void init() {
        restrictions = new String[]{};
        //sortProperty = "productDelivery.id";
    }


    /**
     * get report params
     *
     * @return Map
     */
    private Map<String, Object> getReportParams(int numfac,String etiqueta) {

        String filePath = FileCacheLoader.i.getPath("/customers/reports/qr_inv.png");

        String keyQR = moneyUtil.getLlaveQR(dosage.getNumberAuthorization().toString()
                                            ,dosage.getNumberCurrent()
                                            ,customerOrder.getClientOrder().getNumberDoc()
                                            ,customerOrder.getDateDelicery()
                                            ,customerOrder.getTotal().intValue()
                                            ,dosage.getKey());
        Map<String, Object> paramMap = new HashMap<String, Object>();
        paramMap.put("nitEmpresa","123456022");
        paramMap.put("numFac",numfac);
        paramMap.put("numAutorizacion",dosage.getNumberAuthorization());
        paramMap.put("nitCliente",customerOrder.getClientOrder().getNumberDoc());
        paramMap.put("fecha",customerOrder.getDateDelicery());
        paramMap.put("nombreCliente",customerOrder.getClientOrder().getHst());//verificar el nombre del cliente
        paramMap.put("fechaLimite",dosage.getDateExpiration());
        paramMap.put("codigoControl",moneyUtil.getCodigoDeControl(dosage.getKey()));
        paramMap.put("tipoEtiqueta",etiqueta);
        //verificar por que no requiere el codigo de control

        paramMap.put("llaveQR",keyQR);
        paramMap.put("totalLiteral",moneyUtil.Convertir(customerOrder.getTotal().toString(), true));
        paramMap.put("total",customerOrder.getTotal());
        barcodeRenderer.generateQR(keyQR,filePath);
        return paramMap;
    }

    /**
     * add voucher movement detail sub report in main report
     *
     * @param mainReportParams
     */
    private void addVoucherMovementDetailSubReport(Map mainReportParams) {
        log.debug("Generating addVoucherMovementDetailSubReport.............................");

        Map<String, Object> params = new HashMap<String, Object>();

        String ejbql = "SELECT " +
                " articleOrder.amount, " +
                " articleOrder.productItem.name, " +
                " articleOrder.price, " +
                " articleOrder.total "+
                " FROM ArticleOrder articleOrder";

        String[] restrictions = new String[]{

                "articleOrder.customerOrder = #{printInvoiceReportAction.customerOrder}"};

        String orderBy = "articleOrder.productItem.name";

        //generate the sub report
        String subReportKey = "INVOICEDETAILSUBREPORT";
        TypedReportData subReportData = super.generateSubReport(
                subReportKey,
                "/customers/reports/invoiceDetailSubReport.jrxml",
                PageFormat.LETTER,
                PageOrientation.PORTRAIT,
                createQueryForSubreport(subReportKey, ejbql, Arrays.asList(restrictions), orderBy),
                params);

        //add in main report params
        mainReportParams.putAll(subReportData.getReportParams());
        mainReportParams.put(subReportKey, subReportData.getJasperReport());
    }

    @Factory(value = "invoicePrintTypes", scope = ScopeType.STATELESS)
    public InvoicePrintType[] initProductDeliveryTypes() {
        return InvoicePrintType.values();
    }

    public void search()
    {
        printInvoiceDataModel.search();
        movements = movementService.findMovementByDate(date);
        if(movements.size() > 0)
            imprimirCopia = true;
        else
            imprimirCopia = false;

        customerOrders = printInvoiceDataModel.getResultList();

    }

    public CustomerOrder getCustomerOrder() {
        return customerOrder;
    }

    public void setCustomerOrder(CustomerOrder customerOrder) {
        this.customerOrder = customerOrder;
    }

    public InvoicePrintType getInvoicePrintType() {
        return invoicePrintType;
    }

    public void setInvoicePrintType(InvoicePrintType invoicePrintType) {
        this.invoicePrintType = invoicePrintType;
    }

    public Boolean getImprimirCopia() {
        return imprimirCopia;
    }

    public void setImprimirCopia(Boolean imprimirCopia) {
        this.imprimirCopia = imprimirCopia;
    }

    public PrintInvoiceDataModel getPrintInvoiceDataModel() {
        return printInvoiceDataModel;
    }

    public void setPrintInvoiceDataModel(PrintInvoiceDataModel printInvoiceDataModel) {
        this.printInvoiceDataModel = printInvoiceDataModel;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }
}
