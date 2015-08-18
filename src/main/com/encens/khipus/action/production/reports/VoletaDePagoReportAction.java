package com.encens.khipus.action.production.reports;

import com.encens.khipus.action.reports.GenericReportAction;
import com.encens.khipus.action.reports.PageFormat;
import com.encens.khipus.action.reports.PageOrientation;
import com.encens.khipus.framework.service.GenericService;
import com.encens.khipus.model.employees.GeneratedPayrollType;
import com.encens.khipus.model.employees.Gestion;
import com.encens.khipus.model.employees.GestionPayroll;
import com.encens.khipus.model.employees.Month;
import com.encens.khipus.model.production.*;
import com.encens.khipus.reports.GenerationReportData;
import com.encens.khipus.service.production.BoletaPagoProductor;
import com.encens.khipus.service.production.ProductiveZoneService;
import com.encens.khipus.service.production.RawMaterialPayRollService;
import com.encens.khipus.service.production.RawMaterialPayRollServiceBean;
import com.encens.khipus.util.MessageUtils;
import com.encens.khipus.util.MoneyUtil;
import com.jatun.titus.reportgenerator.util.TypedReportData;
import net.sf.jasperreports.engine.JRPrintPage;
import net.sf.jasperreports.engine.JasperPrint;
import org.apache.commons.lang.time.FastDateFormat;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.*;
import org.joda.time.format.ISODateTimeFormat;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Encens S.R.L.
 * Action to generate payroll summary report by payment method and currency
 *
 * @author
 * @version $Id: SummaryPayrollByPaymentMethodReportAction.java  22-ene-2010 11:38:12$
 */
@Name("voletaDePagoReportAction")
@Scope(ScopeType.CONVERSATION)
//@Restrict("#{s:hasPermission('RAWMATERIALPAYSUMMARY','VIEW')}")
public class VoletaDePagoReportAction extends GenericReportAction {
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
    private Date startDate;
    private Date endDate;
    private Calendar dateIni;
    private Calendar dateEnd;
    private RawMaterialProducer rawMaterialProducer;

    private List<GestionPayroll> gestionPayrollList;


    private GeneratedPayrollType generatedPayrollType = GeneratedPayrollType.OFFICIAL;


    public void generateReport() throws ParseException {
        log.debug("Generate RotatoryFundReportAction........");

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy");
        dateIni = Calendar.getInstance();
        dateEnd = Calendar.getInstance();
        dateIni.set(gestion.getYear(), month.getValue(), periodo.getInitDay());
        dateEnd.set(gestion.getYear(), month.getValue(), periodo.getEndDay(month.getValue() + 1, gestion.getYear()));
        sdf.setCalendar(dateIni);
        sdf.setCalendar(dateEnd);
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");

        dateIni.set(gestion.getYear(), month.getValue(), periodo.getInitDay());
        dateEnd.set(gestion.getYear(), month.getValue(), periodo.getEndDay(month.getValue() + 1, gestion.getYear()));
        startDate = dateFormat.parse(dateFormat.format(dateIni.getTime()));
        endDate = dateFormat.parse(dateFormat.format(dateEnd.getTime()));

        List<BoletaPagoProductor> boletaPagoProductors = rawMaterialPayRollService.findBoletaDePago(startDate,endDate,rawMaterialProducer,zone,metaProduct);

        generarBoletas(boletaPagoProductors);
    }

    private void generarBoletas(List<BoletaPagoProductor> boletaPagoProductors){
        Map params = new HashMap();
        TypedReportData typedReportData;
        setParameters(params, boletaPagoProductors.get(0));
        super.setPageFormat(PageFormat.LETTER);
        params.put("REPORT_LOCALE", new java.util.Locale("en", "US"));
        typedReportData = super.getReport("voletaDePago"
                , "/production/reports/voletaDePago.jrxml"
                , "Boleta de Pago"
                , params);
        typedReportData.getJasperPrint().setPageHeight(419);
        typedReportData.getJasperPrint().setLocaleCode("US");
        for(int i=1;i<boletaPagoProductors.size();i++)
        {
            setParameters(params,boletaPagoProductors.get(i));
            params.put("REPORT_LOCALE", new java.util.Locale("en", "US"));
            TypedReportData boleta = super.getReport("voletaDePago"
                    , "/production/reports/voletaDePago.jrxml"
                    , "Boleta de Pago"
                    , params);
            typedReportData.getJasperPrint().getPages().addAll(boleta.getJasperPrint().getPages());
        }

        try {
            GenerationReportData generationReportData = new GenerationReportData(typedReportData);
            generationReportData.exportReport();
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

    }

    @Override
    protected String getEjbql() {
        return "";
    }

    @Create
    public void init() {
        this.month = Month.getMonth(new Date());
        Calendar end = Calendar.getInstance();
        end.setTime(new Date());
        if(end.get(Calendar.DAY_OF_MONTH) > 15)
            this.periodo = Periodo.SECONDPERIODO;
        else
            this.periodo = Periodo.FIRSTPERIODO;
    }

    private void setParameters(Map params,BoletaPagoProductor boleta){
        MoneyUtil moneyUtil = new MoneyUtil();
        DateFormat df = new SimpleDateFormat("dd/MM/yyyy");
        params.put("fecha",new Date());
        params.put("hora",new Date());
        String periodo = "PERIODO DEL " +df.format(dateIni.getTime())+" AL "+df.format(dateEnd.getTime());
        params.put("periodo",periodo);
        params.put("ci",boleta.getCi());
        params.put("nombreProductor",boleta.getNombrecompletoProductor());
        params.put("nombreGab",boleta.getNombreGAB());
        params.put("precioLeche",boleta.getPrecioLeche());
        params.put("totalBrutoBs",boleta.getTotalBrutoBs());
        params.put("totalLitrosLeche",boleta.getTotalLitrosLeche());
        params.put("credLeche",boleta.getTotalBrutoBs());
        params.put("debRetencion",boleta.getRetencion());
        params.put("debReserva",boleta.getReserva());
        params.put("debAlcohol",boleta.getAlcohol());
        params.put("debConcentrados",boleta.getConcentrados());
        params.put("debCredito",boleta.getCredito());
        params.put("debVeterinario",boleta.getVeterinario());
        params.put("debYogurt",boleta.getYogurt());
        params.put("debTachos",boleta.getTachos());
        if(boleta.getAjustes()<0.0) {
            params.put("debAjustes", boleta.getAjustes() * -1);
            params.put("credAjustes", 0.0);
        }
        else {
            params.put("credAjustes", boleta.getAjustes());
            params.put("debAjustes",0.0);
        }
        params.put("debOtrosDescuentos",boleta.getOtrosDescuentos());
        params.put("credOtrosIngresos",boleta.getOtrosIngresos());
        params.put("liquidoPagable",boleta.getLiquidoPagable());
        params.put("liquidoPagableLiteral",moneyUtil.Convertir(boleta.getLiquidoPagable().toString(), true));
    }

    private String getMes(Month month) {
        String result = "";
        if (Month.JANUARY == month)
            result = "Enero";
        if (Month.FEBRUARY == month)
            result = "Febrero";
        if (Month.MARCH == month)
            result = "Marzo";
        if (Month.APRIL == month)
            result = "Abril";
        if (Month.MAY == month)
            result = "Mayo";
        if (Month.JUNE == month)
            result = "Junio";
        if (Month.JULY == month)
            result = "Julio";
        if (Month.AUGUST == month)
            result = "Agosto";
        if (Month.SEPTEMBER == month)
            result = "Septiembre";
        if (Month.OCTOBER == month)
            result = "Octubre";
        if (Month.NOVEMBER == month)
            result = "Noviembre";
        if (Month.DECEMBER == month)
            result = "Diciembre";

        return result;
    }

    private void getTotal(RawMaterialPayRoll rawMaterialPayRoll) {
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

    @Factory(value = "monthEnumBoleta")
    public Month[] getMonthEnum() {
        return Month.values();
    }

    @Factory(value = "periodosBoleta", scope = ScopeType.STATELESS)
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

    public void selectRawMaterialProducer(RawMaterialProducer producer) {
        try {
            producer = getService().findById(RawMaterialProducer.class, producer.getId());
            setRawMaterialProducer(producer);
        } catch (Exception ex) {
            log.error("Caught Error", ex);
        }
    }

    public TypedReportData getbyGABReportTypedReportData() {
        log.debug("Generate RotatoryFundReportAction........");
        String subReportKey = "RAWMATERIALCOLLECTEDBYGABREPORT";
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy");
        dateIni = Calendar.getInstance();
        dateEnd = Calendar.getInstance();
        dateIni.set(gestion.getYear(), month.getValue(), periodo.getInitDay());
        dateEnd.set(gestion.getYear(), month.getValue(), periodo.getEndDay(month.getValue() + 1, gestion.getYear()));
        sdf.setCalendar(dateIni);
        sdf.setCalendar(dateEnd);
        DateFormat df = new SimpleDateFormat("dd/MM/yyyy");
        Map params = new HashMap();

        //params.put("reportTitle",messages.get("Report.titleGeneral"));
        params.put("title", messages.get("Report.titleGeneral"));
        params.put("header", messages.get("Report.header.collectedByGAB"));
        params.put("periodo", (periodo.getResourceKey().toString() == "Periodo.first") ? "1RA QUINCENA" : "2DA QUINCENA" + " " + getMes(month));
        params.put("startDate", df.format(dateIni.getTime()));
        params.put("endDate", df.format(dateEnd.getTime()));
        params.put("nombre_gab", "GAB: " + zone.getNumber() + " - " + zone.getName());

        boolean isFirst = (periodo.getResourceKey().toString() == "Periodo.first") ? true : false;

        String fileReport = (isFirst) ? "rawMaterialCollectedByGAB1daReport.jrxml" : "rawMaterialCollectedByGAB2daReport.jrxml";

        int cont = periodo.getInitDay();
        for (int i = periodo.getInitDay(); i <= (isFirst?15:31); i++) {

            params.put("DAY" + cont, "D" + i);
            cont++;
        }

        if (cont <= 31 && isFirst) {
            for (int i = cont; i <= 31; i++) {
                params.put("DAY" + cont, "D" + i);
                cont++;
            }
        }

        params.put("dateStart", "Fecha Inicio - " + FastDateFormat.getInstance("dd-MM-yyyy").format(dateIni));
        params.put("dateEnd", "Fecha Fin - " + FastDateFormat.getInstance("dd-MM-yyyy").format(dateEnd));

        return super.getReport(
                subReportKey,
                "/production/reports/" + fileReport,
                //getSqlCollected(),
                getSqlOld(),
                params,
                "rotatoryFundReport");

    }

    protected String getSqlNew() {

        int initDay = periodo.getInitDay();
        int endDay = periodo.getEndDay(month.getValue() + 1, gestion.getYear());
        int month_act = (month.getValue()) + 1;
        int cont = 1;
        String sql = "select am.cantidad , pe.nombres||' '||pe.apellidopaterno ||' '||pe.apellidomaterno as productor \n";
        for (int i = initDay; i <= endDay; i++) {
            sql += "       , (case sa.fecha \n" +
                    "             when to_date('" + i + "/" + month_act + "/" + gestion.getYear() + "','dd/mm/yyyy') \n" +
                    "             then am.cantidad\n" +
                    "             end) as D" + cont + "\n";
            cont++;
        }
        sql += "  from sesionacopio sa\n" +
                "                              inner join acopiomateriaprima am\n" +
                "                              on am.idsesionacopio = sa.idsesionacopio\n" +
                "                              inner join persona pe \n" +
                "                              on am.IDPRODUCTORMATERIAPRIMA=pe.idpersona \n" +
                "                              inner join zonaproductiva zp\n" +
                "                              on zp.idzonaproductiva = sa.idzonaproductiva\n" +
                "                              where sa.fecha between to_date('" + initDay + "/" + month_act + "/" + gestion.getYear() + "','dd/mm/yyyy') and to_date('" + endDay + "/" + month_act + "/" + gestion.getYear() + "','dd/mm/yyyy') \n" +
                "                              AND zp.idzonaproductiva = " + zone.getId().toString() + "\n" +
                "                              order by sa.fecha asc\n";

        return sql;
    }

    private String getSqlCollected(){

        int initDay = periodo.getInitDay();
        int endDay = periodo.getEndDay(month.getValue() + 1, gestion.getYear());
        int cont = 1;
        int cantDays = 1;
        String sql = "";
        int month_act = (month.getValue()) + 1;
        sql += "select productor\n" ;

        for (int i = initDay; i <= endDay; i++) {
            //sql += ((cont == 1) ? "" : " , ")+"max(decode(fecha,to_date('" + i + "/" + month_act + "/" + gestion.getYear() + "','dd/mm/yyyy'),cantidad,0)) AS D"+cont+"\n";
            sql += ", max(decode(fecha,to_date('" + i + "/" + month_act + "/" + gestion.getYear() + "','dd/mm/yyyy'),cantidad,0)) AS D"+cont+"\n";
            cont++;
            cantDays ++;
        }
        cont = 1;
        if (periodo.getResourceKey().toString().compareTo("Periodo.first") == 0) {
            sql += ", 0 AS D16 \n";
        }
        else{
            for(int i= cantDays ; i<=17;i++){
                sql += ", 0 as D"+i+"\n";
            }
        }
        for (int i = initDay; i <= endDay; i++) {
            sql += ((cont == 1) ? "," : " + ")+"max(cantidad)\n";
            cont++;
        }

        sql += "       TOTAL\n" +
                "from (\n" +
                "select pe.nombres||' '||pe.apellidopaterno||' '||pe.apellidomaterno as productor ,am.cantidad as cantidad ,sa.fecha as fecha from acopiomateriaprima am\n" +
                "inner join sesionacopio sa\n" +
                "on am.idsesionacopio =sa.idsesionacopio\n" +
                "inner join persona pe\n" +
                "on pe.idpersona = am.idproductormateriaprima\n" +
                "where sa.fecha between to_date('" + initDay + "/" + month_act + "/" + gestion.getYear() + "') and to_date('" + endDay + "/" + month_act + "/" + gestion.getYear() + "','dd/mm/yyyy')\n" +
                "and sa.idzonaproductiva = "+ zone.getId().toString() + "\n" +
                "and am.cantidad <> 0\n" +
                "order by sa.fecha\n" +
                ")\n" +
                "group by productor\n" +
                "order by productor asc";
        return sql;
    }


    private String getSqlOld() {

        int initDay = periodo.getInitDay();
        int endDay = periodo.getEndDay(month.getValue() + 1, gestion.getYear());
        String sql = "SELECT \tp.productor AS productor, \n";
        int cont = 1;
        for(int d=initDay;d<=endDay;d++ )
        {
            sql +=" IFNULL(d"+cont+".cantidad, 0) D"+cont+",\n";
            cont ++;
        }
        if (periodo.getResourceKey().toString().equals("Periodo.first")) {
            sql +=" 0.0 D16,\n";
        }
        cont = 1;
        //sql += " IFNULL(d"+cont+".cantidad, 0) ";
        for(int d=initDay+1;d<=endDay+1;d++)
        {
            sql += " +IFNULL(d"+cont+".cantidad, 0) ";
            cont++;
        }
        sql +=" TOTAL \n" + " FROM\n" +
                "(\tSELECT pm.`idzonaproductiva`, pm.`idproductormateriaprima`, CONCAT(pe.`apellidopaterno`,' ',pe.`apellidomaterno`,' ',pe.`nombres`) AS productor, 0 AS cantidad\n" +
                "\tFROM productormateriaprima pm                             \n" +
                "\tLEFT JOIN persona pe ON pm.`idproductormateriaprima` = pe.idpersona\n" +
                ")p\n";
        int month_act = (month.getValue()) + 1;
        cont = 1;
        for(int d=initDay;d<=endDay;d++)
        {
            sql +="LEFT JOIN \n" +
                    "\t(\tSELECT sa.`fecha`, sa.`idzonaproductiva`, am.`idproductormateriaprima`, am.`cantidad`\n" +
                    "\t\tFROM acopiomateriaprima am\n" +
                    "\t\t\tLEFT JOIN sesionacopio sa ON am.`idsesionacopio` \t  = sa.`idsesionacopio`\n" +
                    "\t\t\tLEFT JOIN persona pe      ON am.`idproductormateriaprima` = pe.`idpersona`\n" +
                    "\t\tWHERE sa.`fecha` = '"+gestion.getYear()+"-"+month_act+"-"+d+"'\n" +
                    "\t) d"+cont+" \tON p.idproductormateriaprima = d"+cont+".idproductormateriaprima\n";
            cont++;
        }
        sql +=" WHERE p.idzonaproductiva = " + zone.getId().toString();

        return sql;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public RawMaterialProducer getRawMaterialProducer() {
        return rawMaterialProducer;
    }

    public void setRawMaterialProducer(RawMaterialProducer rawMaterialProducer) {
        this.rawMaterialProducer = rawMaterialProducer;
    }

    public void clearRawMaterialProducer() {
        rawMaterialProducer = null;
    }
}