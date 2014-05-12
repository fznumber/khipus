--
--Fecha de creacion: 29/04/2014
--descripcion : agregar insumos a la formulacion
INSERT  INTO FUNCIONALIDAD (IDFUNCIONALIDAD,CODIGO,DESCRIPCION,PERMISO,NOMBRERECURSO,IDMODULO)
  VALUES (386, 'ORDERDELIVERYWAREHOUSE', NULL, 15,'menu.customers.order.orderDelivery', 1); 
INSERT  INTO DERECHOACCESO (IDFUNCIONALIDAD,IDROL,PERMISO,IDCOMPANIA,IDMODULO)
  VALUES (386, 50, 15, 1, 1);   
--select * from funcionalidad  
--select * from modulo;
--COMMIT