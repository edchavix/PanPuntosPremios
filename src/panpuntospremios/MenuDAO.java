package panpuntospremios;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class MenuDAO {

    private final Connection conexion;

    public MenuDAO(Connection conexion) {
        this.conexion = conexion;
    }

    public MenuCompleto buscarPorCodigo(String codigo)
            throws SQLException {

        if (codigo == null || codigo.isBlank()) {
            throw new IllegalArgumentException(
                    "Escribe el codigo del menu."
            );
        }

        String sql = """
                SELECT m.id_menu, m.codigo, m.nombre, m.precio,
                       m.activo,
                       s.id_producto AS s_id,
                       s.codigo AS s_codigo,
                       s.nombre AS s_nombre,
                       s.precio AS s_precio,
                       s.categoria AS s_categoria,
                       s.activo AS s_activo,
                       b.id_producto AS b_id,
                       b.codigo AS b_codigo,
                       b.nombre AS b_nombre,
                       b.precio AS b_precio,
                       b.categoria AS b_categoria,
                       b.activo AS b_activo,
                       a.id_producto AS a_id,
                       a.codigo AS a_codigo,
                       a.nombre AS a_nombre,
                       a.precio AS a_precio,
                       a.categoria AS a_categoria,
                       a.activo AS a_activo
                FROM menus m
                JOIN productos s ON s.id_producto = m.id_sandwich
                JOIN productos b ON b.id_producto = m.id_bebida
                JOIN productos a ON a.id_producto = m.id_acompanamiento
                WHERE m.codigo = ?
                """;

        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setString(1, codigo.trim());

            try (ResultSet resultado = sentencia.executeQuery()) {
                if (!resultado.next()) {
                    return null;
                }

                if (!"S".equals(resultado.getString("activo"))) {
                    throw new IllegalArgumentException(
                            "El menu esta desactivado."
                    );
                }

                if (!"S".equals(resultado.getString("s_activo"))
                        || !"S".equals(resultado.getString("b_activo"))
                        || !"S".equals(resultado.getString("a_activo"))) {
                    throw new IllegalArgumentException(
                            "El menu tiene un componente desactivado."
                    );
                }

                ProductoIndividual sandwich =
                        leerComponente(resultado, "s");

                ProductoIndividual bebida =
                        leerComponente(resultado, "b");

                ProductoIndividual acompanamiento =
                        leerComponente(resultado, "a");

                return new MenuCompleto(
                        resultado.getLong("id_menu"),
                        resultado.getString("codigo"),
                        resultado.getString("nombre"),
                        resultado.getBigDecimal("precio"),
                        sandwich,
                        bebida,
                        acompanamiento
                );
            }
        }
    }

    private ProductoIndividual leerComponente(
            ResultSet resultado, String prefijo) throws SQLException {

        return new ProductoIndividual(
                resultado.getLong(prefijo + "_id"),
                resultado.getString(prefijo + "_codigo"),
                resultado.getString(prefijo + "_nombre"),
                resultado.getBigDecimal(prefijo + "_precio"),
                resultado.getString(prefijo + "_categoria")
        );
    }
}