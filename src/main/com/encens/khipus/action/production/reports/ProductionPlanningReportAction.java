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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    @In
    private SessionUser sessionUser;
    @In
    private EvaluatorMathematicalExpressionsService evaluatorMathematicalExpressionsService;
    @In
    private MetaProductService metaProductService;

    private Map<String, Object> commonDocumentParamsInfo;
    private ProductionPlanning productionPlanning;
    private List<ProductionOrder> productionOrders;
    private List<ProductionPlanningAction.Consolidated> consolidateds;
    private List<MetaProduct> metaProducts;
    private ProductComposition productComposition;
    private ArrayList<Long> metadIds = new ArrayList<Long>();
    private Map<Long, ProductionPlanningAction.Consolidated> datas;
    private ArrayList<Double> mounts = new ArrayList<Double>();
    private ArrayList<Long> ids = new ArrayList<Long>();

    public void generateReport(List<MetaProduct> products,List<ProductionOrder> orders ) {
        log.debug("Generate ProductionPlannigReportAction........");
        TypedReportData typedReportData;
        String templatePath = "/production/reports/productionPlanningReport.jrxml";
        String fileName = "ProductionPlanningReportAction";

        Map params = new HashMap();

        metaProducts = products;

        params.putAll(getCommonDocumentParamsInfo());
        //consolidateds = consolis;
        productionOrders = orders;
        metaProducts.clear();
        consolidateds = getConsolidatedInputs();
        //getMetaProducts(consolidateds);

        for(Map.Entry<Long, ProductionPlanningAction.Consolidated> entry : datas.entrySet())
        {
            //metaProducts.add(metaProductService.find(entry.getKey()));
            ids.add(entry.getKey());
        }

        setReportFormat(ReportFormat.PDF);
        //typedReportData = getReport("productionPlanning", templatePath, MessageUtils.getMessage("Report.rawMaterialPayRollReportAction"), params);
        String query = " select nombre, codigo " +
                       " from metaproductoproduccion " +
                       " where idmetaproductoproduccion in ( ";
        boolean band = true;
        for(Long id: ids)
        {
           query += (band?" ":",") + id.toString();
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
        int cont = 10;
        for(ProductionPlanningAction.Consolidated consolidated:consolidateds)
        {
            ((JRTemplatePrintText)(((JRPrintPage)(typedReportData.getJasperPrint().getPages().get(0))).getElements().get(cont))).setText(String.format("%.2f", consolidated.getAmount()));
            cont +=3;
        }
        try {
            typedReportData.setJasperPrint(jasperPrint);
            GenerationReportData generationReportData = new GenerationReportData(typedReportData);
            generationReportData.exportReport();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<ProductionPlanningAction.Consolidated> getConsolidatedInputs() {
        try {

            datas = new HashMap<Long, ProductionPlanningAction.Consolidated>();
            for (ProductionOrder order : productionOrders) {
                evaluatorMathematicalExpressionsService.executeMathematicalFormulas(order);
                for (ProductionIngredient ingredient : order.getProductComposition().getProductionIngredientList()) {
                    ProductionPlanningAction.Consolidated aux = datas.get(ingredient.getMetaProduct().getId());
                    if (aux == null) {
                        aux = new ProductionPlanningAction.Consolidated();
                        aux.setProduct(ingredient.getMetaProduct());
                        datas.put(ingredient.getMetaProduct().getId(), aux);
                    }
                    aux.setAmount(aux.getAmount() + ingredient.getAmount());
                }
            }
            return new ArrayList<ProductionPlanningAction.Consolidated>(datas.values());
        } catch (Exception ex) {
            log.error("Exception caught", ex);
            return new ArrayList<ProductionPlanningAction.Consolidated>();
        }
    }

    private void getMetaProducts(List<ProductionPlanningAction.Consolidated> consolidatedList) {

             for(ProductionPlanningAction.Consolidated consolidated:consolidatedList)
             {
                 metaProducts.add(consolidated.getProduct());
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

    /*@Override
    protected String getEjbql() {

        return  " SELECT distinct  metaProduct.name, " +
                "                   metaProduct.code " +
                " FROM ProductionOrder productionOrder " +
                " inner join ProductionOrder.productComposition productComposition " +
                " inner join productComposition.productionIngredientList productionIngredient " +
                " inner join productionIngredient.metaProduct metaProduct ";
        //return "SELECT NEW ProductionPlanningAction.Consolidated(productionOrder.amount,productionOrder.product) FROM #{productionOrders} as productionOrder ";
       // return "";
    }*/

/*    @Create
    public void init() {
        restrictions = new String[]{"productionOrder = #{productionOrders}"};
    }*/

}