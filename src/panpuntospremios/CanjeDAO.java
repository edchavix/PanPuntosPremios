package panpuntospremios;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

public class CanjeDAO {

    private final Connection conexion;

    public CanjeDAO(Connection conexion) {
        this.conexion = conexion;
    }

    public long canjear(
            long idCliente, long idRecompensa, String codigoArticulo)
            throws SQLException {

        if (idCliente <= 0 || idRecompensa <= 0) {
            throw new IllegalArgumentException(
                    "Selecciona un cliente y una recompensa."
            );
        }

        if (codigoArticulo == null || codigoArticulo.isBlank()) {
            throw new IllegalArgumentException(
                    "Escribe el codigo del articulo del premio."
            );
        }

        if (!conexion.getAutoCommit()) {
            throw new SQLException(
                    "Hay una operacion pendiente en la conexion."
            );
        }

        conexion.setAutoCommit(false);

        try {
            long saldo = consultarSaldo(idCliente);
            Recompensa recompensa = consultarRecompensa(idRecompensa);

            if (saldo < recompensa.puntos) {
                throw new IllegalArgumentException(
                        "Puntos insuficientes. Se necesitan "
                        + recompensa.puntos + " y el cliente tiene "
                        + saldo + "."
                );
            }

            Map<Long, Long> cantidades = new LinkedHashMap<>();
            long idProducto = 0;
            long idMenu = 0;

            if ("MENU".equals(recompensa.categoria)) {
                idMenu = consultarMenu(codigoArticulo, cantidades);
            } else {
                idProducto = consultarProducto(
                        codigoArticulo,
                        recompensa.categoria,
                        cantidades
                );
            }

            validarYDescontarStock(cantidades);

            long idCanje = insertarCanje(
                    idCliente, idRecompensa, idProducto,
                    idMenu, recompensa.puntos
            );

            registrarMovimientos(idCanje, cantidades);
            descontarPuntos(idCliente, recompensa.puntos);

            conexion.commit();
            conexion.setAutoCommit(true);
            return idCanje;

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

    private long consultarSaldo(long idCliente) throws SQLException {
        String sql = "SELECT puntos, activo FROM clientes "
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

    private Recompensa consultarRecompensa(long idRecompensa)
            throws SQLException {

        String sql = "SELECT categoria, puntos_requeridos, activo "
                + "FROM recompensas WHERE id_recompensa = ? FOR UPDATE";

        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setLong(1, idRecompensa);

            try (ResultSet resultado = sentencia.executeQuery()) {
                if (!resultado.next()
                        || !"S".equals(resultado.getString("activo"))) {
                    throw new IllegalArgumentException(
                            "La recompensa no existe o esta desactivada."
                    );
                }

                return new Recompensa(
                        resultado.getString("categoria"),
                        resultado.getLong("puntos_requeridos")
                );
            }
        }
    }

    private long consultarProducto(
            String codigo, String categoriaRequerida,
            Map<Long, Long> cantidades) throws SQLException {

        String sql = "SELECT id_producto, categoria, activo "
                + "FROM productos WHERE codigo = ? FOR UPDATE";

        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setString(1, codigo.trim());

            try (ResultSet resultado = sentencia.executeQuery()) {
                if (!resultado.next()) {
                    throw new IllegalArgumentException(
                            "No existe el producto indicado."
                    );
                }

                if (!"S".equals(resultado.getString("activo"))) {
                    throw new IllegalArgumentException(
                            "El producto esta desactivado."
                    );
                }

                if (!categoriaRequerida.equals(
                        resultado.getString("categoria"))) {
                    throw new IllegalArgumentException(
                            "El producto no corresponde a la categoria "
                            + "de la recompensa."
                    );
                }

                long id = resultado.getLong("id_producto");
                cantidades.put(id, 1L);
                return id;
            }
        }
    }

    private long consultarMenu(
            String codigo, Map<Long, Long> cantidades)
            throws SQLException {

        String sql = "SELECT id_menu, id_sandwich, id_bebida, "
                + "id_acompanamiento, activo "
                + "FROM menus WHERE codigo = ? FOR UPDATE";

        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setString(1, codigo.trim());

            try (ResultSet resultado = sentencia.executeQuery()) {
                if (!resultado.next()
                        || !"S".equals(resultado.getString("activo"))) {
                    throw new IllegalArgumentException(
                            "El menu no existe o esta desactivado."
                    );
                }

                long idSandwich = resultado.getLong("id_sandwich");
                long idBebida = resultado.getLong("id_bebida");
                long idAcompanamiento =
                        resultado.getLong("id_acompanamiento");

                agregarCantidad(cantidades, idSandwich);
                agregarCantidad(cantidades, idBebida);
                agregarCantidad(cantidades, idAcompanamiento);

                return resultado.getLong("id_menu");
            }
        }
    }

    private void agregarCantidad(
            Map<Long, Long> cantidades, long idProducto) {

        cantidades.put(
                idProducto,
                Math.addExact(cantidades.getOrDefault(idProducto, 0L), 1L)
        );
    }

    private void validarYDescontarStock(
            Map<Long, Long> cantidades) throws SQLException {

        String consulta = "SELECT nombre, stock, activo "
                + "FROM productos WHERE id_producto = ? FOR UPDATE";

        String actualizar = "UPDATE productos "
                + "SET stock = stock - ? WHERE id_producto = ?";

        try (
                PreparedStatement lectura =
                        conexion.prepareStatement(consulta);
                PreparedStatement cambio =
                        conexion.prepareStatement(actualizar)
        ) {
            for (Map.Entry<Long, Long> entrada : cantidades.entrySet()) {
                lectura.setLong(1, entrada.getKey());

                try (ResultSet resultado = lectura.executeQuery()) {
                    if (!resultado.next()
                            || !"S".equals(resultado.getString("activo"))) {
                        throw new IllegalArgumentException(
                                "Un componente del premio esta desactivado."
                        );
                    }

                    if (resultado.getLong("stock") < entrada.getValue()) {
                        throw new IllegalArgumentException(
                                "Existencias insuficientes de "
                                + resultado.getString("nombre") + "."
                        );
                    }
                }

                cambio.setLong(1, entrada.getValue());
                cambio.setLong(2, entrada.getKey());
                cambio.executeUpdate();
            }
        }
    }

    private long insertarCanje(
            long idCliente, long idRecompensa, long idProducto,
            long idMenu, long puntos) throws SQLException {

        String sql = "INSERT INTO canjes "
                + "(id_cliente, id_recompensa, id_producto, id_menu, "
                + "puntos_utilizados) VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement sentencia = conexion.prepareStatement(
                sql, new String[]{"ID_CANJE"})) {

            sentencia.setLong(1, idCliente);
            sentencia.setLong(2, idRecompensa);

            if (idProducto == 0) {
                sentencia.setNull(3, java.sql.Types.NUMERIC);
            } else {
                sentencia.setLong(3, idProducto);
            }

            if (idMenu == 0) {
                sentencia.setNull(4, java.sql.Types.NUMERIC);
            } else {
                sentencia.setLong(4, idMenu);
            }

            sentencia.setLong(5, puntos);
            sentencia.executeUpdate();

            try (ResultSet claves = sentencia.getGeneratedKeys()) {
                if (!claves.next()) {
                    throw new SQLException(
                            "No se pudo obtener el numero del canje."
                    );
                }

                return claves.getLong(1);
            }
        }
    }

    private void registrarMovimientos(
            long idCanje, Map<Long, Long> cantidades)
            throws SQLException {

        String sql = "INSERT INTO movimientos_inventario "
                + "(id_producto, tipo, cantidad, id_canje) "
                + "VALUES (?, 'CANJE', ?, ?)";

        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            for (Map.Entry<Long, Long> entrada : cantidades.entrySet()) {
                sentencia.setLong(1, entrada.getKey());
                sentencia.setLong(2, entrada.getValue());
                sentencia.setLong(3, idCanje);
                sentencia.executeUpdate();
            }
        }
    }

    private void descontarPuntos(long idCliente, long puntos)
            throws SQLException {

        String sql = "UPDATE clientes SET puntos = puntos - ? "
                + "WHERE id_cliente = ? AND puntos >= ?";

        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setLong(1, puntos);
            sentencia.setLong(2, idCliente);
            sentencia.setLong(3, puntos);

            if (sentencia.executeUpdate() != 1) {
                throw new IllegalArgumentException(
                        "El saldo cambio. Vuelve a consultar los puntos."
                );
            }
        }
    }

    private static class Recompensa {

        private final String categoria;
        private final long puntos;

        private Recompensa(String categoria, long puntos) {
            this.categoria = categoria;
            this.puntos = puntos;
        }
    }
}