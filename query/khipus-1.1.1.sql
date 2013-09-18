--bug
alter table SESIONACOPIO drop constraint uq_sesionacopiofecha;
DROP INDEX eos.UQ_SESIONACOPIOFECHA;
commit;