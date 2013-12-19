package com.encens.khipus.action.customers.reports;

import com.encens.khipus.action.production.ProductionPlanningAction;
import com.encens.khipus.action.reports.GenericReportAction;
import com.encens.khipus.action.reports.PageFormat;
import com.encens.khipus.action.reports.PageOrientation;
import com.encens.khipus.action.reports.ReportFormat;
import com.encens.khipus.model.admin.User;
import com.encens.khipus.model.production.*;
import com.encens.khipus.reports.GenerationReportData;
import com.encens.khipus.util.MessageUtils;
import com.encens.khipus.util.RoundUtil;
import com.jatun.titus.reportgenerator.util.TypedReportData;
import net.sf.jasperreports.engine.JRPrintPage;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.fill.JRTemplatePrintText;
import net.sf.jasperreports.engine.fill.JRTemplateText;
import net.sf.jasperreports.engine.type.HorizontalAlignEnum;
import net.sf.jasperreports.engine.type.RunDirectionEnum;
import net.sf.jasperreports.engine.type.VerticalAlignEnum;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
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
@Name("orderReceiptReportAction")
@Scope(ScopeType.CONVERSATION)
public class OrderReceiptReportAction extends GenericReportAction {

    @In
    User currentUser;
    private List<ProductionPlanningAction.Consolidated> consolidatedsIN;
    private List<ProductionOrder> productionOrders;
    private List<ProductionIngredient> ingredientList;
    private List<OrderMaterial> orderMaterials;
    private ProductionPlanning productionPlanning;
    private ProductionOrder productionOrder;
    private List<OrderInput> orderInputs;

    private String date;
    private String state;
    private Double unitPrice;

    public void generateReport() {
        log.debug("Generate OrderReceiptReport........");
        TypedReportData typedReportData;
        String templatePath = "/customers/reports/orderReportReceipt.jrxml";
        String fileName = "Orden_Report_Receipt";
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        date = sdf.format(new Date());
        state = "Prueba";
        Map params = new HashMap();

        params.putAll(getCommonDocumentParamsInfo());

        String query = "select IA.COD_ART, MP.NOMBRE , IA.COD_MED \n" +
                "from INGREDIENTEPRODUCCION IP\n" +
                "INNER JOIN METAPRODUCTOPRODUCCION MP\n" +
                "ON IP.IDMETAPRODUCTOPRODUCCION=MP.IDMETAPRODUCTOPRODUCCION \n" +
                "INNER JOIN WISE.INV_ARTICULOS IA \n" +
                "ON MP.COD_ART=IA.COD_ART\n" +
                "WHERE IP.IDINGREDIENTEPRODUCCION IN (15,16,17,18,19)\n";

        setReportFormat(ReportFormat.PDF);

        typedReportData = getReport(
                fileName
                , templatePath
                , query
                , params
                , "RECEPCION_DE_PEDIDOS"
        );

        JasperPrint jasperPrint = typedReportData.getJasperPrint();
        JRTemplatePrintText temp = ((JRTemplatePrintText) (((JRPrintPage) (typedReportData.getJasperPrint().getPages().get(0))).getElements().get(10)));
        JRTemplateText templateText = new JRTemplateText(temp.getOrigin(),temp.getDefaultStyleProvider());
        templateText.setHorizontalAlignment(HorizontalAlignEnum.RIGHT);
        templateText.setVerticalAlignment(VerticalAlignEnum.MIDDLE);
        templateText.copyLineBox(temp.getLineBox());
        templateText.setStyle(temp.getTemplate().getStyle());
        templateText.setFontName(temp.getFontName());
        templateText.setFontSize(temp.getFontSize());
        templateText.setMode(temp.getModeValue());
        JRTemplatePrintText printText = new JRTemplatePrintText(templateText);
        //printText = temp;
        printText.setText("prueba");
        printText.setRunDirection(RunDirectionEnum.LTR);
        printText.setLineSpacingFactor(1.2578125f);
        printText.setLeadingOffset(-1.7578125f);
        printText.setTextHeight(10.0625f);
        printText.setX(temp.getX()+temp.getX());
        printText.setY(temp.getY());
        printText.setHeight(temp.getHeight());
        printText.setWidth(temp.getWidth());
        ((JRPrintPage) (typedReportData.getJasperPrint().getPages().get(0))).getElements().add(printText);

        try {
            typedReportData.setJasperPrint(jasperPrint);
            GenerationReportData generationReportData = new GenerationReportData(typedReportData);
            generationReportData.exportReport();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Map<String, Object> getCommonDocumentParamsInfo() {

        Map<String, Object> paramMap = new HashMap<String, Object>();
        paramMap.put("position", "prueba");
        paramMap.put("userLoginParam", "prueba");
        paramMap.put("reportTitle", MessageUtils.getMessage("ProductionPlanning.orderInputOrMaterial"));
        paramMap.put("dateParam", "prueba");
        paramMap.put("estate", "prueba");
        paramMap.put("nameProduct", "prueba");
        paramMap.put("codeProduct", "prueba");
        paramMap.put("numOrder", "prueba");
        return paramMap;
    }

}