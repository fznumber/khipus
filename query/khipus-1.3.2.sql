--Khipus KHIPUS-54: Implementar la probación de asientos contables
--Diego H. Loza Fernandez
--Fecha de creacion: 25/04/2014
INSERT  INTO FUNCIONALIDAD (IDFUNCIONALIDAD,CODIGO,DESCRIPCION,PERMISO,NOMBRERECURSO,IDMODULO)
  VALUES (385, 'APPROVEDALLACCOUNTENTRIES', NULL, 15,'menu.warehouse.accountEntries', 5); 
INSERT  INTO DERECHOACCESO (IDFUNCIONALIDAD,IDROL,PERMISO,IDCOMPANIA,IDMODULO)
  VALUES (385, 50, 15, 1, 5);     
--select * from funcionalidad;  
--select * from modulo
--COMMIT
