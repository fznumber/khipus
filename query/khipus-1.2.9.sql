--Khipus MRP PROD-84: Imlementar el registro de porcentaje graso
--Diego H. Loza Fernandez
--Fecha de creacion: 27/01/2014
ALTER TABLE PLANILLAACOPIO ADD PORCENTAJEGRASA NUMBER(16,2) default 0.0 NULL;
ALTER TABLE ORDENPRODUCCION ADD PORCENTAJEGRASA NUMBER(16,2) default 0.0 NULL;
--commit


