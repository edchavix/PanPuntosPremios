package panpuntospremios;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import javax.swing.table.DefaultTableModel;

public class ReporteDAO {

    private final Connection conexion;

    public ReporteDAO(Connection conexion) {
        this.conexion = conexion;
    }

    public DefaultTableModel consultar(String reporte) throws SQLException {
        if ("Ventas por periodo".equals(reporte)) {
            throw new IllegalArgumentException(
                    "Selecciona las fechas del reporte de ventas."
            );
        }
        return ejecutar(obtenerConsulta(reporte));
    }

    public DefaultTableModel consultarVentasPorPeriodo(
            Timestamp desde, Timestamp hastaExclusivo) throws SQLException {

        if (desde == null || hastaExclusivo == null
                || !desde.before(hastaExclusivo)) {
            throw new IllegalArgumentException(
                    "El rango de fechas no es valido."
            );
        }

        String sql = "WITH metodos AS ("
                + "SELECT 'EFECTIVO' AS metodo_pago FROM dual "
                + "UNION ALL SELECT 'TARJETA' AS metodo_pago FROM dual"
                + "), ventas AS ("
                + "SELECT pg.metodo, COUNT(*) AS cantidad_ventas, "
                + "NVL(SUM(pg.monto), 0) AS total_vendido "
                + "FROM pagos pg WHERE pg.fecha_pago >= ? AND pg.fecha_pago < ? "
                + "GROUP BY pg.metodo"
                + "), resumen AS ("
                + "SELECT m.metodo_pago, NVL(v.cantidad_ventas, 0) AS cantidad_ventas, "
                + "NVL(v.total_vendido, 0) AS total_vendido "
                + "FROM metodos m LEFT JOIN ventas v ON v.metodo = m.metodo_pago "
                + "UNION ALL "
                + "SELECT 'TOTAL' AS metodo_pago, COUNT(*) AS cantidad_ventas, "
                + "NVL(SUM(pg.monto), 0) AS total_vendido "
                + "FROM pagos pg WHERE pg.fecha_pago >= ? AND pg.fecha_pago < ?"
                + ") SELECT metodo_pago, cantidad_ventas, total_vendido "
                + "FROM resumen ORDER BY CASE metodo_pago "
                + "WHEN 'EFECTIVO' THEN 1 WHEN 'TARJETA' THEN 2 ELSE 3 END";

        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setTimestamp(1, desde);
            sentencia.setTimestamp(2, hastaExclusivo);
            sentencia.setTimestamp(3, desde);
            sentencia.setTimestamp(4, hastaExclusivo);
            try (ResultSet resultado = sentencia.executeQuery()) {
                return crearModelo(resultado);
            }
        }
    }

    public DefaultTableModel consultarPedidosCliente(long idCliente)
            throws SQLException {

        String sql = "SELECT id_pedido, fecha_creacion, estado, total, "
                + "puntos_ganados FROM pedidos "
                + "WHERE id_cliente = ? ORDER BY id_pedido DESC";

        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setLong(1, idCliente);
            try (ResultSet resultado = sentencia.executeQuery()) {
                return crearModelo(resultado);
            }
        }
    }

    public DefaultTableModel consultarHistorialPuntos(long idCliente)
            throws SQLException {

        String sql = "SELECT fecha, tipo, referencia, puntos FROM ("
                + "SELECT p.fecha_pago AS fecha, 'ACUMULACION' AS tipo, "
                + "'PEDIDO ' || TO_CHAR(p.id_pedido) AS referencia, "
                + "p.puntos_ganados AS puntos FROM pedidos p "
                + "WHERE p.id_cliente = ? "
                + "AND p.estado IN ('PAGADO', 'ENTREGADO') "
                + "AND p.puntos_ganados > 0 "
                + "UNION ALL "
                + "SELECT c.fecha_canje AS fecha, 'CANJE' AS tipo, "
                + "'CANJE ' || TO_CHAR(c.id_canje) AS referencia, "
                + "-c.puntos_utilizados AS puntos FROM canjes c "
                + "WHERE c.id_cliente = ?"
                + ") ORDER BY fecha DESC";

        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setLong(1, idCliente);
            sentencia.setLong(2, idCliente);
            try (ResultSet resultado = sentencia.executeQuery()) {
                return crearModelo(resultado);
            }
        }
    }

    private DefaultTableModel ejecutar(String sql) throws SQLException {
        try (PreparedStatement sentencia = conexion.prepareStatement(sql);
             ResultSet resultado = sentencia.executeQuery()) {
            return crearModelo(resultado);
        }
    }

    private DefaultTableModel crearModelo(ResultSet resultado)
            throws SQLException {

        ResultSetMetaData metadatos = resultado.getMetaData();
        int cantidadColumnas = metadatos.getColumnCount();
        DefaultTableModel modelo = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int fila, int columna) {
                return false;
            }
        };

        for (int columna = 1; columna <= cantidadColumnas; columna++) {
            modelo.addColumn(metadatos.getColumnLabel(columna));
        }

        while (resultado.next()) {
            Object[] fila = new Object[cantidadColumnas];
            for (int columna = 1; columna <= cantidadColumnas; columna++) {
                fila[columna - 1] = resultado.getObject(columna);
            }
            modelo.addRow(fila);
        }

        return modelo;
    }

    private String obtenerConsulta(String reporte) {
        switch (reporte) {
            case "Productos mas vendidos":
                return "WITH unidades AS ("
                        + "SELECT d.id_producto, d.cantidad "
                        + "FROM detalle_pedido d "
                        + "JOIN pedidos p ON p.id_pedido = d.id_pedido "
                        + "WHERE d.id_producto IS NOT NULL "
                        + "AND p.estado IN ('PAGADO', 'ENTREGADO') "
                        + "UNION ALL "
                        + "SELECT m.id_sandwich, d.cantidad "
                        + "FROM detalle_pedido d JOIN menus m ON m.id_menu = d.id_menu "
                        + "JOIN pedidos p ON p.id_pedido = d.id_pedido "
                        + "WHERE d.id_menu IS NOT NULL "
                        + "AND p.estado IN ('PAGADO', 'ENTREGADO') "
                        + "UNION ALL "
                        + "SELECT m.id_bebida, d.cantidad "
                        + "FROM detalle_pedido d JOIN menus m ON m.id_menu = d.id_menu "
                        + "JOIN pedidos p ON p.id_pedido = d.id_pedido "
                        + "WHERE d.id_menu IS NOT NULL "
                        + "AND p.estado IN ('PAGADO', 'ENTREGADO') "
                        + "UNION ALL "
                        + "SELECT m.id_acompanamiento, d.cantidad "
                        + "FROM detalle_pedido d JOIN menus m ON m.id_menu = d.id_menu "
                        + "JOIN pedidos p ON p.id_pedido = d.id_pedido "
                        + "WHERE d.id_menu IS NOT NULL "
                        + "AND p.estado IN ('PAGADO', 'ENTREGADO')"
                        + ") SELECT pr.codigo, pr.nombre, pr.categoria, "
                        + "SUM(u.cantidad) AS unidades_vendidas "
                        + "FROM unidades u JOIN productos pr "
                        + "ON pr.id_producto = u.id_producto "
                        + "GROUP BY pr.codigo, pr.nombre, pr.categoria "
                        + "ORDER BY unidades_vendidas DESC, pr.codigo";

            case "Productos agotados o con bajo inventario":
                return "SELECT codigo, nombre, categoria, stock "
                        + "FROM productos WHERE stock <= 5 "
                        + "ORDER BY stock, codigo";

            case "Puntos acumulados por cliente":
                return "SELECT identificacion, nombre, puntos "
                        + "FROM clientes ORDER BY puntos DESC, nombre";

            case "Historial de canjes":
                return consultaCanjes();

            case "Ventas de menus completos":
                return "SELECT m.codigo, m.nombre, "
                        + "SUM(d.cantidad) AS cantidad_vendida, "
                        + "SUM(d.cantidad * d.precio_unitario) AS importe_total "
                        + "FROM detalle_pedido d "
                        + "JOIN menus m ON m.id_menu = d.id_menu "
                        + "JOIN pedidos p ON p.id_pedido = d.id_pedido "
                        + "WHERE p.estado IN ('PAGADO', 'ENTREGADO') "
                        + "GROUP BY m.codigo, m.nombre "
                        + "ORDER BY cantidad_vendida DESC, m.codigo";

            case "Pedidos":
                return "SELECT p.id_pedido, p.fecha_creacion, "
                        + "c.identificacion, c.nombre AS cliente, "
                        + "p.estado, p.total, p.puntos_ganados "
                        + "FROM pedidos p "
                        + "JOIN clientes c ON c.id_cliente = p.id_cliente "
                        + "ORDER BY p.id_pedido DESC";

            case "Pagos":
                return "SELECT pg.id_pago, pg.id_pedido, pg.fecha_pago, "
                        + "c.identificacion, c.nombre AS cliente, "
                        + "pg.metodo, pg.monto, pg.efectivo_recibido, "
                        + "pg.cambio, pg.referencia "
                        + "FROM pagos pg "
                        + "JOIN pedidos p ON p.id_pedido = pg.id_pedido "
                        + "JOIN clientes c ON c.id_cliente = p.id_cliente "
                        + "ORDER BY pg.id_pago DESC";

            case "Existencias":
                return "SELECT codigo, categoria, nombre, precio, stock, activo "
                        + "FROM productos ORDER BY categoria, codigo";

            case "Movimientos de inventario":
                return "SELECT mi.id_movimiento, mi.fecha_movimiento, "
                        + "mi.tipo, pr.codigo, pr.nombre, mi.cantidad, "
                        + "mi.id_pedido, mi.id_canje "
                        + "FROM movimientos_inventario mi "
                        + "JOIN productos pr ON pr.id_producto = mi.id_producto "
                        + "ORDER BY mi.id_movimiento DESC";

            default:
                throw new IllegalArgumentException(
                        "Selecciona un reporte valido."
                );
        }
    }

    private String consultaCanjes() {
        return "SELECT c.id_canje, c.fecha_canje, cl.identificacion, "
                + "cl.nombre AS cliente, r.nombre AS recompensa, "
                + "c.puntos_utilizados, COALESCE(pr.codigo, m.codigo) AS articulo, "
                + "COALESCE(pr.nombre, m.nombre) AS nombre_articulo "
                + "FROM canjes c "
                + "JOIN clientes cl ON cl.id_cliente = c.id_cliente "
                + "JOIN recompensas r ON r.id_recompensa = c.id_recompensa "
                + "LEFT JOIN productos pr ON pr.id_producto = c.id_producto "
                + "LEFT JOIN menus m ON m.id_menu = c.id_menu "
                + "ORDER BY c.id_canje DESC";
    }
}
