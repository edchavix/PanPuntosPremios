package panpuntospremios;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class InventarioDAO {

    private final Connection conexion;

    public InventarioDAO(Connection conexion) {
        this.conexion = conexion;
    }

    public void reponer(String codigo, long cantidad) throws SQLException {
        if (codigo == null || codigo.isBlank()) {
            throw new IllegalArgumentException(
                    "Escribe el codigo del producto."
            );
        }

        if (cantidad <= 0 || cantidad > 9999999999L) {
            throw new IllegalArgumentException(
                    "La cantidad debe ser un entero positivo "
                    + "de hasta 10 digitos."
            );
        }

        if (!conexion.getAutoCommit()) {
            throw new SQLException(
                    "Hay una operacion pendiente en la conexion."
            );
        }

        conexion.setAutoCommit(false);

        try {
            long idProducto;
            long stockActual;
            String activo;

            String consulta = "SELECT id_producto, stock, activo "
                    + "FROM productos WHERE codigo = ? FOR UPDATE";

            try (PreparedStatement sentencia =
                    conexion.prepareStatement(consulta)) {

                sentencia.setString(1, codigo.trim());

                try (ResultSet resultado = sentencia.executeQuery()) {
                    if (!resultado.next()) {
                        throw new IllegalArgumentException(
                                "No existe un producto con ese codigo."
                        );
                    }

                    idProducto = resultado.getLong("id_producto");
                    stockActual = resultado.getLong("stock");
                    activo = resultado.getString("activo");
                }
            }

            if (!"S".equals(activo)) {
                throw new IllegalArgumentException(
                        "Activa el producto antes de registrar "
                        + "una reposicion."
                );
            }

            if (stockActual + cantidad > 9999999999L) {
                throw new IllegalArgumentException(
                        "La reposicion supera el limite de existencias."
                );
            }

            String actualizar = "UPDATE productos "
                    + "SET stock = stock + ? WHERE id_producto = ?";

            try (PreparedStatement sentencia =
                    conexion.prepareStatement(actualizar)) {

                sentencia.setLong(1, cantidad);
                sentencia.setLong(2, idProducto);

                if (sentencia.executeUpdate() != 1) {
                    throw new SQLException(
                            "No se pudieron actualizar las existencias."
                    );
                }
            }

            String movimiento = "INSERT INTO movimientos_inventario "
                    + "(id_producto, tipo, cantidad) "
                    + "VALUES (?, 'REPOSICION', ?)";

            try (PreparedStatement sentencia =
                    conexion.prepareStatement(movimiento)) {

                sentencia.setLong(1, idProducto);
                sentencia.setLong(2, cantidad);
                sentencia.executeUpdate();
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
    }
}