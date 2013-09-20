--
alter table REGISTROPAGOMATERIAPRIMA add TOTALPAGOACOPIO NUMBER(16,2) default 0.0;
alter table REGISTROPAGOMATERIAPRIMA add LIQUIDOPAGABLE NUMBER(16,2) default 0.0;
--
alter table PLANILLAPAGOMATERIAPRIMA add TOTALACOPIADOXGAB NUMBER(16,2) default 0.0;
alter table PLANILLAPAGOMATERIAPRIMA add TOTALMONTOACOPIOADOXGAB NUMBER(16,2) default 0.0;
alter table PLANILLAPAGOMATERIAPRIMA add TOTALRETENCIONESXGAB NUMBER(16,2) default 0.0;
alter table PLANILLAPAGOMATERIAPRIMA add TOTALCREDITOXGAB NUMBER(16,2) default 0.0;
alter table PLANILLAPAGOMATERIAPRIMA add TOTALVETERINARIOXGAB NUMBER(16,2) default 0.0;
alter table PLANILLAPAGOMATERIAPRIMA add TOTALYOGURDXGAB NUMBER(16,2) default 0.0;
alter table PLANILLAPAGOMATERIAPRIMA add TOTALTACHOSXGAB NUMBER(16,2) default 0.0;
alter table PLANILLAPAGOMATERIAPRIMA add TOTALOTROSDECUENTOSXGAB NUMBER(16,2) default 0.0;
alter table PLANILLAPAGOMATERIAPRIMA add TOTALAJUSTEXGAB NUMBER(16,2) default 0.0;
alter table PLANILLAPAGOMATERIAPRIMA add TOTALOTROSINGRESOSXGAB NUMBER(16,2) default 0.0;
alter table PLANILLAPAGOMATERIAPRIMA add TOTALIQUIDOXGAB NUMBER(16,2) default 0.0;
alter table PLANILLAPAGOMATERIAPRIMA add TOTADESCUENTOSXGAB NUMBER(16,2) default 0.0;
alter table PLANILLAPAGOMATERIAPRIMA add TOTALALCOHOLXGAB NUMBER(16,2) default 0.0;
alter table PLANILLAPAGOMATERIAPRIMA add TOTALCONCENTRADOSXGAB NUMBER(16,2) default 0.0;
--
alter table DESCUENTPRODUCTMATERIAPRIMA add ALCOHOL NUMBER(16,2) default 0.0;
alter table DESCUENTPRODUCTMATERIAPRIMA add CONCENTRADOS NUMBER(16,2) default 0.0;
ALTER TABLE DESCUENTPRODUCTMATERIAPRIMA MODIFY (
                                                YOGURT NUMBER(16,2) default 0.0,
                                                VETERINARIO NUMBER(16,2) default 0.0,
                                                CREDITO NUMBER(16,2) default 0.0,
                                                TACHOS NUMBER(16,2) default 0.0,
                                                RETENCION NUMBER(16,2) default 0.0,
                                                OTROSINGRESOS NUMBER(16,2) default 0.0,
                                                OTROSDESCUENTOS NUMBER(16,2) default 0.0,
                                                ALCOHOL NUMBER(16,2) default 0.0,
                                                CONCENTRADOS NUMBER(16,2) default 0.0
                                                );
ALTER TABLE NOTARECHAZOMATERIAPRIMA MODIFY (CANTIDADRECHAZADA NUMBER(16,2) default 0.0);
ALTER TABLE ACOPIOMATERIAPRIMA MODIFY (CANTIDAD NUMBER(16,2) default 0.0);
ALTER TABLE REGISTROPAGOMATERIAPRIMA MODIFY (
                                             TOTALGANADO NUMBER(16,2) default 0.0,
                                             CANTIDADTOTAL NUMBER(16,2) default 0.0,
                                             AJUSTEZONAPRODUCTIVA NUMBER(16,2) default 0.0,
                                             TOTALPAGOACOPIO NUMBER(16,2) default 0.0
                                             );
/*Pago productores -> reportes -> Resumen de Pago*/
INSERT  INTO FUNCIONALIDAD (IDFUNCIONALIDAD,CODIGO,DESCRIPCION,PERMISO,NOMBRERECURSO,IDMODULO)
  VALUES (350, 'REPORTPAYROLL', NULL, 15, 'menu.production.supplierPayments.reports.payRoll', 6);
INSERT  INTO DERECHOACCESO (IDFUNCIONALIDAD,IDROL,PERMISO,IDCOMPANIA,IDMODULO)
  VALUES (350, 50, 15, 1, 6);
--COMMIT
alter table RHMARCADO add DESCRIPCION CLOB NULL;
--alter table RHMARCADO drop column DESCRIPCION;
--COMMIT 