package com.encens.khipus.action.production.reports;

import com.encens.khipus.action.production.ProductionPlanningAction;
import com.encens.khipus.action.reports.GenericReportAction;
import com.encens.khipus.action.reports.PageFormat;
import com.encens.khipus.action.reports.PageOrientation;
import com.encens.khipus.action.reports.ReportFormat;
import com.encens.khipus.model.admin.User;
import com.encens.khipus.model.production.*;
import com.encens.khipus.model.warehouse.ProductItem;
import com.encens.khipus.model.warehouse.Warehouse;
import com.encens.khipus.reports.GenerationReportData;
import com.encens.khipus.service.production.ProductionPlanningService;
import com.encens.khipus.service.warehouse.ProductItemService;
import com.encens.khipus.service.warehouse.WarehouseService;
import com.encens.khipus.util.MessageUtils;
import com.encens.khipus.util.RoundUtil;
import com.jatun.titus.reportgenerator.util.TypedReportData;
import net.sf.jasperreports.engine.JRPrintPage;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.fill.JRTemplatePrintText;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.security.Restrict;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Encens S.R.L.
 * This class implements the valued warehouse residue report action
 *
 * @author
 * @version 2.3
 */

@Name("summaryMaterialAndInputByDayReportAction")
@Scope(ScopeType.PAGE)
@Restrict("#{s:hasPermission('GENERATEREQUESTBYPLANNIG','VIEW')}")
public class SummaryMaterialAndInputByDayReportAction extends GenericReportAction {
    private ProductionPlanning planning;

    public void generateReport(ProductionPlanning productionPlanning) {
        log.debug("Generating IncomeByInvoiceReportAction............................");
        planning =  productionPlanning;
        TypedReportData reportInPuts;
        TypedReportData reportMaterial;
        //add sub reports
        setReportFormat(ReportFormat.PDF);

        reportMaterial = getSummaryMaterialSubReport();
        reportInPuts = getSummaryInputSubReport();
        for (Object jrPrintPage : reportInPuts.getJasperPrint().getPages()) {
            reportMaterial.getJasperPrint().addPage((JRPrintPage) jrPrintPage);
        }

        try {
            GenerationReportData generationReportData = new GenerationReportData(reportMaterial);
            generationReportData.exportReport();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private TypedReportData getSummaryInputSubReport() {
        log.debug("Generating addSummaryMaterialSubReport.............................!!!!!!!");
        Map<String, Object> params = new HashMap<String, Object>();

        String subReportKey = "ORDERINPUTSUBREPORT";

        String ejbql = " SELECT orderInput.productItemCode " +
                " ,orderInput.productItem.name " +
                " ,orderInput.productItem.usageMeasureCode " +
                " ,sum(orderInput.amount)  " +
                " from ProductionOrder productionOrder" +
                " inner join productionOrder.orderInputs orderInput" ;


        String[] restrictions = new String[]{
                "productionOrder.productionPlanning = #{summaryMaterialAndInputByDayReportAction.planning}"};

        String materialsSubReportOrderBy = "orderInput.productItem.name";

        String materialsSubReportGroupBy = "orderInput.productItemCode, orderInput.productItem.name ,orderInput.productItem.usageMeasureCode";

        //generate the sub report
        TypedReportData materialReport = super.getReport(
                subReportKey,
                "/production/reports/requestInputByPlannig.jrxml",
                PageFormat.LETTER,
                PageOrientation.LANDSCAPE,
                createQueryForSubreport(subReportKey, ejbql, Arrays.asList(restrictions), materialsSubReportOrderBy,materialsSubReportGroupBy),
                messages.get("ProductionPlanning.report.summaryInputByPlannig"),
                params);

        return materialReport;

    }

    private TypedReportData getSummaryMaterialSubReport() {
        log.debug("Generating addSummaryMaterialSubReport.............................!!!!!!!");
        Map<String, Object> params = new HashMap<String, Object>();

        String subReportKey = "ORDERMATERIALSUBREPORT";

        String ejbql = " SELECT orderMaterial.productItemCode " +
                       " ,orderMaterial.productItem.name " +
                       " ,orderMaterial.productItem.usageMeasureCode " +
                       " ,sum(orderMaterial.amountRequired)  " +
                       " from ProductionOrder productionOrder " +
                       " inner join productionOrder.orderMaterials orderMaterial";


        String[] restrictions = new String[]{
                "productionOrder.productionPlanning = #{summaryMaterialAndInputByDayReportAction.planning}"};

        String materialsSubReportOrderBy = "orderMaterial.productItem.name";

        String materialsSubReportGroupBy = "orderMaterial.productItemCode, orderMaterial.productItem.name ,orderMaterial.productItem.usageMeasureCode";

        //generate the sub report
        TypedReportData materialReport = super.getReport(
                subReportKey,
                "/production/reports/requestMaterialByPlannig.jrxml",
                PageFormat.LETTER,
                PageOrientation.LANDSCAPE,
                createQueryForSubreport(subReportKey, ejbql, Arrays.asList(restrictions), materialsSubReportOrderBy,materialsSubReportGroupBy),
                messages.get("ProductionPlanning.report.summaryMaterialByPlannig"),
                params);

        return materialReport;

    }

    public ProductionPlanning getPlanning() {
        return planning;
    }

    public void setPlanning(ProductionPlanning planning) {
        this.planning = planning;
    }
}
