package panpuntospremios;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ProductoDAO {

    private final Connection conexion;

    public ProductoDAO(Connection conexion) {
        this.conexion = conexion;
    }

    public Producto buscarPorCodigo(String codigo) throws SQLException {
        if (codigo == null || codigo.isBlank()) {
            throw new IllegalArgumentException(
                    "Escribe el codigo del producto."
            );
        }

        String sql = "SELECT id_producto, codigo, categoria, nombre, "
                + "precio, stock, activo "
                + "FROM productos WHERE codigo = ?";

        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setString(1, codigo.trim());

            try (ResultSet resultado = sentencia.executeQuery()) {
                if (resultado.next()) {
                    Producto producto = new Producto(
                            resultado.getString("codigo"),
                            resultado.getString("categoria"),
                            resultado.getString("nombre"),
                            resultado.getBigDecimal("precio")
                    );

                    producto.setIdProducto(
                            resultado.getLong("id_producto")
                    );
                    producto.setStock(resultado.getLong("stock"));
                    producto.setActivo(
                            "S".equals(resultado.getString("activo"))
                    );

                    return producto;
                }
            }
        }

        return null;
    }

    public void registrar(Producto producto) throws SQLException {
        validarProducto(producto);

        String sql = "INSERT INTO productos "
                + "(codigo, categoria, nombre, precio) "
                + "VALUES (?, ?, ?, ?)";

        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setString(1, producto.getCodigo().trim());
            sentencia.setString(2, producto.getCategoria());
            sentencia.setString(3, producto.getNombre().trim());
            sentencia.setBigDecimal(4, producto.getPrecio());
            sentencia.executeUpdate();
        }
    }

    public void actualizar(Producto producto) throws SQLException {
        validarProducto(producto);
        validarId(producto.getIdProducto());

        String sql = "UPDATE productos SET nombre = ?, precio = ? "
                + "WHERE id_producto = ?";

        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setString(1, producto.getNombre().trim());
            sentencia.setBigDecimal(2, producto.getPrecio());
            sentencia.setLong(3, producto.getIdProducto());

            if (sentencia.executeUpdate() != 1) {
                throw new SQLException(
                        "No se encontro el producto para actualizar."
                );
            }
        }
    }

    public void cambiarEstado(long idProducto, boolean activo)
            throws SQLException {

        validarId(idProducto);

        String sql = "UPDATE productos SET activo = ? "
                + "WHERE id_producto = ?";

        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setString(1, activo ? "S" : "N");
            sentencia.setLong(2, idProducto);

            if (sentencia.executeUpdate() != 1) {
                throw new SQLException(
                        "No se encontro el producto para cambiar su estado."
                );
            }
        }
    }

    private void validarId(long idProducto) {
        if (idProducto <= 0) {
            throw new IllegalArgumentException(
                    "Primero busca el producto."
            );
        }
    }

    private void validarProducto(Producto producto) {
        if (producto == null) {
            throw new IllegalArgumentException(
                    "Debes proporcionar los datos del producto."
            );
        }

        String codigo = producto.getCodigo();
        String nombre = producto.getNombre();
        String categoria = producto.getCategoria();
        BigDecimal precio = producto.getPrecio();

        if (codigo == null || codigo.isBlank()) {
            throw new IllegalArgumentException(
                    "Escribe el codigo del producto."
            );
        }

        if (codigo.trim().length() > 30) {
            throw new IllegalArgumentException(
                    "El codigo admite un maximo de 30 caracteres."
            );
        }

        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException(
                    "Escribe el nombre del producto."
            );
        }

        if (nombre.trim().length() > 100) {
            throw new IllegalArgumentException(
                    "El nombre admite un maximo de 100 caracteres."
            );
        }

        if (!"SANDWICH".equals(categoria)
                && !"BEBIDA".equals(categoria)
                && !"ACOMPANAMIENTO".equals(categoria)) {
            throw new IllegalArgumentException(
                    "Selecciona una categoria valida."
            );
        }

        if (precio == null || precio.signum() < 0
                || precio.compareTo(new BigDecimal("99999999.99")) > 0) {
            throw new IllegalArgumentException(
                    "El precio debe estar entre 0 y 99999999.99."
            );
        }

        try {
            precio.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException error) {
            throw new IllegalArgumentException(
                    "El precio admite como maximo dos decimales."
            );
        }
    }
}