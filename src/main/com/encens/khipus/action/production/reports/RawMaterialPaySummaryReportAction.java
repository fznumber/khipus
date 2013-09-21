package com.encens.khipus.action.production.reports;

import com.encens.khipus.action.reports.GenericReportAction;
import com.encens.khipus.action.reports.PageFormat;
import com.encens.khipus.action.reports.PageOrientation;
import com.encens.khipus.action.reports.ReportFormat;
import com.encens.khipus.model.employees.GeneratedPayrollType;
import com.encens.khipus.model.employees.Gestion;
import com.encens.khipus.model.employees.GestionPayroll;
import com.encens.khipus.model.employees.Month;
import com.encens.khipus.model.production.MetaProduct;
import com.encens.khipus.model.production.Periodo;
import com.encens.khipus.model.production.ProductiveZone;
import com.encens.khipus.service.production.RawMaterialPayRollService;
import com.encens.khipus.service.production.RawMaterialPayRollServiceBean;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.*;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

/**
 * Encens S.R.L.
 * Action to generate payroll summary report by payment method and currency
 *
 * @author
 * @version $Id: SummaryPayrollByPaymentMethodReportAction.java  22-ene-2010 11:38:12$
 */
@Name("rawMaterialPaySummaryReportAction")
@Scope(ScopeType.CONVERSATION)
//@Restrict("#{s:hasPermission('RAWMATERIALPAYSUMMARY','VIEW')}")
public class RawMaterialPaySummaryReportAction extends GenericReportAction {
    @In
    RawMaterialPayRollService rawMaterialPayRollService;

    private String summaryReportTitle;
    private String gestionTitle;

    private Gestion gestion;
    private Month month;
    private Periodo periodo;
    private ProductiveZone zone;
    private MetaProduct metaProduct;
    private double unitPrice;
    private double mountCollection;
    private double totalCollectionXUnitPrice;
    private double differences;
    private double weightReal;
    private double totalWeightRealXUnitPrice;
    private double yogurt;
    private double veterinari;
    private double credit;
    private double recipient;
    private RawMaterialPayRollServiceBean.SummaryTotal summaryTotal;
    private RawMaterialPayRollServiceBean.Discounts discounts;

    private Calendar dateIni;
    private Calendar dateEnd;

    private List<GestionPayroll> gestionPayrollList;


    private GeneratedPayrollType generatedPayrollType = GeneratedPayrollType.OFFICIAL;


    public void generateReport() {

        HashMap<String, Object> reportParameters = new HashMap<String, Object>();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        dateIni = Calendar.getInstance();
        dateEnd = Calendar.getInstance();
        dateIni.set(gestion.getYear(),month.getValue(),periodo.getInitDay());
        dateEnd.set(gestion.getYear(),month.getValue(),periodo.getEndDay(month.getValue()));
        sdf.format(dateIni);
        sdf.format(dateEnd);
        addSummaryTotal(reportParameters);
        log.debug("generating expenseBudgetReport......................................");

        super.generateReport(
                "rawMaterialPaySummaryReportAction",
                "/production/reports/rawMaterialPaySummaryReport.jrxml",
                PageFormat.LETTER,
                PageOrientation.PORTRAIT,
                messages.get("Report.rawMaterialPaySummaryReportAction"),
                reportParameters);
    }



    private void addSummaryTotal(HashMap<String, Object> params) {
        discounts = rawMaterialPayRollService.getDiscounts(dateIni.getTime(),dateEnd.getTime(),null,null);

        summaryTotal = rawMaterialPayRollService.getSumaryTotal(dateIni.getTime(),dateEnd.getTime(),null,null);
        Double totalMoney = (summaryTotal.collectedTotal * discounts.unitPrice) - summaryTotal.differencesTotal;
        params.put("total_collected", summaryTotal.collectedTotal.toString());
        params.put("price_unit", discounts.unitPrice.toString());
        params.put("difference_money", ((Double) (summaryTotal.differencesTotal * unitPrice)).toString());
        params.put("total_collected_money", ((Double) (summaryTotal.collectedTotal * discounts.unitPrice)).toString());
        params.put("total_money", totalMoney.toString());
        params.put("weight_balance_total", (summaryTotal.balanceWeightTotal).toString());

        //discounts
        Double totalDifferences = discounts.yogurt + discounts.veterinary + discounts.credit + discounts.recip + discounts.retention;
        Double liquidPay = totalMoney - totalDifferences;
        params.put("yogurt", discounts.yogurt.toString());
        params.put("veterinary", discounts.veterinary.toString());
        params.put("credit", discounts.credit.toString());
        params.put("recip", discounts.recip.toString());
        params.put("retention", discounts.retention.toString());
        params.put("total_differences", totalDifferences.toString());
        params.put("liquid_pay", liquidPay.toString());

    }

    @Override
    protected String getEjbql() {
        return "";
    }

    @Create
    public void init() {
    }

    public RawMaterialPayRollService getRawMaterialPayRollService() {
        return rawMaterialPayRollService;
    }

    public void setRawMaterialPayRollService(RawMaterialPayRollService rawMaterialPayRollService) {
        this.rawMaterialPayRollService = rawMaterialPayRollService;
    }

    public String getSummaryReportTitle() {
        return summaryReportTitle;
    }

    public void setSummaryReportTitle(String summaryReportTitle) {
        this.summaryReportTitle = summaryReportTitle;
    }

    public String getGestionTitle() {
        return gestionTitle;
    }

    public void setGestionTitle(String gestionTitle) {
        this.gestionTitle = gestionTitle;
    }

    public Gestion getGestion() {
        return gestion;
    }

    public void setGestion(Gestion gestion) {
        this.gestion = gestion;
    }

    public Month getMonth() {
        return month;
    }

    public void setMonth(Month month) {
        this.month = month;
    }

    public List<GestionPayroll> getGestionPayrollList() {
        return gestionPayrollList;
    }

    public void setGestionPayrollList(List<GestionPayroll> gestionPayrollList) {
        this.gestionPayrollList = gestionPayrollList;
    }

    public void cleanGestionList() {
        setGestionPayrollList(null);
    }

    public Periodo getPeriodo() {
        return periodo;
    }

    public void setPeriodo(Periodo periodo) {
        this.periodo = periodo;
    }

    public ProductiveZone getZone() {
        return zone;
    }

    public void setZone(ProductiveZone zone) {
        this.zone = zone;
    }

    @Factory(value = "periodos", scope = ScopeType.STATELESS)
    public Periodo[] getPeriodos() {
        return Periodo.values();
    }
    /*
    public void selectProductiveZone(ProductiveZone productiveZone) {
        try {
            productiveZone = getService().findById(ProductiveZone.class, productiveZone.getId());
            getInstance().setProductiveZone(productiveZone);
        } catch (Exception ex) {
            log.error("Caught Error", ex);
            facesMessages.addFromResourceBundle(ERROR, "Common.globalError.description");
        }
    }
    */

    public GeneratedPayrollType getGeneratedPayrollType() {
        return generatedPayrollType;
    }

    public void setGeneratedPayrollType(GeneratedPayrollType generatedPayrollType) {
        this.generatedPayrollType = generatedPayrollType;
    }

    public MetaProduct getMetaProduct() {
        return metaProduct;
    }

    public void setMetaProduct(MetaProduct metaProduct) {
        this.metaProduct = metaProduct;
    }

}