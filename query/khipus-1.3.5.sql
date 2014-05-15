--Khipus MRP PROD-91
--Fecha de creacion: 15/05/2014
--descripcion : Implementar el reporte que muestre el total de insumos y materiales
INSERT  INTO FUNCIONALIDAD (IDFUNCIONALIDAD,CODIGO,DESCRIPCION,PERMISO,NOMBRERECURSO,IDMODULO)
  VALUES (387, 'ESTIMATIONSTOCKREPORT', NULL, 15,'Reports.warehouse.EstimationStockReport', 1); 
INSERT  INTO DERECHOACCESO (IDFUNCIONALIDAD,IDROL,PERMISO,IDCOMPANIA,IDMODULO)
  VALUES (387, 50, 15, 1, 1);   
--select * from funcionalidad  
--select * from modulo;
--COMMIT
