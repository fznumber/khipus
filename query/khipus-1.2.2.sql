--Khipus MRP PROD-78: Generar vales de ingreso para insumos, materiales y ordenes
--Diego H. Loza Fernandez
--Fecha de creacion: 12/12/2013
alter table ordenproduccion add (COSTOUNITARIO	NUMBER(16,6)	null);
--commit
--Khipus MRP PROD-79: Generar el asiento contable para productos terminados
--Diego H. Loza Fernandez
--Fecha de creacion: 16/12/2013
alter table costosindirectos add ( CUENTA	VARCHAR2(20 BYTE) null);                                   
alter table costosindirectos add constraint fk_ARCGMS_COSTOSINDIRECTOS FOREIGN KEY (NO_CIA,CUENTA) REFERENCES WISE.ARCGMS(NO_CIA,CUENTA);

alter table ordenproduccion add (  costototalelfec number(16,2) null
                                  ,costototalypfb number(16,2) null
                                  ,costototaldepregrupo number(16,2) null
                                  ,costototaldepreheramientas number(16,2) null
                                  ,costototalmanoobrainidirecta number(16,2) null
                                  ,costototalmanoobraeventual number(16,2) null);
ALTER TABLE COSTOSINDIRECTOS RENAME TO CONFCOSTOSINDIRECTOS;
ALTER TABLE CONFCOSTOSINDIRECTOS RENAME COLUMN IDCOSTOSINDIRECTOS TO IDCONFCOSTOSINDIRECTOS;

CREATE TABLE COSTOSINDIRECTOS
        (
            IDCOSTOSINDIRECTOS
        );
--commit

CREATE TABLE COSTOSINDIRECTOS (
   IDCOSTOSINDIRECTOS NUMBER (24,0) NOT NULL
  ,NOMBRE VARCHAR(512) NOT NULL
  ,DESCRIPCION VARCHAR(512) NULL
  ,MONTOBS NUMBER(16,2) NOT NULL  
  ,NO_CIA	VARCHAR2(2 BYTE) NULL
  ,CUENTA	VARCHAR2(20 BYTE) null
  ,VERSION   NUMBER(24) NOT NULL
  ,IDCOMPANIA NUMBER(24) NOT  NULL
  ,CONSTRAINT pk_COSTOSINDIRECTOS PRIMARY KEY (IDCOSTOSINDIRECTOS)
);
--drop table COSTOSINDIRECTOS
ALTER TABLE COSTOSINDIRECTOS
ADD CONSTRAINT fk_INV_GRUPOS_COSTOSINDIRECTOS FOREIGN KEY (NO_CIA,COD_GRU) REFERENCES WISE.INV_GRUPOS(NO_CIA,COD_GRU);
