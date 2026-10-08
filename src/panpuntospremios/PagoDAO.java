package panpuntospremios;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.TreeMap;

public class PagoDAO {

    private final Connection conexion;

    public PagoDAO(Connection conexion) {
        this.conexion = conexion;
    }

    public PagoEfectivo cobrarEfectivo(
            long idPedido, BigDecimal recibido) throws SQLException {

        if (idPedido <= 0) {
            throw new IllegalArgumentException(
                    "Escribe un numero de pedido valido."
            );
        }

        if (!conexion.getAutoCommit()) {
            throw new SQLException(
                    "Hay una operacion pendiente en la conexion."
            );
        }

        conexion.setAutoCommit(false);
        PagoEfectivo pago;

        try {
            long idCliente;
            BigDecimal total;

            String consultaPedido = "SELECT id_cliente, estado, total "
                    + "FROM pedidos WHERE id_pedido = ? FOR UPDATE";

            try (PreparedStatement sentencia =
                    conexion.prepareStatement(consultaPedido)) {

                sentencia.setLong(1, idPedido);

                try (ResultSet resultado = sentencia.executeQuery()) {
                    if (!resultado.next()) {
                        throw new IllegalArgumentException(
                                "No existe ese pedido."
                        );
                    }

                    if (!"PENDIENTE".equals(
                            resultado.getString("estado"))) {
                        throw new IllegalArgumentException(
                                "Solo se pueden cobrar pedidos pendientes."
                        );
                    }

                    idCliente = resultado.getLong("id_cliente");
                    total = resultado.getBigDecimal("total");
                }
            }

            pago = new PagoEfectivo(total, recibido);
            long saldoActual = consultarCliente(idCliente);

            Map<Long, Long> cantidades = new TreeMap<>();
            long puntos = leerDetalles(idPedido, total, cantidades);

            if (puntos > 9999999999L
                    || saldoActual + puntos > 9999999999L) {
                throw new IllegalArgumentException(
                        "El saldo de puntos supera el limite permitido."
                );
            }

            descontarInventario(idPedido, cantidades);
            registrarPago(idPedido, pago);
            acreditarPuntos(idCliente, puntos);
            marcarPagado(idPedido, puntos);

            conexion.commit();

        } catch (SQLException | RuntimeException error) {
            try {
                conexion.rollback();
            } catch (SQLException errorReversion) {
                error.addSuppressed(errorReversion);
                throw error;
            }

            try {
                conexion.setAutoCommit(true);
            } catch (SQLException errorRestauracion) {
                error.addSuppressed(errorRestauracion);
            }

            throw error;
        }

        conexion.setAutoCommit(true);
        return pago;
    }
    
        public PagoTarjeta cobrarTarjeta(
            long idPedido, String referencia, boolean aprobado)
            throws SQLException {

        if (idPedido <= 0) {
            throw new IllegalArgumentException(
                    "Escribe un numero de pedido valido."
            );
        }

        if (!conexion.getAutoCommit()) {
            throw new SQLException(
                    "Hay una operacion pendiente en la conexion."
            );
        }

        conexion.setAutoCommit(false);

        try {
            long idCliente;
            BigDecimal total;

            String consultaPedido = "SELECT id_cliente, estado, total "
                    + "FROM pedidos WHERE id_pedido = ? FOR UPDATE";

            try (PreparedStatement sentencia =
                    conexion.prepareStatement(consultaPedido)) {
                sentencia.setLong(1, idPedido);

                try (ResultSet resultado = sentencia.executeQuery()) {
                    if (!resultado.next()) {
                        throw new IllegalArgumentException(
                                "No existe ese pedido."
                        );
                    }

                    if (!"PENDIENTE".equals(
                            resultado.getString("estado"))) {
                        throw new IllegalArgumentException(
                                "Solo se pueden cobrar pedidos pendientes."
                        );
                    }

                    idCliente = resultado.getLong("id_cliente");
                    total = resultado.getBigDecimal("total");
                }
            }

            PagoTarjeta pago = new PagoTarjeta(
                    total, referencia, aprobado
            );

            pago.exigirAprobacion();

            long saldoActual = consultarCliente(idCliente);
            Map<Long, Long> cantidades = new TreeMap<>();
            long puntos = leerDetalles(idPedido, total, cantidades);

            if (puntos > 9999999999L
                    || saldoActual + puntos > 9999999999L) {
                throw new IllegalArgumentException(
                        "El saldo de puntos supera el limite permitido."
                );
            }

            descontarInventario(idPedido, cantidades);
            registrarPagoTarjeta(idPedido, pago);
            acreditarPuntos(idCliente, puntos);
            marcarPagado(idPedido, puntos);

            conexion.commit();
            conexion.setAutoCommit(true);
            return pago;

        } catch (SQLException | RuntimeException error) {
            try {
                conexion.rollback();
            } catch (SQLException errorReversion) {
                error.addSuppressed(errorReversion);
            }

            try {
                conexion.setAutoCommit(true);
            } catch (SQLException errorRestauracion) {
                error.addSuppressed(errorRestauracion);
            }

            throw error;
        }
    }
    

    private long consultarCliente(long idCliente) throws SQLException {
        String sql = "SELECT activo, puntos FROM clientes "
                + "WHERE id_cliente = ? FOR UPDATE";

        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setLong(1, idCliente);

            try (ResultSet resultado = sentencia.executeQuery()) {
                if (!resultado.next()
                        || !"S".equals(resultado.getString("activo"))) {
                    throw new IllegalArgumentException(
                            "El cliente no existe o esta desactivado."
                    );
                }

                return resultado.getLong("puntos");
            }
        }
    }

    private long leerDetalles(
            long idPedido, BigDecimal total,
            Map<Long, Long> cantidades) throws SQLException {

        String sql = "SELECT id_producto, id_menu, cantidad, "
                + "precio_unitario, puntos_unitarios "
                + "FROM detalle_pedido WHERE id_pedido = ?";

        BigDecimal totalCalculado = BigDecimal.ZERO;
        long puntos = 0;
        int filas = 0;

        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setLong(1, idPedido);

            try (ResultSet resultado = sentencia.executeQuery()) {
                while (resultado.next()) {
                    filas++;

                    long cantidad = resultado.getLong("cantidad");

                    totalCalculado = totalCalculado.add(
                            resultado.getBigDecimal("precio_unitario")
                                    .multiply(BigDecimal.valueOf(cantidad))
                    );

                    long puntosLinea = Math.multiplyExact(
                            resultado.getLong("puntos_unitarios"),
                            cantidad
                    );
                    puntos = Math.addExact(puntos, puntosLinea);

                    long idProducto = resultado.getLong("id_producto");

                    if (!resultado.wasNull()) {
                        acumular(cantidades, idProducto, cantidad);
                    } else {
                        agregarComponentes(
                                resultado.getLong("id_menu"),
                                cantidad,
                                cantidades
                        );
                    }
                }
            }
        }

        if (filas == 0 || totalCalculado.compareTo(total) != 0) {
            throw new IllegalArgumentException(
                    "Los detalles no coinciden con el total del pedido."
            );
        }

        return puntos;
    }

    private void agregarComponentes(
            long idMenu, long cantidad,
            Map<Long, Long> cantidades) throws SQLException {

        String sql = "SELECT activo, id_sandwich, id_bebida, "
                + "id_acompanamiento FROM menus "
                + "WHERE id_menu = ? FOR UPDATE";

        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setLong(1, idMenu);

            try (ResultSet resultado = sentencia.executeQuery()) {
                if (!resultado.next()
                        || !"S".equals(resultado.getString("activo"))) {
                    throw new IllegalArgumentException(
                            "Un menu del pedido no existe o esta desactivado."
                    );
                }

                acumular(
                        cantidades,
                        resultado.getLong("id_sandwich"),
                        cantidad
                );
                acumular(
                        cantidades,
                        resultado.getLong("id_bebida"),
                        cantidad
                );
                acumular(
                        cantidades,
                        resultado.getLong("id_acompanamiento"),
                        cantidad
                );
            }
        }
    }

    private void acumular(
            Map<Long, Long> cantidades, long idProducto, long cantidad) {

        cantidades.put(
                idProducto,
                Math.addExact(
                        cantidades.getOrDefault(idProducto, 0L),
                        cantidad
                )
        );
    }

    private void descontarInventario(
            long idPedido, Map<Long, Long> cantidades) throws SQLException {

        String consultar = "SELECT nombre, stock, activo FROM productos "
                + "WHERE id_producto = ? FOR UPDATE";

        String actualizar = "UPDATE productos SET stock = stock - ? "
                + "WHERE id_producto = ?";

        String movimiento = "INSERT INTO movimientos_inventario "
                + "(id_producto, tipo, cantidad, id_pedido) "
                + "VALUES (?, 'VENTA', ?, ?)";

        try (
                PreparedStatement lectura =
                        conexion.prepareStatement(consultar);
                PreparedStatement cambio =
                        conexion.prepareStatement(actualizar);
                PreparedStatement historial =
                        conexion.prepareStatement(movimiento)
        ) {
            for (Map.Entry<Long, Long> entrada : cantidades.entrySet()) {
                long idProducto = entrada.getKey();
                long cantidad = entrada.getValue();

                lectura.setLong(1, idProducto);

                try (ResultSet resultado = lectura.executeQuery()) {
                    if (!resultado.next()) {
                        throw new IllegalArgumentException(
                                "Un producto del pedido ya no existe."
                        );
                    }

                    String nombre = resultado.getString("nombre");

                    if (!"S".equals(resultado.getString("activo"))) {
                        throw new IllegalArgumentException(
                                "El producto " + nombre + " esta desactivado."
                        );
                    }

                    if (resultado.getLong("stock") < cantidad) {
                        throw new IllegalArgumentException(
                                "Existencias insuficientes de " + nombre
                                + ". El pedido no fue cobrado."
                        );
                    }
                }

                cambio.setLong(1, cantidad);
                cambio.setLong(2, idProducto);

                if (cambio.executeUpdate() != 1) {
                    throw new SQLException(
                            "No se pudieron descontar las existencias."
                    );
                }

                historial.setLong(1, idProducto);
                historial.setLong(2, cantidad);
                historial.setLong(3, idPedido);
                historial.executeUpdate();
            }
        }
    }

    private void registrarPago(long idPedido, PagoEfectivo pago)
            throws SQLException {

        String sql = "INSERT INTO pagos "
                + "(id_pedido, metodo, monto, efectivo_recibido, cambio) "
                + "VALUES (?, 'EFECTIVO', ?, ?, ?)";

        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setLong(1, idPedido);
            sentencia.setBigDecimal(2, pago.getMonto());
            sentencia.setBigDecimal(3, pago.getRecibido());
            sentencia.setBigDecimal(4, pago.getCambio());
            sentencia.executeUpdate();
        }
    }
    
        private void registrarPagoTarjeta(
            long idPedido, PagoTarjeta pago) throws SQLException {

        String sql = "INSERT INTO pagos "
                + "(id_pedido, metodo, monto, referencia) "
                + "VALUES (?, 'TARJETA', ?, ?)";

        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setLong(1, idPedido);
            sentencia.setBigDecimal(2, pago.getMonto());
            sentencia.setString(3, pago.getReferencia());
            sentencia.executeUpdate();
        }
    }
    
    

    private void acreditarPuntos(long idCliente, long puntos)
            throws SQLException {

        String sql = "UPDATE clientes SET puntos = puntos + ? "
                + "WHERE id_cliente = ?";

        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setLong(1, puntos);
            sentencia.setLong(2, idCliente);

            if (sentencia.executeUpdate() != 1) {
                throw new SQLException(
                        "No se pudieron acreditar los puntos."
                );
            }
        }
    }

    private void marcarPagado(long idPedido, long puntos)
            throws SQLException {

        String sql = "UPDATE pedidos SET estado = 'PAGADO', "
                + "fecha_pago = SYSTIMESTAMP, puntos_ganados = ? "
                + "WHERE id_pedido = ? AND estado = 'PENDIENTE'";

        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setLong(1, puntos);
            sentencia.setLong(2, idPedido);

            if (sentencia.executeUpdate() != 1) {
                throw new SQLException(
                        "No se pudo actualizar el estado del pedido."
                );
            }
        }
    }
}