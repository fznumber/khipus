--Khipus MRP PROD-97
--Fecha de creacion: 22/05/2014
--descripcion : Implemantar la aprobacion automatica del asiento del ingreso al almacen de productos terminados
ALTER TABLE CONFIGURACION ADD (NO_USR_PRODUCCION VARCHAR2(4 BYTE) NOT NULL);
  
--select * from WISE.CONFIGURACION  
--select * from modulo;
--COMMIT
