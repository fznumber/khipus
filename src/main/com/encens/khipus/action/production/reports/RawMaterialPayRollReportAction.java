package com.encens.khipus.action.production.reports;

import com.encens.khipus.action.reports.GenericReportAction;
import com.encens.khipus.action.reports.PageFormat;
import com.encens.khipus.action.reports.PageOrientation;
import com.encens.khipus.framework.service.GenericService;
import com.encens.khipus.model.employees.GeneratedPayrollType;
import com.encens.khipus.model.employees.Gestion;
import com.encens.khipus.model.employees.GestionPayroll;
import com.encens.khipus.model.employees.Month;
import com.encens.khipus.model.production.MetaProduct;
import com.encens.khipus.model.production.Periodo;
import com.encens.khipus.model.production.ProductiveZone;
import com.encens.khipus.model.production.RawMaterialPayRoll;
import com.encens.khipus.reports.GenerationReportData;
import com.encens.khipus.service.production.ProductiveZoneService;
import com.encens.khipus.service.production.RawMaterialPayRollService;
import com.encens.khipus.service.production.RawMaterialPayRollServiceBean;
import com.encens.khipus.util.MessageUtils;
import com.jatun.titus.reportgenerator.util.TypedReportData;
import net.sf.jasperreports.engine.JRPrintPage;
import net.sf.jasperreports.engine.JasperPrint;
import org.apache.commons.lang.time.FastDateFormat;
import org.apache.poi.hssf.record.formula.functions.T;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.*;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.jboss.seam.international.StatusMessage.Severity.ERROR;

/**
 * Encens S.R.L.
 * Action to generate payroll summary report by payment method and currency
 *
 * @author
 * @version $Id: SummaryPayrollByPaymentMethodReportAction.java  22-ene-2010 11:38:12$
 */
@Name("rawMaterialPayRollReportAction")
@Scope(ScopeType.CONVERSATION)
//@Restrict("#{s:hasPermission('RAWMATERIALPAYSUMMARY','VIEW')}")
public class RawMaterialPayRollReportAction extends GenericReportAction {
    @In
    RawMaterialPayRollService rawMaterialPayRollService;

    @In
    ProductiveZoneService productiveZoneService;

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
        log.debug("Generate RotatoryFundReportAction........");

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy");
        dateIni = Calendar.getInstance();
        dateEnd = Calendar.getInstance();
        dateIni.set(gestion.getYear(),month.getValue(),periodo.getInitDay());
        dateEnd.set(gestion.getYear(),month.getValue(),periodo.getEndDay(month.getValue()+1,gestion.getYear()));
        sdf.setCalendar(dateIni);
        sdf.setCalendar(dateEnd);
        DateFormat df = new SimpleDateFormat("dd/MM/yyyy");

        JasperPrint jasperPrint1 = new JasperPrint();
        JasperPrint jasperPrint2;

        Map params = new HashMap();

        List<ProductiveZone> productiveZones = productiveZoneService.findAll();
        TypedReportData typedReportData;
        TypedReportData mostrar = new TypedReportData();
        RawMaterialPayRoll rawMaterialPayRoll;
        boolean tomarPrimero = true;

        for(ProductiveZone productiveZone :productiveZones)
         {
            zone = productiveZone;
            rawMaterialPayRoll = rawMaterialPayRollService.getTotalsRawMaterialPayRoll(dateIni,dateEnd,zone,metaProduct);

            params.put("reportTitle",messages.get("Report.titleGeneral"));
            params.put("periodo",(periodo.getResourceKey().toString()== "Periodo.first") ?"1RA QUINCENA":"2DA QUINCENA" +" "+getMes(month));
            params.put("startDate",df.format(dateIni.getTime()));
            params.put("endDate",df.format(dateEnd.getTime()));
            params.put("nombre_gab","GAB: "+zone.getNumber()+" - "+zone.getName());


            params.put("totalCollectedByGAB", rawMaterialPayRoll.getTotalCollectedByGAB());
            params.put("totalMountCollectdByGAB",rawMaterialPayRoll.getTotalMountCollectdByGAB());
            params.put("totalRetentionGAB",rawMaterialPayRoll.getTotalRetentionGAB());
            params.put("totalCreditByGAB",rawMaterialPayRoll.getTotalCreditByGAB());
            params.put("totalVeterinaryByGAB",rawMaterialPayRoll.getTotalVeterinaryByGAB());
            params.put("totalAlcoholByGAB",rawMaterialPayRoll.getTotalAlcoholByGAB());
            params.put("totalConcentratedByGAB",rawMaterialPayRoll.getTotalConcentratedByGAB());
            params.put("totalYogourdByGAB",rawMaterialPayRoll.getTotalYogourdByGAB());
            params.put("totalRecipByGAB",rawMaterialPayRoll.getTotalRecipByGAB());
            params.put("totalDiscountByGAB",rawMaterialPayRoll.getTotalDiscountByGAB());
            params.put("totalAdjustmentByGAB",rawMaterialPayRoll.getTotalAdjustmentByGAB());
            params.put("totalOtherIncomeByGAB",rawMaterialPayRoll.getTotalOtherIncomeByGAB());
            params.put("totalLiquidByGAB",rawMaterialPayRoll.getTotalLiquidByGAB());
            params.put("dateStart","Fecha Inicio - " + FastDateFormat.getInstance("dd-MM-yyyy").format(dateIni));
            params.put("dateEnd","Fecha Fin - "+ FastDateFormat.getInstance("dd-MM-yyyy").format(dateEnd));

            typedReportData = super.getReport("rotatoryFundReport", "/production/reports/rawMaterialPayRollReport.jrxml", MessageUtils.getMessage("Report.rawMaterialPayRollReportAction"), params);
            if(tomarPrimero)
            {
                jasperPrint1 = typedReportData.getJasperPrint();
                mostrar = typedReportData;
            }else
            {
                jasperPrint2 = typedReportData.getJasperPrint();
                List pages = jasperPrint2.getPages();
                for(Object jrPrintPage: pages){
                    jasperPrint1.addPage((JRPrintPage)jrPrintPage);
                }
            }
             tomarPrimero = false;
        }

        try {
            mostrar.setJasperPrint(jasperPrint1);
            GenerationReportData generationReportData = new GenerationReportData(mostrar);
            generationReportData.exportReport();
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    @Override
    protected String getEjbql() {
        return "SELECT " +
                " rawMaterialPayRecord.id, " +
                " rawMaterialProducer.firstName, " +
                " RawMaterialPayRecord.totalAmount, " +
                " rawMaterialPayRoll.unitPrice, " +
                " RawMaterialPayRecord.totalPayCollected, " +
                " rawMaterialProducerDiscount.withholdingTax, " +
                " rawMaterialProducerDiscount.alcohol, " +
                " rawMaterialProducerDiscount.concentrated, " +
                " rawMaterialProducerDiscount.credit, " +
                " rawMaterialProducerDiscount.veterinary, " +
                " rawMaterialProducerDiscount.yogurt, " +
                " rawMaterialProducerDiscount.cans, " +
                " rawMaterialProducerDiscount.otherDiscount, " +
                " rawMaterialPayRecord.productiveZoneAdjustment, " +
                " rawMaterialProducerDiscount.otherIncoming, " +
                " rawMaterialPayRecord.liquidPayable, " +
                " rawMaterialProducer.lastName, " +
                " rawMaterialProducer.maidenName, " +
                " productiveZone.name " +
                " FROM RawMaterialPayRoll rawMaterialPayRoll " +
                " inner join RawMaterialPayRoll.rawMaterialPayRecordList rawMaterialPayRecord " +
                " inner join rawMaterialPayRecord.rawMaterialProducerDiscount rawMaterialProducerDiscount " +
                " inner join rawMaterialProducerDiscount.rawMaterialProducer rawMaterialProducer " +
                " inner join RawMaterialPayRoll.productiveZone productiveZone ";

    }

    private String getMes(Month month)
    {
        String result = "";
        if(Month.JANUARY == month)
            result = "Enero";
        if(Month.FEBRUARY == month)
            result = "Febrero";
        if(Month.MARCH == month)
            result = "Marzo";
        if(Month.APRIL == month)
            result = "Abril";
        if(Month.MAY == month)
            result = "Mayo";
        if(Month.JUNE == month)
            result = "Junio";
        if(Month.JULY == month)
            result = "Julio";
        if(Month.AUGUST == month)
            result = "Agosto";
        if(Month.SEPTEMBER == month)
            result = "Septiembre";
        if(Month.OCTOBER == month)
            result = "Octubre";
        if(Month.NOVEMBER == month)
            result = "Noviembre";
        if(Month.DECEMBER == month)
            result = "Diciembre";

        return result;
    }

    @Create
    public void init() {
        restrictions = new String[]{"rawMaterialPayRoll.productiveZone = #{rawMaterialPayRollReportAction.zone}",
                                    "rawMaterialPayRoll.metaProduct = #{rawMaterialPayRollReportAction.metaProduct}"};
        sortProperty = "rawMaterialProducer.firstName";
    }

    private void getTotal(RawMaterialPayRoll rawMaterialPayRoll)
    {
        //rawMaterialPayRoll.
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

    @Factory(value = "monthEnumPayRoll")
    public Month[] getMonthEnum() {
        return Month.values();
    }

    @Factory(value = "periodosPayRoll", scope = ScopeType.STATELESS)
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

    public Calendar getDateIni() {
        return dateIni;
    }

    public void setDateIni(Calendar dateIni) {
        this.dateIni = dateIni;
    }

    public Calendar getDateEnd() {
        return dateEnd;
    }

    public void setDateEnd(Calendar dateEnd) {
        this.dateEnd = dateEnd;
    }

    public String getFullNameOfProductiveZone() {
        return (zone == null ? "" : zone.getFullName());
    }

    public void setFullNameOfProductiveZone(String fullName) {

    }

    protected GenericService getService() {
        return rawMaterialPayRollService;
    }

    public void selectProductiveZone(ProductiveZone productiveZone) {
        try {
            productiveZone = getService().findById(ProductiveZone.class, productiveZone.getId());
            setZone(productiveZone);
        } catch (Exception ex) {
            log.error("Caught Error", ex);
        }
    }


}