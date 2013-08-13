package com.encens.khipus.dashboard.module.cashbox;

import com.encens.khipus.dashboard.component.factory.SqlQuery;
import com.encens.khipus.util.Constants;

/**
 * @author
 * @version 2.7
 */
public class IncomeByInvoiceByCategorySqlQuery implements SqlQuery {
    private Integer executorUnitCode;
    private Integer startDate;
    private Integer endDate;

    public String getSql() {
        String sql = "SELECT TO_NUMBER(ANIO||MES) CODIGO,\n" +
                "       TO_NUMBER(MES) MES, \n" +
                "       TO_NUMBER(ANIO) ANIO, \n" +
                "       MES_LITERAL, \n" +
                "       MONTO_B_SERV_ACAD_OBLIGATORIOS, \n" +
                "       MONTO_D_SERV_ACAD_OBLIGATORIOS,\n" +
                "       MONTO_B_SERV_ACAD_OPTATIVOS, \n" +
                "       MONTO_D_SERV_ACAD_OPTATIVOS,\n" +
                "       MONTO_B_VENTA_ARTICULOS, MONTO_D_VENTA_ARTICULOS,\n" +
                "       MONTO_B_ALQUILER_AMBIENTES, MONTO_D_ALQUILER_AMBIENTES,\n" +
                "       (MONTO_B_SERV_ACAD_OBLIGATORIOS + MONTO_B_SERV_ACAD_OPTATIVOS + MONTO_B_VENTA_ARTICULOS + MONTO_B_ALQUILER_AMBIENTES) TOTAL_CONCEPTOS_BS,\n" +
                "       (MONTO_D_SERV_ACAD_OBLIGATORIOS + MONTO_D_SERV_ACAD_OPTATIVOS + MONTO_D_VENTA_ARTICULOS + MONTO_D_ALQUILER_AMBIENTES) TOTAL_CONCEPTOS_DOL,\n" +
                "       (ROUND((MONTO_B_SERV_ACAD_OBLIGATORIOS + MONTO_B_SERV_ACAD_OPTATIVOS + MONTO_B_VENTA_ARTICULOS + MONTO_B_ALQUILER_AMBIENTES)/" + Constants.CASHBOX_SCHEMA + ".F_TIPO_CAMBIO_MES(MES, ANIO), 2)+ (MONTO_D_SERV_ACAD_OBLIGATORIOS + MONTO_D_SERV_ACAD_OPTATIVOS + MONTO_D_VENTA_ARTICULOS + MONTO_D_ALQUILER_AMBIENTES)) TOTAL_DOL,\n" +
                "       " + Constants.CASHBOX_SCHEMA + ".F_TIPO_CAMBIO_MES(MES, ANIO) TIPO_CAMBIO\n" +
                "FROM (SELECT TO_CHAR(M.FECHA,'MM') MES, TO_CHAR(M.FECHA,'YYYY') ANIO,\n" +
                "             DECODE(TO_CHAR(M.FECHA,'MM'),'01','ENERO','02','FEBRERO','03','MARZO','04','ABRIL','05','MAYO','06','JUNIO','07','JULIO','08','AGOSTO','09','SEPTIEMBRE','10','OCTUBRE','11','NOVIEMBRE','12','DICIEMBRE') MES_LITERAL,\n" +
                "             SUM(M.MONTO_B_SERV_ACAD_OBLIGATORIOS) MONTO_B_SERV_ACAD_OBLIGATORIOS,\n" +
                "             SUM(M.MONTO_D_SERV_ACAD_OBLIGATORIOS) MONTO_D_SERV_ACAD_OBLIGATORIOS,\n" +
                "             SUM(M.MONTO_B_SERV_ACAD_OPTATIVOS) MONTO_B_SERV_ACAD_OPTATIVOS,\n" +
                "             SUM(M.MONTO_D_SERV_ACAD_OPTATIVOS) MONTO_D_SERV_ACAD_OPTATIVOS,\n" +
                "             SUM(M.MONTO_B_VENTA_ARTICULOS) MONTO_B_VENTA_ARTICULOS,\n" +
                "             SUM(M.MONTO_D_VENTA_ARTICULOS) MONTO_D_VENTA_ARTICULOS,\n" +
                "             SUM(M.MONTO_B_ALQUILER_AMBIENTES) MONTO_B_ALQUILER_AMBIENTES,\n" +
                "             SUM(M.MONTO_D_ALQUILER_AMBIENTES) MONTO_D_ALQUILER_AMBIENTES\n" +
                "             FROM (SELECT TRUNC(M.FECHA) FECHA,\n" +
                "                          SUM(DECODE(CAT.COD,'01',DECODE(M.MONEDA,'B',M.MONTOTESO*M.CANT,0), 0) ) MONTO_B_SERV_ACAD_OBLIGATORIOS,\n" +
                "                          SUM(DECODE(CAT.COD,'01',DECODE(M.MONEDA,'D',M.MONTOTESO*M.CANT,0), 0) ) MONTO_D_SERV_ACAD_OBLIGATORIOS,\n" +
                "                          SUM(DECODE(CAT.COD,'02',DECODE(M.MONEDA,'B',M.MONTOTESO*M.CANT,0), 0) ) MONTO_B_SERV_ACAD_OPTATIVOS,\n" +
                "                          SUM(DECODE(CAT.COD,'02',DECODE(M.MONEDA,'D',M.MONTOTESO*M.CANT,0), 0) ) MONTO_D_SERV_ACAD_OPTATIVOS,\n" +
                "                          SUM(DECODE(CAT.COD,'03',DECODE(M.MONEDA,'B',M.MONTOTESO*M.CANT,0), 0) ) MONTO_B_VENTA_ARTICULOS,\n" +
                "                          SUM(DECODE(CAT.COD,'03',DECODE(M.MONEDA,'D',M.MONTOTESO*M.CANT,0), 0) ) MONTO_D_VENTA_ARTICULOS,\n" +
                "                          SUM(DECODE(CAT.COD,'04',DECODE(M.MONEDA,'B',M.MONTOTESO*M.CANT,0), 0) ) MONTO_B_ALQUILER_AMBIENTES,\n" +
                "                          SUM(DECODE(CAT.COD,'04',DECODE(M.MONEDA,'D',M.MONTOTESO*M.CANT,0), 0) ) MONTO_D_ALQUILER_AMBIENTES\n" +
                "                   FROM " + Constants.CASHBOX_SCHEMA + ".B_MOVIMIENTOS M, \n" +
                "                        " + Constants.CASHBOX_SCHEMA + ".ESTRUCTURAS EST,\n" +
                "                        " + Constants.CASHBOX_SCHEMA + ".CUENTAS CUEN,\n" +
                "                        " + Constants.CASHBOX_SCHEMA + ".CATEGORIAS CAT\n" +
                "                   WHERE M.ESTADO = 'V'\n" +
                "                         AND M.EST_COD = EST.CODIGO\n" +
                "                         AND M.CUEN_ID = CUEN.ID\n" +
                "                         AND CUEN.CAT_COD = CAT.COD\n";
        sql += addFilters();

        sql += "                   GROUP BY TRUNC(M.FECHA)\n" +
                "                   UNION\n" +
                "                   SELECT TRUNC(M.FECHA) FECHA,\n" +
                "                          SUM(DECODE(CAT.COD,'01',DECODE(M.MONEDA,'B',M.MONTOTESO*M.CANT,0), 0) ) MONTO_B_SERV_ACAD_OBLIGATORIOS,\n" +
                "                          SUM(DECODE(CAT.COD,'01',DECODE(M.MONEDA,'D',M.MONTOTESO*M.CANT,0), 0) ) MONTO_D_SERV_ACAD_OBLIGATORIOS,\n" +
                "                          SUM(DECODE(CAT.COD,'02',DECODE(M.MONEDA,'B',M.MONTOTESO*M.CANT,0), 0) ) MONTO_B_SERV_ACAD_OPTATIVOS,\n" +
                "                          SUM(DECODE(CAT.COD,'02',DECODE(M.MONEDA,'D',M.MONTOTESO*M.CANT,0), 0) ) MONTO_D_SERV_ACAD_OPTATIVOS,\n" +
                "                          SUM(DECODE(CAT.COD,'03',DECODE(M.MONEDA,'B',M.MONTOTESO*M.CANT,0), 0) ) MONTO_B_VENTA_ARTICULOS,\n" +
                "                          SUM(DECODE(CAT.COD,'03',DECODE(M.MONEDA,'D',M.MONTOTESO*M.CANT,0), 0) ) MONTO_D_VENTA_ARTICULOS,\n" +
                "                          SUM(DECODE(CAT.COD,'04',DECODE(M.MONEDA,'B',M.MONTOTESO*M.CANT,0), 0) ) MONTO_B_ALQUILER_AMBIENTES,\n" +
                "                          SUM(DECODE(CAT.COD,'04',DECODE(M.MONEDA,'D',M.MONTOTESO*M.CANT,0), 0) ) MONTO_D_ALQUILER_AMBIENTES\n" +
                "                   FROM " + Constants.CASHBOX_SCHEMA + ".MOVIMIENTOS M, \n" +
                "                        " + Constants.CASHBOX_SCHEMA + ".ESTRUCTURAS EST,\n" +
                "                        " + Constants.CASHBOX_SCHEMA + ".CUENTAS CUEN, \n" +
                "                        " + Constants.CASHBOX_SCHEMA + ".CATEGORIAS CAT\n" +
                "                   WHERE M.ESTADO = 'V'\n" +
                "                         AND M.EST_COD = EST.CODIGO\n" +
                "                         AND M.CUEN_ID = CUEN.ID\n" +
                "                         AND CUEN.CAT_COD = CAT.COD \n";
        sql += addFilters();

        sql += "                   GROUP BY TRUNC(M.FECHA)\n" +
                "                  ) M\n" +
                "             GROUP BY TO_CHAR(M.FECHA,'MM'), \n" +
                "                      TO_CHAR(M.FECHA,'YYYY')\n" +
                ") ORDER BY CODIGO";
        return sql;
    }

    public void setExecutorUnitCode(Integer executorUnitCode) {
        this.executorUnitCode = executorUnitCode;
    }

    public void setStartDate(Integer startDate) {
        this.startDate = startDate;
    }

    public void setEndDate(Integer endDate) {
        this.endDate = endDate;
    }

    private String addFilters() {
        String sql = "";
        if (null != startDate) {
            sql += " AND TO_NUMBER(TO_CHAR(M.FECHA,'YYYY')||TO_CHAR(M.FECHA,'MM')||TO_CHAR(M.FECHA,'DD')) >=" + startDate + "\n";
        }

        if (null != endDate) {
            sql += " AND TO_NUMBER(TO_CHAR(M.FECHA,'YYYY')||TO_CHAR(M.FECHA,'MM')||TO_CHAR(M.FECHA,'DD')) <=" + endDate + "\n";
        }

        if (null != executorUnitCode) {
            sql += " AND EST.UNIDAD_ACAD_ADM = " + executorUnitCode + "\n";
        }

        return sql;
    }
}
