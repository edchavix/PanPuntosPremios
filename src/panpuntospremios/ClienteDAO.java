package panpuntospremios;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ClienteDAO {

    private final Connection conexion;

    public ClienteDAO(Connection conexion) {
        this.conexion = conexion;
    }

    public Cliente buscarPorIdentificacion(String identificacion)
            throws SQLException {

        if (identificacion == null || identificacion.isBlank()) {
            throw new IllegalArgumentException(
                    "Escribe la identificacion del cliente."
            );
        }

        String sql = "SELECT id_cliente, identificacion, nombre, "
                + "telefono, correo, puntos, activo "
                + "FROM clientes WHERE identificacion = ?";

        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setString(1, identificacion.trim());

            try (ResultSet resultado = sentencia.executeQuery()) {
                if (resultado.next()) {
                    Cliente cliente = new Cliente(
                            resultado.getString("identificacion"),
                            resultado.getString("nombre"),
                            resultado.getString("telefono"),
                            resultado.getString("correo")
                    );

                    cliente.setIdCliente(resultado.getLong("id_cliente"));
                    cliente.setPuntos(resultado.getLong("puntos"));
                    cliente.setActivo(
                            "S".equals(resultado.getString("activo"))
                    );

                    return cliente;
                }
            }
        }

        return null;
    }

    public void registrar(Cliente cliente) throws SQLException {
        validarCliente(cliente);

        String sql = "INSERT INTO clientes "
                + "(identificacion, nombre, telefono, correo) "
                + "VALUES (?, ?, ?, ?)";

        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setString(1, cliente.getIdentificacion().trim());
            sentencia.setString(2, cliente.getNombre().trim());
            sentencia.setString(3, limpiarOpcional(cliente.getTelefono()));
            sentencia.setString(4, limpiarOpcional(cliente.getCorreo()));
            sentencia.executeUpdate();
        }
    }

    public void actualizar(Cliente cliente) throws SQLException {
        validarCliente(cliente);

        if (cliente.getIdCliente() <= 0) {
            throw new IllegalArgumentException(
                    "Primero busca el cliente que deseas modificar."
            );
        }

        String sql = "UPDATE clientes "
                + "SET nombre = ?, telefono = ?, correo = ? "
                + "WHERE id_cliente = ?";

        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setString(1, cliente.getNombre().trim());
            sentencia.setString(2, limpiarOpcional(cliente.getTelefono()));
            sentencia.setString(3, limpiarOpcional(cliente.getCorreo()));
            sentencia.setLong(4, cliente.getIdCliente());

            int filas = sentencia.executeUpdate();

            if (filas != 1) {
                throw new SQLException(
                        "No se encontro el cliente para actualizar."
                );
            }
        }
    }

    private void validarCliente(Cliente cliente) {
        if (cliente == null) {
            throw new IllegalArgumentException(
                    "Debes proporcionar los datos del cliente."
            );
        }

        String identificacion = cliente.getIdentificacion();
        String nombre = cliente.getNombre();
        String telefono = limpiarOpcional(cliente.getTelefono());
        String correo = limpiarOpcional(cliente.getCorreo());

        if (identificacion == null || identificacion.isBlank()) {
            throw new IllegalArgumentException(
                    "Escribe la identificacion del cliente."
            );
        }

        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException(
                    "Escribe el nombre del cliente."
            );
        }

        if (identificacion.trim().length() > 30
                || nombre.trim().length() > 100) {
            throw new IllegalArgumentException(
                    "La identificacion admite 30 caracteres "
                    + "y el nombre 100 como maximo."
            );
        }

        if (telefono != null) {
            if (telefono.length() > 20
                    || !telefono.matches("[+0-9()\\s-]+")
                    || !telefono.matches(".*[0-9].*")) {
                throw new IllegalArgumentException(
                        "El telefono debe contener numeros y puede incluir "
                        + "espacios, +, guiones o parentesis. Maximo 20 caracteres."
                );
            }
        }

        if (correo != null) {
            if (correo.length() > 100
                    || !correo.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
                throw new IllegalArgumentException(
                        "Escribe un correo con formato nombre@dominio.com "
                        + "y un maximo de 100 caracteres."
                );
            }
        }
    }

    private String limpiarOpcional(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }

        return valor.trim();
    }
}