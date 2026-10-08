package panpuntospremios;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class PedidoDAO {

    private final Connection conexion;

    public PedidoDAO(Connection conexion) {
        this.conexion = conexion;
    }

    public long guardar(Pedido pedido) throws SQLException {
        if (pedido == null || pedido.estaVacio()) {
            throw new IllegalArgumentException(
                    "Agrega al menos un articulo al pedido."
            );
        }

        if (pedido.calcularTotal().compareTo(
                new BigDecimal("9999999999.99")) > 0) {
            throw new IllegalArgumentException(
                    "El total supera el limite permitido."
            );
        }

        if (pedido.calcularPuntos() > 9999999999L) {
            throw new IllegalArgumentException(
                    "Los puntos del pedido superan el limite permitido."
            );
        }

        if (!conexion.getAutoCommit()) {
            throw new SQLException(
                    "Hay una operacion pendiente en la conexion."
            );
        }

        conexion.setAutoCommit(false);
        long idPedido;

        try {
            validarCliente(pedido.getCliente().getIdCliente());
            new ValidadorStock(conexion).validar(pedido);

            String sqlPedido = "INSERT INTO pedidos "
                    + "(id_cliente, estado, total, puntos_ganados) "
                    + "VALUES (?, 'PENDIENTE', ?, 0)";

            try (PreparedStatement sentencia = conexion.prepareStatement(
                    sqlPedido, new String[]{"ID_PEDIDO"})) {

                sentencia.setLong(
                        1, pedido.getCliente().getIdCliente()
                );
                sentencia.setBigDecimal(2, pedido.calcularTotal());
                sentencia.executeUpdate();

                try (ResultSet claves = sentencia.getGeneratedKeys()) {
                    if (!claves.next()) {
                        throw new SQLException(
                                "No se pudo obtener el numero del pedido."
                        );
                    }

                    idPedido = claves.getLong(1);
                }
            }

            String sqlDetalle = "INSERT INTO detalle_pedido "
                    + "(id_pedido, id_producto, id_menu, cantidad, "
                    + "precio_unitario, puntos_unitarios) "
                    + "VALUES (?, ?, ?, ?, ?, ?)";

            try (PreparedStatement sentencia =
                    conexion.prepareStatement(sqlDetalle)) {

                for (DetallePedido detalle : pedido.getDetalles()) {
                    ItemVenta articulo = detalle.getArticulo();

                    sentencia.setLong(1, idPedido);

                    if (articulo instanceof ProductoIndividual) {
                        sentencia.setLong(2, articulo.getId());
                        sentencia.setNull(3, java.sql.Types.NUMERIC);

                    } else if (articulo instanceof MenuCompleto) {
                        sentencia.setNull(2, java.sql.Types.NUMERIC);
                        sentencia.setLong(3, articulo.getId());

                    } else {
                        throw new IllegalArgumentException(
                                "El tipo de articulo no es compatible."
                        );
                    }

                    sentencia.setInt(4, detalle.getCantidad());
                    sentencia.setBigDecimal(5, articulo.getPrecio());
                    sentencia.setInt(6, articulo.calcularPuntos());
                    sentencia.executeUpdate();
                }
            }

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
        return idPedido;
    }

    private void validarCliente(long idCliente) throws SQLException {
        String sql = "SELECT activo FROM clientes "
                + "WHERE id_cliente = ? FOR UPDATE";

        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setLong(1, idCliente);

            try (ResultSet resultado = sentencia.executeQuery()) {
                if (!resultado.next()) {
                    throw new IllegalArgumentException(
                            "El cliente ya no existe."
                    );
                }

                if (!"S".equals(resultado.getString("activo"))) {
                    throw new IllegalArgumentException(
                            "El cliente esta desactivado."
                    );
                }
            }
        }
    }
}