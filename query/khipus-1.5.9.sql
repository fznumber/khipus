--Fecha de creacion: 30/10/2014
--descripcion : CRUD RESERVAS PRODUCTORES LECHEROS
--permiso para crear un producto en base a otro
create table gestionimpuesto
(
  idgestionimpuesto number(24) not null
 ,fechainicio date not null
 ,fechafin date not null
 ,constraint pk_gestionimpuesto primary key (idgestionimpuesto)
);
create table impuestoproductor(
  idimpuestoproductor number(24) not null
  ,numeroformulario varchar(10)
  ,IDPRODUCTORMATERIAPRIMA number(24,0) not null
  ,idgestionimpuesto number(24) not null
  ,constraint pk_impuestoproductor primary key (idimpuestoproductor)
);
alter table impuestoproductor add constraint fk_prodmatprim FOREIGN KEY (IDPRODUCTORMATERIAPRIMA) REFERENCES PRODUCTORMATERIAPRIMA(IDPRODUCTORMATERIAPRIMA);
alter table impuestoproductor add constraint fk_GESTIONIMPUESTO FOREIGN KEY (idgestionimpuesto) REFERENCES gestionimpuesto(idgestionimpuesto);
--drop table gestionimpuesto
--drop table impuestoproductor
--permiso para crear un las gestiones de impuesto
INSERT  INTO FUNCIONALIDAD (IDFUNCIONALIDAD,CODIGO,DESCRIPCION,PERMISO,NOMBRERECURSO,IDMODULO)
  VALUES (406, 'GESTIONTAX', NULL, 15,'GestionTax.CreateGestionTax', 6); 
INSERT  INTO DERECHOACCESO (IDFUNCIONALIDAD,IDROL,PERMISO,IDCOMPANIA,IDMODULO)
  VALUES (406, 50, 15, 1, 6);   
-- select * from funcionalidad  
--COMMIT


  
