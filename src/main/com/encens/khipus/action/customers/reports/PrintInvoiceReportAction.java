package com.encens.khipus.action.customers.reports;

import com.encens.khipus.action.reports.GenericReportAction;
import com.encens.khipus.action.reports.PageFormat;
import com.encens.khipus.action.reports.PageOrientation;
import com.encens.khipus.action.reports.ReportFormat;
import com.encens.khipus.exception.EntryNotFoundException;
import com.encens.khipus.model.admin.User;
import com.encens.khipus.model.customers.CustomerOrder;
import com.encens.khipus.model.customers.Dosage;
import com.encens.khipus.model.warehouse.InventoryMovement;
import com.encens.khipus.model.warehouse.InventoryMovementPK;
import com.encens.khipus.model.warehouse.ProductDelivery;
import com.encens.khipus.model.warehouse.WarehouseVoucher;
import com.encens.khipus.service.customers.DosageSevice;
import com.encens.khipus.service.warehouse.WarehouseService;
import com.encens.khipus.util.MessageUtils;
import com.jatun.titus.reportgenerator.util.TypedReportData;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.security.Restrict;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
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

    private Dosage dosage;
    private CustomerOrder customerOrder;
    private MoneyUtil moneyUtil;

    @Restrict("#{s:hasPermission('PRODUCTDELIVERY','VIEW')}")
    public void generateReport(CustomerOrder order) {
        log.debug("Generate PrintInvoiceReportAction......");
        try {
            dosage = warehouseService.findById(Dosage.class,new Long(110));
        } catch (EntryNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        moneyUtil = new MoneyUtil();
        customerOrder = order;
        setReportFormat(ReportFormat.PDF);

        Map params = new HashMap();

        addVoucherMovementDetailSubReport(params);

        params.putAll(getReportParams(null));
        super.generateReport("productDeliveryReceiptReport",
                            "/customers/reports/invoiceReceptionReport.jrxml",
                            PageFormat.LEGAL,
                            PageOrientation.PORTRAIT,
                            MessageUtils.getMessage("Reports.productDeliveryReceipt.title"),
                            params);
    }

    @Override
    protected String getEjbql() {
        return "SELECT customerOrder.id " +
                " FROM CustomerOrder customerOrder";
    }

    @Create
    public void init() {
        restrictions = new String[]{"customerOrder = #{printInvoiceReportAction.customerOrder}"};
        //sortProperty = "productDelivery.id";
    }


    /**
     * get report params
     *
     * @return Map
     */
    private Map<String, Object> getReportParams(WarehouseVoucher warehouseVoucher) {


        Map<String, Object> paramMap = new HashMap<String, Object>();
        paramMap.put("nitEmpresa","123456022");
        paramMap.put("numFac",dosage.getNumberCurrent());
        paramMap.put("numAutorizacion",dosage.getNumberAuthorization());
        paramMap.put("nitCliente",customerOrder.getClientOrder().getNumberDoc());
        paramMap.put("fecha",customerOrder.getDateDelicery());
        paramMap.put("nombreCliente",customerOrder.getClientOrder().getHst());//verificar el nombre del cliente
        paramMap.put("fechaLimite",dosage.getDateExpiration());
        paramMap.put("codigoControl",moneyUtil.getCodigoDeControl(dosage.getKey()));
        //verificar por que no requiere el codigo de control
        paramMap.put("llaveQR",moneyUtil.getLlaveQR(dosage.getNumberAuthorization().toString(),dosage.getNumberCurrent(),customerOrder.getClientOrder().getNumberDoc(),customerOrder.getDateDelicery(),customerOrder.getTotal().intValue(),dosage.getKey()));
        paramMap.put("totalLiteral",moneyUtil.Convertir(customerOrder.getTotal().toString(), true));
        paramMap.put("total",customerOrder.getTotal());

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

    public CustomerOrder getCustomerOrder() {
        return customerOrder;
    }

    public void setCustomerOrder(CustomerOrder customerOrder) {
        this.customerOrder = customerOrder;
    }
}
