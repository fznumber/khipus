package com.encens.khipus.action.production.reports;

import com.encens.khipus.action.reports.GenericReportAction;
import com.encens.khipus.action.reports.PageFormat;
import com.encens.khipus.action.reports.PageOrientation;
import com.encens.khipus.action.reports.ReportFormat;
import com.encens.khipus.framework.service.GenericService;
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

import java.text.DateFormat;
import java.text.DecimalFormat;
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

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy");
        DateFormat df = new SimpleDateFormat("dd/MM/yyyy");

        dateIni = Calendar.getInstance();
        dateEnd = Calendar.getInstance();
        dateIni.set(gestion.getYear(),month.getValue(),periodo.getInitDay());
        dateEnd.set(gestion.getYear(),month.getValue(),periodo.getEndDay(month.getValue()+1));
        sdf.setCalendar(dateIni);
        sdf.setCalendar(dateEnd);

        addSummaryTotal(reportParameters);
        log.debug("generating expenseBudgetReport......................................");

        reportParameters.put("reportTitle",messages.get("Report.titleGeneral"));
        reportParameters.put("period",messages.get("Report.period"));
        reportParameters.put("startDate",df.format(dateIni.getTime()));
        reportParameters.put("endDate",df.format(dateEnd.getTime()));

        super.generateReport(
                "rawMaterialPaySummaryReportAction",
                "/production/reports/rawMaterialPaySummaryReport.jrxml",
                PageFormat.LETTER,
                PageOrientation.PORTRAIT,
                messages.get("Report.rawMaterialPaySummaryReportAction"),
                reportParameters);
    }

    private void addSummaryTotal(HashMap<String, Object> params) {
        //discounts = rawMaterialPayRollService.getDiscounts(dateIni.getTime(),dateEnd.getTime(),null,null);
        DecimalFormat df = new DecimalFormat("#0.00");
        discounts = rawMaterialPayRollService.getDiscounts(dateIni,dateEnd,zone,metaProduct);

        summaryTotal = rawMaterialPayRollService.getSumaryTotal(dateIni,dateEnd,zone,metaProduct);

        Double totalMoneyCollected = discounts.mount;
        Double totalDifferencesMoney = rawMaterialPayRollService.getTotalMoneyDiff(discounts.unitPrice, dateIni, dateEnd, metaProduct);
        Double diffTotal = rawMaterialPayRollService.getTotalDiff(discounts.unitPrice, dateIni, dateEnd, metaProduct);
        Double balanceWeightTotal = rawMaterialPayRollService.getBalanceWeightTotal(discounts.unitPrice, dateIni, dateEnd, metaProduct);
        Double totalMoneyBalance = totalMoneyCollected + totalDifferencesMoney;
        params.put("total_collected", df.format(discounts.collected));
        params.put("diff_total", df.format(diffTotal));
        params.put("price_unit", df.format(discounts.unitPrice));
        params.put("total_money_collected", df.format(totalMoneyCollected));
        params.put("difference_money", df.format(totalDifferencesMoney));
        params.put("total_money", df.format(totalMoneyBalance));
        params.put("weight_balance_total", df.format(balanceWeightTotal));
        params.put("total_other_incom", df.format(totalMoneyBalance + discounts.otherIncome));

        //discounts
        Double totalDiscount = discounts.alcohol + discounts.concentrated + discounts.yogurt
                             + discounts.veterinary + discounts.credit + discounts.recip + discounts.retention
                             + discounts.otherDiscount ;
        //Double liquidPay = totalMoney - totalDifferences;
        params.put("alcohol", df.format(discounts.alcohol));
        params.put("concentrated", df.format(discounts.concentrated));
        params.put("yogurt", df.format(discounts.yogurt));
        params.put("veterinary", df.format(discounts.veterinary));
        params.put("credit", df.format(discounts.credit));
        params.put("recip", df.format(discounts.recip));
        params.put("retention", df.format(discounts.retention));
        params.put("otrosDescuentos", df.format(discounts.otherDiscount));
        params.put("otrosIngresos", df.format(discounts.otherIncome));
        params.put("ajuste", df.format(discounts.adjustment));
        params.put("total_differences", df.format(totalDiscount));
        params.put("liquid_pay", df.format(discounts.liquid));

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

    public void selectProductiveZone(ProductiveZone productiveZone) {
        try {
            productiveZone = getService().findById(ProductiveZone.class, productiveZone.getId());
            setZone(productiveZone);
        } catch (Exception ex) {
            log.error("Caught Error", ex);
        }
    }

    public String getFullNameOfProductiveZone() {
        return (zone == null ? "" : zone.getFullName());
    }

    public void setFullNameOfProductiveZone(String fullName) {

    }

    protected GenericService getService() {
        return rawMaterialPayRollService;
    }

}