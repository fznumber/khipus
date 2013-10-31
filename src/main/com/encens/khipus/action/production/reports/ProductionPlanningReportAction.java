package com.encens.khipus.action.production.reports;

import com.encens.khipus.action.SessionUser;
import com.encens.khipus.action.production.ProductionPlanningAction;
import com.encens.khipus.action.reports.GenericReportAction;
import com.encens.khipus.action.reports.PageFormat;
import com.encens.khipus.action.reports.PageOrientation;
import com.encens.khipus.action.reports.ReportFormat;
import com.encens.khipus.model.admin.User;
import com.encens.khipus.model.production.*;
import com.encens.khipus.model.warehouse.WarehouseVoucher;
import com.encens.khipus.reports.GenerationReportData;
import com.encens.khipus.service.production.EvaluatorMathematicalExpressionsService;
import com.encens.khipus.service.production.MetaProductService;
import com.encens.khipus.util.MessageUtils;
import com.jatun.titus.reportgenerator.util.TypedReportData;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.base.JRBaseBand;
import net.sf.jasperreports.engine.base.JRBaseStaticText;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.design.JRDesignGroup;
import net.sf.jasperreports.engine.design.JRDesignSection;
import net.sf.jasperreports.engine.design.JRDesignStaticText;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.fill.JRTemplatePrintText;
import net.sf.jasperreports.engine.type.HorizontalAlignEnum;
import net.sf.jasperreports.engine.type.ModeEnum;
import org.apache.poi.hssf.record.formula.functions.T;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.log.Log;

import java.io.IOException;
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
@Name("productionPlanningReportAction")
@Scope(ScopeType.CONVERSATION)
public class ProductionPlanningReportAction extends GenericReportAction {

    @In
    User currentUser;
    private List<ProductionPlanningAction.Consolidated> consolidatedsIN;
    private List<ProductionOrder> productionOrders;
    private ProductionPlanning productionPlanning;


    private String date;
    private String state;

    /*public void generateReport(List<ProductionPlanningAction.Consolidated> consolidatedLists,ProductionPlanning productionPlan,List<ProductionOrder> orders) {
        log.debug("Generating productionOrderPlanningDetailSubReport............................");
        productionOrders = orders;
        consolidatedsIN = consolidatedLists;
        productionPlanning = productionPlan;
        Map params = new HashMap();
        setReportFormat(ReportFormat.PDF);
        params.putAll(getCommonDocumentParamsInfo());
        //add sub reports
        addProductionOrderPlanningDetailSubReport(params);

        super.generateSqlReport("incomeByInvoiceReport", "/production/reports/productionPlanningReportprub.jrxml", "titulo prueba", params);
    }*/

    private void addProductionOrderPlanningDetailSubReport(Map mainReportParams) {
        log.debug("Generating productionOrderPlanningDetailSubReport.............................");

        Map<String, Object> params = new HashMap<String, Object>();

        String ejbql = "SELECT " +
                " productionOrder.productComposition.processedProduct.name, " +
                " productionOrder.productComposition.processedProduct.code, " +
                " productionOrder.code, " +
                " productionOrder.productComposition.name, " +
                " productionOrder.supposedAmount, " +
                " productionOrder.producingAmount " +
                " FROM ProductionOrder productionOrder " ;

        String sql = "SELECT mp.nombre as nombre ,mp.codigo as codigo , op.codigo as codigo_orden, cp.nombre as formula, op.TEORICOOBTENIDO as teorico, op.CANTIDADPRODUCIR as producido \n" +
                "FROM ordenproduccion OP\n" +
                "INNER JOIN composicionproducto CP\n" +
                "ON cp.idcomposicionproducto = op.idcomposicionproducto\n" +
                "INNER JOIN productoprocesado PP\n" +
                "ON pp.idproductoprocesado = cp.idproductoprocesado\n" +
                "INNER JOIN metaproductoproduccion MP\n" +
                "ON mp.idmetaproductoproduccion = pp.idproductoprocesado\n" +
                "WHERE op.idordenproduccion IN (";

        boolean band = true;
        for(ProductionOrder productionOrder: productionOrders)
        {
            sql += (band?" ":",") + productionOrder.getId().toString();
            band = false;
        }
        sql += " )";


        String[] restrictions = new String[]{
                "productionOrder = #{productionOrders}",
        };

        String orderBy = "";
        //generate the sub report
        String subReportKey = "INCOMEBYCONCEPTSUBREPORT";

        TypedReportData subReportData = super.generateSqlSubReport(
                subReportKey,
                "/production/reports/productionOrderPlanningDetailSubReport.jrxml",
                PageFormat.LETTER,
                PageOrientation.PORTRAIT,
                sql,
                params);

        //add in main report params
        mainReportParams.putAll(subReportData.getReportParams());
        mainReportParams.put(subReportKey, subReportData.getJasperReport());
    }

    public void generateReport(List<ProductionPlanningAction.Consolidated> consolidatedLists,ProductionPlanning productionPlanning,List<ProductionOrder> orders) {
        log.debug("Generate ProductionPlannigReportAction........");
        productionOrders = orders;
        TypedReportData typedReportData;
        String templatePath = "/production/reports/productionPlanningReportprub.jrxml";
        String fileName = "ProductionPlanningReportAction";
        SimpleDateFormat sdf=new java.text.SimpleDateFormat("dd/MM/yyyy");
        date = sdf.format(productionPlanning.getDate());
        state  = getEstate(productionPlanning.getState());
        Map params = new HashMap();

        params.putAll(getCommonDocumentParamsInfo());
        consolidatedsIN = consolidatedLists;
        setReportFormat(ReportFormat.PDF);

        addProductionOrderPlanningDetailSubReport(params);

        String query = " select nombre, codigo, cod_med  " +
                       " from metaproductoproduccion mp " +
                       " inner join WISE.inv_articulos ia " +
                       " on ia.cod_art = mp.codigo " +
                       " where idmetaproductoproduccion in ( ";
        boolean band = true;
        for(ProductionPlanningAction.Consolidated consolidated: consolidatedsIN)
        {
           query += (band?" ":",") + consolidated.getIdMeta().toString();
           band = false;
        }
        query += " )";

        typedReportData = getReport(
                fileName
                , templatePath
                , query
                , params
                , "productionPlanningReport"
        );

        JasperPrint jasperPrint = typedReportData.getJasperPrint();

        for(int i =0; i<typedReportData.getJasperPrint().getPages().size();i++)
        {
            int contName = 7;
            int contUnidad = 9;
            int contCod = 8;
            int cantidad = 10;

            for(ProductionPlanningAction.Consolidated consolidated:consolidatedsIN)
            {
                ((JRTemplatePrintText)(((JRPrintPage)(typedReportData.getJasperPrint().getPages().get(i))).getElements().get(contName))).setText(consolidated.getName());
                ((JRTemplatePrintText)(((JRPrintPage)(typedReportData.getJasperPrint().getPages().get(i))).getElements().get(contUnidad))).setText(consolidated.getUnit());
                ((JRTemplatePrintText)(((JRPrintPage)(typedReportData.getJasperPrint().getPages().get(i))).getElements().get(contCod))).setText(consolidated.getCode());
                ((JRTemplatePrintText)(((JRPrintPage)(typedReportData.getJasperPrint().getPages().get(i))).getElements().get(cantidad))).setText(String.format("%.2f", consolidated.getAmount()));
                contName += 4;
                contUnidad += 4;
                contCod +=4;
                cantidad +=4;
            }
        }
        try {
            typedReportData.setJasperPrint(jasperPrint);
            GenerationReportData generationReportData = new GenerationReportData(typedReportData);
            generationReportData.exportReport();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*private TypedReportData addProductionOrderPlanningDetailSubReport() {
        log.debug("Generating productionOrderPlanningDetailSubReport.............................");

        Map<String, Object> params = new HashMap<String, Object>();

        String ejbql = "SELECT " +
                " productionOrder.productComposition.processedProduct.name, " +
                " productionOrder.productComposition.processedProduct.code, " +
                " productionOrder.code, " +
                " productionOrder.productComposition.name, " +
                " productionOrder.supposedAmount, " +
                " productionOrder.producingAmount " +
                " FROM ProductionOrder productionOrder " ;

        String sql = "SELECT mp.nombre ,mp.codigo, op.codigo, cp.nombre, op.TEORICOOBTENIDO, op.CANTIDADPRODUCIR \n" +
                "FROM ordenproduccion OP\n" +
                "INNER JOIN composicionproducto CP\n" +
                "ON cp.idcomposicionproducto = op.idcomposicionproducto\n" +
                "INNER JOIN productoprocesado PP\n" +
                "ON pp.idproductoprocesado = cp.idproductoprocesado\n" +
                "INNER JOIN metaproductoproduccion MP\n" +
                "ON mp.idmetaproductoproduccion = pp.idproductoprocesado\n" +
                "WHERE op.idordenproduccion IN (";

        boolean band = true;
        for(ProductionOrder productionOrder: productionOrders)
        {
            sql += (band?" ":",") + productionOrder.getId().toString();
            band = false;
        }
        sql += " )";


        String[] restrictions = new String[]{
                "productionOrder = #{productionOrders}",
        };

        String orderBy = "";
        //generate the sub report
        String subReportKey = "PRODUCTIONPLANINGSUBREPORT";

       *//*return super.generateSubReport(
                subReportKey,
                "/production/reports/productionOrderPlanningDetailSubReportJPA.jrxml",
                PageFormat.LETTER,
                PageOrientation.PORTRAIT,
                //createQueryForSubreport(subReportKey, ejbql, null, orderBy),
                createQueryForSubreport(subReportKey, ejbql, new ArrayList(),""),
                params);*//*

        return super.generateSqlSubReport(
                subReportKey,
                "/production/reports/productionOrderPlanningDetailSubReport.jrxml",
                PageFormat.LETTER,
                PageOrientation.PORTRAIT,
                sql,
                params);

        //add in main report params
        //mainReportParams.putAll(subReportData.getReportParams());
        //mainReportParams.put(subReportKey, subReportData.getJasperReport());

    }
    */
    /*
    @Override
    protected String getEjbql()
    {
        " select nombre, codigo, cod_med  " +
                " from metaproductoproduccion mp " +
                " inner join WISE.inv_articulos ia " +
                " on ia.cod_art = mp.codigo " +
                " where idmetaproductoproduccion in (
        String sql = " SELECT metaProduct.name, metaProduct.code   " +
                " FROM MetaProduct metaProduct  " +
                " inner join ";
        return sql;
    }
    */

/*
    @Create
    public void init() {
        restrictions = new String[]{""
        };

        sortProperty = "";
    }*/

    private String getEstate(ProductionPlanningState statePlaning)
    {
        String estateLiteral = "";

        if(statePlaning == ProductionPlanningState.EXECUTED)
            estateLiteral = MessageUtils.getMessage("ProductionPlanning.makeExecuted");

        if(statePlaning == ProductionPlanningState.FINALIZED)
            estateLiteral = MessageUtils.getMessage("productionPlanningAction.makeFinalized");

        if(statePlaning == ProductionPlanningState.PENDING)
            estateLiteral = MessageUtils.getMessage("ProductionPlanning.makePending");

        return estateLiteral;
    }

    public Map<String, Object> getCommonDocumentParamsInfo() {

        Map<String, Object> paramMap = new HashMap<String, Object>();
        paramMap.put("position", currentUser.getRoles().get(0).getName());
        paramMap.put("userLoginParam", currentUser.getEmployee().getFullName());
        paramMap.put("reportTitle", MessageUtils.getMessage("ProductionPlanning.orderInputOrMaterial"));
        paramMap.put("dateParam", date);
        paramMap.put("estate", state);
        return paramMap;
    }

}