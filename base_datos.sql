CREATE TABLE clientes
(
    id_cliente NUMBER GENERATED ALWAYS AS IDENTITY,
    identificacion VARCHAR2(30) NOT NULL,
    nombre VARCHAR2(100) NOT NULL,
    telefono VARCHAR2(20),
    correo VARCHAR2(100),
    puntos NUMBER(10) DEFAULT 0 NOT NULL,
    activo CHAR(1) DEFAULT 'S' NOT NULL,
    fecha_registro TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,

    CONSTRAINT pk_clientes PRIMARY KEY (id_cliente),
    CONSTRAINT uq_clientes_identificacion UNIQUE (identificacion),
    CONSTRAINT ck_clientes_puntos CHECK (puntos >= 0),
    CONSTRAINT ck_clientes_activo CHECK (activo IN ('S', 'N'))
); 
SELECT * FROM clientes;

CREATE TABLE productos (
    id_producto NUMBER GENERATED ALWAYS AS IDENTITY,
    codigo VARCHAR2(30) NOT NULL,
    categoria VARCHAR2(20) NOT NULL,
    nombre VARCHAR2(100) NOT NULL,
    precio NUMBER(10,2) NOT NULL,
    stock NUMBER(10) DEFAULT 0 NOT NULL,
    activo CHAR(1) DEFAULT 'S' NOT NULL,

    CONSTRAINT pk_productos PRIMARY KEY (id_producto),
    CONSTRAINT uq_productos_codigo UNIQUE (codigo),
    CONSTRAINT ck_productos_categoria
        CHECK (categoria IN ('SANDWICH', 'BEBIDA', 'ACOMPANAMIENTO')),
    CONSTRAINT ck_productos_precio CHECK (precio >= 0),
    CONSTRAINT ck_productos_stock CHECK (stock >= 0),
    CONSTRAINT ck_productos_activo CHECK (activo IN ('S', 'N'))
);

CREATE TABLE menus (
    id_menu NUMBER GENERATED ALWAYS AS IDENTITY,
    codigo VARCHAR2(30) NOT NULL,
    nombre VARCHAR2(100) NOT NULL,
    id_sandwich NUMBER NOT NULL,
    id_bebida NUMBER NOT NULL,
    id_acompanamiento NUMBER NOT NULL,
    precio NUMBER(10,2) NOT NULL,
    activo CHAR(1) DEFAULT 'S' NOT NULL,

    CONSTRAINT pk_menus PRIMARY KEY (id_menu),
    CONSTRAINT uq_menus_codigo UNIQUE (codigo),
    CONSTRAINT fk_menus_sandwich
        FOREIGN KEY (id_sandwich)
        REFERENCES productos (id_producto),
    CONSTRAINT fk_menus_bebida
        FOREIGN KEY (id_bebida)
        REFERENCES productos (id_producto),
    CONSTRAINT fk_menus_acompanamiento
        FOREIGN KEY (id_acompanamiento)
        REFERENCES productos (id_producto),
    CONSTRAINT ck_menus_precio CHECK (precio >= 0),
    CONSTRAINT ck_menus_activo CHECK (activo IN ('S', 'N'))
);

CREATE TABLE pedidos (
    id_pedido NUMBER GENERATED ALWAYS AS IDENTITY,
    id_cliente NUMBER NOT NULL,
    fecha_creacion TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    estado VARCHAR2(10) DEFAULT 'PENDIENTE' NOT NULL,
    total NUMBER(12,2) DEFAULT 0 NOT NULL,
    puntos_ganados NUMBER(10) DEFAULT 0 NOT NULL,
    fecha_pago TIMESTAMP,
    fecha_entrega TIMESTAMP,

    CONSTRAINT pk_pedidos PRIMARY KEY (id_pedido),
    CONSTRAINT fk_pedidos_cliente
        FOREIGN KEY (id_cliente)
        REFERENCES clientes (id_cliente),
    CONSTRAINT ck_pedidos_estado
        CHECK (estado IN (
            'PENDIENTE', 'PAGADO', 'ENTREGADO', 'CANCELADO'
        )),
    CONSTRAINT ck_pedidos_total CHECK (total >= 0),
    CONSTRAINT ck_pedidos_puntos CHECK (puntos_ganados >= 0)
);

CREATE TABLE detalle_pedido (
    id_detalle NUMBER GENERATED ALWAYS AS IDENTITY,
    id_pedido NUMBER NOT NULL,
    id_producto NUMBER,
    id_menu NUMBER,
    cantidad NUMBER(10) NOT NULL,
    precio_unitario NUMBER(10,2) NOT NULL,
    puntos_unitarios NUMBER(10) DEFAULT 0 NOT NULL,

    CONSTRAINT pk_detalle_pedido PRIMARY KEY (id_detalle),
    CONSTRAINT fk_detalle_pedido
        FOREIGN KEY (id_pedido)
        REFERENCES pedidos (id_pedido),
    CONSTRAINT fk_detalle_producto
        FOREIGN KEY (id_producto)
        REFERENCES productos (id_producto),
    CONSTRAINT fk_detalle_menu
        FOREIGN KEY (id_menu)
        REFERENCES menus (id_menu),
    CONSTRAINT ck_detalle_tipo CHECK (
        (id_producto IS NOT NULL AND id_menu IS NULL)
        OR
        (id_producto IS NULL AND id_menu IS NOT NULL)
    ),
    CONSTRAINT ck_detalle_cantidad CHECK (cantidad > 0),
    CONSTRAINT ck_detalle_precio CHECK (precio_unitario >= 0),
    CONSTRAINT ck_detalle_puntos CHECK (puntos_unitarios >= 0)
);

CREATE TABLE pagos (
    id_pago NUMBER GENERATED ALWAYS AS IDENTITY,
    id_pedido NUMBER NOT NULL,
    fecha_pago TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    metodo VARCHAR2(10) NOT NULL,
    monto NUMBER(12,2) NOT NULL,
    efectivo_recibido NUMBER(12,2),
    cambio NUMBER(12,2),
    referencia VARCHAR2(100),

    CONSTRAINT pk_pagos PRIMARY KEY (id_pago),
    CONSTRAINT uq_pagos_pedido UNIQUE (id_pedido),
    CONSTRAINT fk_pagos_pedido
        FOREIGN KEY (id_pedido)
        REFERENCES pedidos (id_pedido),
    CONSTRAINT ck_pagos_metodo
        CHECK (metodo IN ('EFECTIVO', 'TARJETA')),
    CONSTRAINT ck_pagos_monto CHECK (monto >= 0),
    CONSTRAINT ck_pagos_datos CHECK (
        (
            metodo = 'EFECTIVO'
            AND efectivo_recibido IS NOT NULL
            AND efectivo_recibido >= monto
            AND cambio IS NOT NULL
            AND cambio = efectivo_recibido - monto
            AND referencia IS NULL
        )
        OR
        (
            metodo = 'TARJETA'
            AND referencia IS NOT NULL
            AND efectivo_recibido IS NULL
            AND cambio IS NULL
        )
    )
);
SELECT * FROM pagos;

CREATE TABLE recompensas (
    id_recompensa NUMBER GENERATED ALWAYS AS IDENTITY,
    nombre VARCHAR2(100) NOT NULL,
    categoria VARCHAR2(20) NOT NULL,
    puntos_requeridos NUMBER(10) NOT NULL,
    activo CHAR(1) DEFAULT 'S' NOT NULL,

    CONSTRAINT pk_recompensas PRIMARY KEY (id_recompensa),
    CONSTRAINT uq_recompensas_categoria UNIQUE (categoria),
    CONSTRAINT ck_recompensas_categoria CHECK (
        categoria IN ('BEBIDA', 'ACOMPANAMIENTO', 'SANDWICH', 'MENU')
    ),
    CONSTRAINT ck_recompensas_puntos CHECK (puntos_requeridos > 0),
    CONSTRAINT ck_recompensas_activo CHECK (activo IN ('S', 'N'))
);

INSERT INTO recompensas (nombre, categoria, puntos_requeridos)
VALUES ('Bebida gratis', 'BEBIDA', 10);

INSERT INTO recompensas (nombre, categoria, puntos_requeridos)
VALUES ('Acompanamiento gratis', 'ACOMPANAMIENTO', 15);

INSERT INTO recompensas (nombre, categoria, puntos_requeridos)
VALUES ('Sandwich gratis', 'SANDWICH', 30);

INSERT INTO recompensas (nombre, categoria, puntos_requeridos)
VALUES ('Menu gratis', 'MENU', 50);

COMMIT;
SELECT nombre, categoria, puntos_requeridos FROM recompensas;

CREATE TABLE canjes (
    id_canje NUMBER GENERATED ALWAYS AS IDENTITY,
    id_cliente NUMBER NOT NULL,
    id_recompensa NUMBER NOT NULL,
    id_producto NUMBER,
    id_menu NUMBER,
    fecha_canje TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    puntos_utilizados NUMBER(10) NOT NULL,

    CONSTRAINT pk_canjes PRIMARY KEY (id_canje),
    CONSTRAINT fk_canjes_cliente
        FOREIGN KEY (id_cliente)
        REFERENCES clientes (id_cliente),
    CONSTRAINT fk_canjes_recompensa
        FOREIGN KEY (id_recompensa)
        REFERENCES recompensas (id_recompensa),
    CONSTRAINT fk_canjes_producto
        FOREIGN KEY (id_producto)
        REFERENCES productos (id_producto),
    CONSTRAINT fk_canjes_menu
        FOREIGN KEY (id_menu)
        REFERENCES menus (id_menu),
    CONSTRAINT ck_canjes_tipo CHECK (
        (id_producto IS NOT NULL AND id_menu IS NULL)
        OR
        (id_producto IS NULL AND id_menu IS NOT NULL)
    ),
    CONSTRAINT ck_canjes_puntos CHECK (puntos_utilizados > 0)
);

CREATE TABLE movimientos_inventario (
    id_movimiento NUMBER GENERATED ALWAYS AS IDENTITY,
    id_producto NUMBER NOT NULL,
    fecha_movimiento TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    tipo VARCHAR2(15) NOT NULL,
    cantidad NUMBER(10) NOT NULL,
    id_pedido NUMBER,
    id_canje NUMBER,

    CONSTRAINT pk_movimientos_inv PRIMARY KEY (id_movimiento),
    CONSTRAINT fk_mov_inv_producto
        FOREIGN KEY (id_producto)
        REFERENCES productos (id_producto),
    CONSTRAINT fk_mov_inv_pedido
        FOREIGN KEY (id_pedido)
        REFERENCES pedidos (id_pedido),
    CONSTRAINT fk_mov_inv_canje
        FOREIGN KEY (id_canje)
        REFERENCES canjes (id_canje),
    CONSTRAINT ck_mov_inv_cantidad CHECK (cantidad > 0),
    CONSTRAINT ck_mov_inv_tipo CHECK (
        tipo IN ('REPOSICION', 'VENTA', 'CANJE')
    ),
    CONSTRAINT ck_mov_inv_origen CHECK (
        (tipo = 'REPOSICION'
            AND id_pedido IS NULL AND id_canje IS NULL)
        OR
        (tipo = 'VENTA'
            AND id_pedido IS NOT NULL AND id_canje IS NULL)
        OR
        (tipo = 'CANJE'
            AND id_canje IS NOT NULL AND id_pedido IS NULL)
    )
);

CREATE TABLE movimientos_inventario (
    id_movimiento NUMBER GENERATED ALWAYS AS IDENTITY,
    id_producto NUMBER NOT NULL,
    fecha_movimiento TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    tipo VARCHAR2(15) NOT NULL,
    cantidad NUMBER(10) NOT NULL,
    id_pedido NUMBER,
    id_canje NUMBER,

    CONSTRAINT pk_movimientos_inv PRIMARY KEY (id_movimiento),
    CONSTRAINT fk_mov_inv_producto
        FOREIGN KEY (id_producto)
        REFERENCES productos (id_producto),
    CONSTRAINT fk_mov_inv_pedido
        FOREIGN KEY (id_pedido)
        REFERENCES pedidos (id_pedido),
    CONSTRAINT fk_mov_inv_canje
        FOREIGN KEY (id_canje)
        REFERENCES canjes (id_canje),
    CONSTRAINT ck_mov_inv_cantidad CHECK (cantidad > 0),
    CONSTRAINT ck_mov_inv_tipo CHECK (
        tipo IN ('REPOSICION', 'VENTA', 'CANJE')
    ),
    CONSTRAINT ck_mov_inv_origen CHECK (
        (tipo = 'REPOSICION'
            AND id_pedido IS NULL AND id_canje IS NULL)
        OR
        (tipo = 'VENTA'
            AND id_pedido IS NOT NULL AND id_canje IS NULL)
        OR
        (tipo = 'CANJE'
            AND id_canje IS NOT NULL AND id_pedido IS NULL)
    )
);

SELECT table_name
FROM user_tables
WHERE table_name IN (
    'CLIENTES',
    'PRODUCTOS',
    'MENUS',
    'PEDIDOS',
    'DETALLE_PEDIDO',
    'PAGOS',
    'RECOMPENSAS',
    'CANJES',
    'MOVIMIENTOS_INVENTARIO'
)
ORDER BY table_name;

INSERT INTO productos (codigo, categoria, nombre, precio)
VALUES ('SAN001', 'SANDWICH', 'Sandwich de pollo', 25.00);

COMMIT;

INSERT INTO productos (codigo, categoria, nombre, precio)
VALUES ('BEB001', 'BEBIDA', 'Refresco', 8.00);

INSERT INTO productos (codigo, categoria, nombre, precio)
VALUES ('ACO001', 'ACOMPANAMIENTO', 'Papas fritas', 12.00);

COMMIT;

SELECT codigo, categoria, nombre, precio, stock
FROM productos
ORDER BY codigo;

BEGIN
    UPDATE productos
    SET stock = stock + 20
    WHERE codigo IN ('SAN001', 'BEB001', 'ACO001');

    INSERT INTO movimientos_inventario (
        id_producto, tipo, cantidad
    )
    SELECT id_producto, 'REPOSICION', 20
    FROM productos
    WHERE codigo IN ('SAN001', 'BEB001', 'ACO001');

    COMMIT;
EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK;
        RAISE;
END;
/

SELECT codigo, categoria, nombre, precio, stock
FROM productos
ORDER BY codigo;

INSERT INTO menus (
    codigo, nombre,
    id_sandwich, id_bebida, id_acompanamiento,
    precio
)
VALUES (
    'MEN001',
    'Menu de pollo',
    (SELECT id_producto FROM productos WHERE codigo = 'SAN001'),
    (SELECT id_producto FROM productos WHERE codigo = 'BEB001'),
    (SELECT id_producto FROM productos WHERE codigo = 'ACO001'),
    40.00
);

COMMIT;

SELECT m.codigo,
       m.nombre,
       s.nombre AS sandwich,
       b.nombre AS bebida,
       a.nombre AS acompanamiento,
       m.precio
FROM menus m
JOIN productos s ON s.id_producto = m.id_sandwich
JOIN productos b ON b.id_producto = m.id_bebida
JOIN productos a ON a.id_producto = m.id_acompanamiento;

INSERT INTO clientes (identificacion, nombre)
VALUES ('PRUEBA001', 'Cliente de prueba');

COMMIT;

SELECT identificacion, nombre, puntos, activo
FROM clientes
WHERE identificacion = 'PRUEBA001';

SELECT p.codigo,
       p.nombre,
       m.fecha_movimiento,
       m.tipo,
       m.cantidad
FROM movimientos_inventario m
JOIN productos p ON p.id_producto = m.id_producto
WHERE p.codigo = 'BEB002'
ORDER BY m.fecha_movimiento DESC, m.id_movimiento DESC;

SELECT id_pedido, estado, total, puntos_ganados, fecha_pago
FROM pedidos
WHERE id_pedido = 1;

SELECT id_producto, id_menu, cantidad, precio_unitario,
       cantidad * precio_unitario AS subtotal,
       cantidad * puntos_unitarios AS puntos_posibles
FROM detalle_pedido
WHERE id_pedido = 1
ORDER BY id_detalle;


SELECT codigo, stock
FROM productos
WHERE codigo IN ('SAN001', 'BEB001', 'ACO001')
ORDER BY codigo;


