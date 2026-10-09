ALTER TABLE pedidos DROP CONSTRAINT ck_pedidos_estado;

UPDATE pedidos
SET estado = 'ANULADO'
WHERE estado = 'CANCELADO';

ALTER TABLE pedidos ADD CONSTRAINT ck_pedidos_estado
CHECK (estado IN ('PENDIENTE', 'PAGADO', 'ENTREGADO', 'ANULADO'));

COMMIT;
