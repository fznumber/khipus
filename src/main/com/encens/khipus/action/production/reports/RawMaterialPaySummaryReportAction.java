package com.encens.khipus.action.production.reports;

import com.encens.khipus.action.reports.GenericReportAction;
import com.encens.khipus.action.reports.PageFormat;
import com.encens.khipus.action.reports.PageOrientation;
import com.encens.khipus.model.employees.GeneratedPayrollType;
import com.encens.khipus.model.employees.Gestion;
import com.encens.khipus.model.employees.GestionPayroll;
import com.encens.khipus.model.employees.Month;
import com.encens.khipus.model.production.Periodo;
import com.encens.khipus.model.production.ProductiveZone;
import com.encens.khipus.service.employees.PayrollSummaryReportService;
import com.encens.khipus.service.production.RawMaterialPayRollService;
import com.encens.khipus.service.production.RawMaterialPayRollServiceBean;
import com.encens.khipus.util.DateUtils;
import com.encens.khipus.util.MessageUtils;
import com.jatun.titus.reportgenerator.util.TypedReportData;
import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.*;
import org.jboss.seam.annotations.security.Restrict;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.*;

import static org.jboss.seam.international.StatusMessage.Severity.ERROR;

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
        //add seam properties as filter, summary only to OFFICIAL payroll
        //setGeneratedPayrollType(GeneratedPayrollType.OFFICIAL);



        Map params = new HashMap();
        //params.put("TITLE_PARAM", "PRUEBA");
        //params.put("GESTION_PARAM", 2013);

        //add summary by sede sub report
        //addSummaryBySedeSubReport(params);
        //add summary by currency sub report
        //addSummaryByCurrencySubReport(params);

        //params.put("SECTORTITLE_PARAM", (sector != null) ? sector.getName() : "");
        dateIni = Calendar.getInstance();
        dateEnd = Calendar.getInstance();
        dateIni.set(gestion.getYear(),month.getValue(),periodo.getInitDay());
        dateEnd.set(gestion.getYear(),month.getValue(),periodo.getEndDay(month.getValue()));

        addSummaryTotal(params);



        summaryReportTitle = "Titulo de prueba";

        super.generateReport("RawMaterialPaySummaryReportAction", "/production/reports/rawMaterialPaySummaryReport.jrxml",
                PageFormat.LETTER, PageOrientation.PORTRAIT, "prueba", params);

    }



    private void addSummaryTotal(Map params) {
        discounts = rawMaterialPayRollService.getDiscounts(dateIni.getTime(),dateEnd.getTime(),null,null);

        summaryTotal = rawMaterialPayRollService.getSumaryTotal(dateIni.getTime(),dateEnd.getTime(),null,null);
        Double totalMoney = (summaryTotal.collectedTotal * discounts.unitPrice) - summaryTotal.differencesTotal;
        params.put("total_collected", summaryTotal.collectedTotal);
        params.put("price_unit", discounts.unitPrice);
        params.put("difference_money", summaryTotal.differencesTotal * unitPrice);
        params.put("total_collected_money", summaryTotal.collectedTotal * discounts.unitPrice);
        params.put("total_money", totalMoney);
        params.put("weight_balance_total", summaryTotal.balanceWeightTotal);

        //discounts
        Double totalDifferences = discounts.yogurt + discounts.veterinary + discounts.credit + discounts.recip + discounts.retention;
        Double liquidPay = totalMoney - totalDifferences;
        params.put("yogurt", discounts.yogurt);
        params.put("veterinary", discounts.veterinary);
        params.put("credit", discounts.credit);
        params.put("recip", discounts.recip);
        params.put("retention", discounts.retention);
        params.put("total_differences", totalDifferences);
        params.put("liquid_pay", liquidPay);

    }

    @Override
    protected String getEjbql() {
        return "";
    }

    @Create
    public void init() {
        restrictions = new String[]{};
    }

    /**
     * Add the sub report in main report
     * summary payment method by sede
     *
     * @param mainReportParams main report params
     */
    private void addSummaryBySedeSubReport(Map mainReportParams) {
        log.debug("Generating addSummaryBySedeSubReport.............................");
        String subReportKey = "SUMMARYBYSEDESUBREPORT";

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("SUMMARYSERVICE_PARAM", (PayrollSummaryReportService) Component.getInstance("payrollSummaryReportService"));

        String gestionPayrollConditions = composeWhereGestionPayrollConditions("generatedPayroll.gestionPayroll.id");

        String ejbql = "SELECT " +
                "generatedPayroll.id," +
                "generatedPayroll.gestionPayroll.businessUnit.publicity," +
                "generatedPayroll.name," +
                "generatedPayroll.gestionPayroll.jobCategory.sector.id," +
                "generatedPayroll.gestionPayroll.jobCategory.id," +
                "generatedPayroll.gestionPayroll.exchangeRate.rate" +
                " FROM GeneratedPayroll generatedPayroll" +
                (gestionPayrollConditions != null ? " WHERE " + gestionPayrollConditions : "");

        String[] restrictions = new String[]{"generatedPayroll.generatedPayrollType=#{summaryPayrollByPaymentMethodReportAction.generatedPayrollType}"};
        String pollByCareerOrder = "generatedPayroll.id";

        //generate the sub report
        TypedReportData subReportData = super.generateSubReport(
                subReportKey,
                "/employees/reports/payrollSummaryBySedeSubReport.jrxml",
                PageFormat.LETTER,
                PageOrientation.PORTRAIT,
                createQueryForSubreport(subReportKey, ejbql, Arrays.asList(restrictions), pollByCareerOrder),
                params);

        //add in main report params
        mainReportParams.putAll(subReportData.getReportParams());
        mainReportParams.put(subReportKey, subReportData.getJasperReport());
    }

    /**
     * Add the sub report in main report
     * summary by currency
     *
     * @param mainReportParams main report params
     */
    private void addSummaryByCurrencySubReport(Map mainReportParams) {
        log.debug("Generating addSummaryByCurrencySubReport.............................");
        String subReportKey = "SUMMARYBYCURRENCYSUBREPORT";

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("SUMMARYSERVICE_PARAM", (PayrollSummaryReportService) Component.getInstance("payrollSummaryReportService"));

        String gestionPayrollConditions = composeWhereGestionPayrollConditions("generatedPayroll.gestionPayroll.id");
        String ejbql = "SELECT " +
                "generatedPayroll.id," +
                "generatedPayroll.gestionPayroll.businessUnit.publicity," +
                "generatedPayroll.gestionPayroll.jobCategory.sector.id," +
                "generatedPayroll.gestionPayroll.jobCategory.id," +
                "generatedPayroll.gestionPayroll.exchangeRate.rate" +
                " FROM GeneratedPayroll generatedPayroll" +
                (gestionPayrollConditions != null ? " WHERE " + gestionPayrollConditions : "");

        String[] restrictions = new String[]{"generatedPayroll.generatedPayrollType=#{summaryPayrollByPaymentMethodReportAction.generatedPayrollType}"};
        String pollByCareerOrder = "generatedPayroll.id";

        //generate the sub report
        TypedReportData subReportData = super.generateSubReport(
                subReportKey,
                "/employees/reports/payrollSummaryByCurrencySubReport.jrxml",
                PageFormat.LETTER,
                PageOrientation.PORTRAIT,
                createQueryForSubreport(subReportKey, ejbql, Arrays.asList(restrictions), pollByCareerOrder),
                params);

        //add in main report params
        mainReportParams.putAll(subReportData.getReportParams());
        mainReportParams.put(subReportKey, subReportData.getJasperReport());
    }

    /**
     * Compose where conditions from gestion payroll list
     *
     * @param gestionPayrollIdProperty property to condition
     * @return String
     */
    private String composeWhereGestionPayrollConditions(String gestionPayrollIdProperty) {
        String conditions = null;
        if (gestionPayrollList != null) {
            for (GestionPayroll gestionPayroll : gestionPayrollList) {
                if (conditions == null) {
                    conditions = gestionPayrollIdProperty + "=" + gestionPayroll.getId();
                } else {
                    conditions = conditions + " OR " + gestionPayrollIdProperty + "=" + gestionPayroll.getId();
                }
            }
            if (conditions != null) {
                conditions = "(" + conditions + ")";
            }
        }
        return conditions;
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

}