package com.encens.khipus.action.production.reports;

import com.encens.khipus.action.SessionUser;
import com.encens.khipus.action.production.ProductionPlanningAction;
import com.encens.khipus.action.reports.GenericReportAction;
import com.encens.khipus.action.reports.PageFormat;
import com.encens.khipus.action.reports.PageOrientation;
import com.encens.khipus.action.reports.ReportFormat;
import com.encens.khipus.model.admin.User;
import com.encens.khipus.model.production.ProductComposition;
import com.encens.khipus.model.production.ProductionOrder;
import com.encens.khipus.model.production.ProductionPlanning;
import com.encens.khipus.model.warehouse.WarehouseVoucher;
import com.encens.khipus.reports.GenerationReportData;
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
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.log.Log;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    @In
    private SessionUser sessionUser;

    private Map<String, Object> commonDocumentParamsInfo;
    private ProductionPlanning productionPlanning;
    private List<ProductionOrder> productionOrders;
    private List<ProductionPlanningAction.Consolidated> consolidateds;
    private ProductComposition productComposition;

    public void generateReport(List<ProductionPlanningAction.Consolidated> consolidateds,List<ProductionOrder> productionOrders ) {
        log.debug("Generate ProductionPlannigReportAction........");
        TypedReportData typedReportData;
        //setWarehouseVoucher(getEntityManager().find(WarehouseVoucher.class, warehouseVoucher.getId()));
        String templatePath = "/production/reports/productionPlanningReport.jrxml";
        String fileName = "";

        Map params = new HashMap();

        params.putAll(getCommonDocumentParamsInfo());
        consolidateds = consolidateds;
        productionOrders = productionOrders;

        JRDesignStaticText staticText = new JRDesignStaticText();
        staticText.setX(75);
        staticText.setY(83);
        staticText.setWidth(100);
        staticText.setHeight(20);
        staticText.setMode(ModeEnum.OPAQUE);
        staticText.setHorizontalAlignment(HorizontalAlignEnum.LEFT);
        staticText.setText("AQUIIIIIIIIIIII ");
        staticText.getLineBox().getLeftPen().setLineWidth(1);
        staticText.getLineBox().getTopPen().setLineWidth(1);
        staticText.getLineBox().setLeftPadding(10);

        setReportFormat(ReportFormat.PDF);
        //add sub report
        //addProductionPlannnigSubReport(params);
        //typedReportData = super.getReport("productionPlanning", templatePath, PageFormat.LETTER, PageOrientation.PORTRAIT, fileName, params);
        //value = (java.lang.Double)((Double)(((java.util.List<ProductionPlanningAction.Consolidated>)consolidateds.getValue()).get(((java.lang.Integer)parameter_cont.getValue())).getAmount()));
        typedReportData = getReport("productionPlanning", templatePath, MessageUtils.getMessage("Report.rawMaterialPayRollReportAction"), params);

        JasperPrint jasperPrint = typedReportData.getJasperPrint();

        ((JRTemplatePrintText)(((JRPrintPage)(typedReportData.getJasperPrint().getPages().get(0))).getElements().get(20))).setText("AQUIII");

       /* try {
            jasperPrint = JasperFillManager.fillReport( "prueba", params, new JRBeanCollectionDataSource(consolidateds));
        } catch (JRException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }*/

        //JasperFillManager.fillReport( fileName, parameters, new JRBeanCollectionDataSource(list));
        /*List pages = jasperPrint.getPages();
        JRPrintPage p = (JRPrintPage)pages.get(0);*/

        //typedReportData.getReportData().getJasperReport().getDetailSection().getBands()[0].getChildren().add(staticText);
        //typedReportData.getReportData().getJasperReport().getDetailSection().getBands()[0] = staticText;
        //(JRDesignSection)(typedReportData.getReportData().getJasperReport().getDetailSection()).addBand(null);
        //JasperReport jasperReport = new JasperReport();
        //typedReportData.setJasperReport();
        JasperDesign jasperDesign = typedReportData.getJasperDesign();
        /*JRBaseStaticText texto = (JRBaseStaticText)typedReportData.getJasperReport().getColumnHeader().getChildren().get(7);
        texto.setText("mirame..");
        texto.setX(75);
        texto.setWidth(168);
        typedReportData.getJasperReport().getColumnHeader().getChildren().add(texto);*/

     /*   try {
            jasperDesign.addGroup(new JRDesignGroup());
        } catch (JRException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }*/

        /*for(int j =0; j < el.size(); j++)
           {
                JRPrintElement e = ((JRPrintElement)el.get(j));
                if(e.getKey().equals("Detail 1"))
                        height = e.getHeight();
           }*/

        try {
            typedReportData.setJasperPrint(jasperPrint);
            GenerationReportData generationReportData = new GenerationReportData(typedReportData);
            generationReportData.exportReport();
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    public Map<String, Object> getCommonDocumentParamsInfo() {

        Map<String, Object> paramMap = new HashMap<String, Object>();
        paramMap.put("userLoginParam", currentUser.getEmployee().getFullName());
        paramMap.put("reportTitle", "titulo");
        paramMap.put("dateParam", "fecha");
        paramMap.put("estate", "estado");
        return paramMap;
    }

    @Override
    protected String getEjbql() {

        return  " SELECT distinct  metaProduct.name, " +
                "                   metaProduct.code " +
                " FROM ProductionOrder productionOrder " +
                " inner join ProductionOrder.productComposition productComposition " +
                " inner join productComposition.productionIngredientList productionIngredient " +
                " inner join productionIngredient.metaProduct metaProduct ";
        //return "SELECT NEW ProductionPlanningAction.Consolidated(productionOrder.amount,productionOrder.product) FROM #{productionOrders} as productionOrder ";
       // return "";
    }

    @Create
    public void init() {
        restrictions = new String[]{"productionOrder = #{productionOrders}"};
    }

}