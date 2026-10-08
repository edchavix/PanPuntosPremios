package panpuntospremios;

import java.sql.Connection;
import java.sql.SQLException;

public class CatalogoDAO {

    private final ProductoDAO productoDAO;
    private final MenuDAO menuDAO;

    public CatalogoDAO(Connection conexion) {
        productoDAO = new ProductoDAO(conexion);
        menuDAO = new MenuDAO(conexion);
    }

    public ItemVenta buscar(String tipo, String codigo)
            throws SQLException {

        if ("PRODUCTO".equals(tipo)) {
            return buscarProducto(codigo);
        }

        if ("MENU".equals(tipo)) {
            return menuDAO.buscarPorCodigo(codigo);
        }

        throw new IllegalArgumentException(
                "Selecciona PRODUCTO o MENU."
        );
    }

    private ProductoIndividual buscarProducto(String codigo)
            throws SQLException {

        Producto producto = productoDAO.buscarPorCodigo(codigo);

        if (producto == null) {
            return null;
        }

        if (!producto.isActivo()) {
            throw new IllegalArgumentException(
                    "El producto esta desactivado."
            );
        }

        return new ProductoIndividual(
                producto.getIdProducto(),
                producto.getCodigo(),
                producto.getNombre(),
                producto.getPrecio(),
                producto.getCategoria()
        );
    }
}