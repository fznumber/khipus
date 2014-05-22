--Khipus MRP PROD-97
--Fecha de creacion: 22/05/2014
--descripcion : Implemantar la aprobacion automatica del asiento del ingreso al almacen de productos terminados
ALTER TABLE WISE.CONFIGURACION ADD (NO_USR_PRODUCCION VARCHAR2(4 BYTE) DEFAULT 'ADM') ;

ALTER TABLE WISE.CONFIGURACION 
ADD CONSTRAINT fk_CONFIGURACION_USER FOREIGN KEY (NO_USR_PRODUCCION) REFERENCES WISE.usuarios(NO_USR);  
--select * from WISE.CONFIGURACION  
--select * from modulo;
--COMMIT
--Khipus MRP PROD-95
--Fecha de creacion: 22/05/2014
--descripcion : Modificar la entrega del pedido - solo debe generarse los vales
ALTER TABLE WISE.INV_VENTART ADD (NO_VALE	VARCHAR2(20 BYTE) NULL);
