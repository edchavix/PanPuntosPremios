# Diagrama entidad-relación

El modelo usa nueve tablas. Un detalle de pedido referencia exactamente un producto o un menú; un canje referencia exactamente un producto o un menú. Los movimientos de inventario se asocian a una reposición, una venta o un canje.

```mermaid
erDiagram
    CLIENTES {
        NUMBER id_cliente PK
        VARCHAR identificacion UK
        VARCHAR nombre
        VARCHAR telefono
        VARCHAR correo
        NUMBER puntos
        CHAR activo
        TIMESTAMP fecha_registro
    }

    PRODUCTOS {
        NUMBER id_producto PK
        VARCHAR codigo UK
        VARCHAR categoria
        VARCHAR nombre
        NUMBER precio
        NUMBER stock
        CHAR activo
    }

    MENUS {
        NUMBER id_menu PK
        VARCHAR codigo UK
        VARCHAR nombre
        NUMBER id_sandwich FK
        NUMBER id_bebida FK
        NUMBER id_acompanamiento FK
        NUMBER precio
        CHAR activo
    }

    PEDIDOS {
        NUMBER id_pedido PK
        NUMBER id_cliente FK
        TIMESTAMP fecha_creacion
        VARCHAR estado
        NUMBER total
        NUMBER puntos_ganados
        TIMESTAMP fecha_pago
        TIMESTAMP fecha_entrega
    }

    DETALLE_PEDIDO {
        NUMBER id_detalle PK
        NUMBER id_pedido FK
        NUMBER id_producto FK
        NUMBER id_menu FK
        NUMBER cantidad
        NUMBER precio_unitario
        NUMBER puntos_unitarios
    }

    PAGOS {
        NUMBER id_pago PK
        NUMBER id_pedido FK
        TIMESTAMP fecha_pago
        VARCHAR metodo
        NUMBER monto
        NUMBER efectivo_recibido
        NUMBER cambio
        VARCHAR referencia
    }

    RECOMPENSAS {
        NUMBER id_recompensa PK
        VARCHAR nombre
        VARCHAR categoria UK
        NUMBER puntos_requeridos
        CHAR activo
    }

    CANJES {
        NUMBER id_canje PK
        NUMBER id_cliente FK
        NUMBER id_recompensa FK
        NUMBER id_producto FK
        NUMBER id_menu FK
        TIMESTAMP fecha_canje
        NUMBER puntos_utilizados
    }

    MOVIMIENTOS_INVENTARIO {
        NUMBER id_movimiento PK
        NUMBER id_producto FK
        TIMESTAMP fecha_movimiento
        VARCHAR tipo
        NUMBER cantidad
        NUMBER id_pedido FK
        NUMBER id_canje FK
    }

    CLIENTES ||--o{ PEDIDOS : realiza
    PEDIDOS ||--|{ DETALLE_PEDIDO : contiene
    PRODUCTOS o|--o{ DETALLE_PEDIDO : producto_vendido
    MENUS o|--o{ DETALLE_PEDIDO : menu_vendido
    PEDIDOS ||--o| PAGOS : recibe
    PRODUCTOS ||--o{ MENUS : componente_sandwich
    PRODUCTOS ||--o{ MENUS : componente_bebida
    PRODUCTOS ||--o{ MENUS : componente_acompanamiento
    CLIENTES ||--o{ CANJES : realiza
    RECOMPENSAS ||--o{ CANJES : recompensa
    PRODUCTOS o|--o{ CANJES : producto_canjeado
    MENUS o|--o{ CANJES : menu_canjeado
    PRODUCTOS ||--o{ MOVIMIENTOS_INVENTARIO : registra
    PEDIDOS o|--o{ MOVIMIENTOS_INVENTARIO : venta
    CANJES o|--o{ MOVIMIENTOS_INVENTARIO : canje
```
