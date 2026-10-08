package panpuntospremios;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.TreeMap;

public class ValidadorStock {

    private final Connection conexion;

    public ValidadorStock(Connection conexion) {
        this.conexion = conexion;
    }

    public void validar(Pedido pedido) throws SQLException {
        if (pedido == null || pedido.estaVacio()) {
            throw new IllegalArgumentException(
                    "Agrega al menos un articulo al pedido."
            );
        }

        Map<Long, Long> cantidades = new TreeMap<>();

        for (DetallePedido detalle : pedido.getDetalles()) {
            ItemVenta articulo = detalle.getArticulo();
            long cantidad = detalle.getCantidad();

            if (articulo instanceof ProductoIndividual producto) {
                acumular(cantidades, producto.getId(), cantidad);

            } else if (articulo instanceof MenuCompleto menu) {
                validarMenu(menu);

                acumular(
                        cantidades,
                        menu.getSandwich().getId(),
                        cantidad
                );
                acumular(
                        cantidades,
                        menu.getBebida().getId(),
                        cantidad
                );
                acumular(
                        cantidades,
                        menu.getAcompanamiento().getId(),
                        cantidad
                );

            } else {
                throw new IllegalArgumentException(
                        "El tipo de articulo no es compatible."
                );
            }
        }

        String sql = "SELECT nombre, stock, activo "
                + "FROM productos WHERE id_producto = ?";

        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            for (Map.Entry<Long, Long> entrada : cantidades.entrySet()) {
                sentencia.setLong(1, entrada.getKey());

                try (ResultSet resultado = sentencia.executeQuery()) {
                    if (!resultado.next()) {
                        throw new IllegalArgumentException(
                                "Un producto del pedido ya no existe."
                        );
                    }

                    String nombre = resultado.getString("nombre");
                    long disponibles = resultado.getLong("stock");
                    long necesarias = entrada.getValue();

                    if (!"S".equals(resultado.getString("activo"))) {
                        throw new IllegalArgumentException(
                                "El producto " + nombre + " esta desactivado."
                        );
                    }

                    if (disponibles < necesarias) {
                        throw new IllegalArgumentException(
                                "Existencias insuficientes de " + nombre
                                + ". Necesarias: " + necesarias
                                + ". Disponibles: " + disponibles + "."
                        );
                    }
                }
            }
        }
    }

    private void acumular(
            Map<Long, Long> cantidades, long idProducto, long cantidad) {

        long actual = cantidades.getOrDefault(idProducto, 0L);
        cantidades.put(idProducto, Math.addExact(actual, cantidad));
    }

    private void validarMenu(MenuCompleto menu) throws SQLException {
        String sql = "SELECT activo, id_sandwich, id_bebida, "
                + "id_acompanamiento FROM menus WHERE id_menu = ?";

        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setLong(1, menu.getId());

            try (ResultSet resultado = sentencia.executeQuery()) {
                if (!resultado.next()) {
                    throw new IllegalArgumentException(
                            "El menu " + menu.getNombre() + " ya no existe."
                    );
                }

                if (!"S".equals(resultado.getString("activo"))) {
                    throw new IllegalArgumentException(
                            "El menu " + menu.getNombre()
                            + " esta desactivado."
                    );
                }

                if (resultado.getLong("id_sandwich")
                        != menu.getSandwich().getId()
                        || resultado.getLong("id_bebida")
                        != menu.getBebida().getId()
                        || resultado.getLong("id_acompanamiento")
                        != menu.getAcompanamiento().getId()) {

                    throw new IllegalArgumentException(
                            "Los componentes del menu cambiaron. "
                            + "Quitalo del pedido y agregalo nuevamente."
                    );
                }
            }
        }
    }
}