--Khipus MRP PROD-90
--Fecha de creacion: 13/05/2014
--descripcion : agregar insumos a la formulacion
INSERT  INTO FUNCIONALIDAD (IDFUNCIONALIDAD,CODIGO,DESCRIPCION,PERMISO,NOMBRERECURSO,IDMODULO)
  VALUES (386, 'ESTIMATIONSTOCKREPORT', NULL, 15,'Reports.warehouse.EstimationStockReport', 1); 
INSERT  INTO DERECHOACCESO (IDFUNCIONALIDAD,IDROL,PERMISO,IDCOMPANIA,IDMODULO)
  VALUES (386, 50, 15, 1, 1);   
--select * from funcionalidad  
--select * from modulo;
--COMMIT
select sum(art.cantidad) from USER01_DAF.pedidos ped
INNER  JOIN USER01_DAF.articulos_pedido art
on art.pedido = ped.pedido
where ped.estado_pedido = 'PEN'
and art.cod_art = '100';
SELECT * FROM USER01_DAF.articulos_pedido;

select * from ;