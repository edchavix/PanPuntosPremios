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
        if (cliente == null) {
            throw new IllegalArgumentException(
                    "Debes proporcionar los datos del cliente."
            );
        }

        String identificacion = cliente.getIdentificacion();
        String nombre = cliente.getNombre();
        String telefono = cliente.getTelefono();
        String correo = cliente.getCorreo();

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

        identificacion = identificacion.trim();
        nombre = nombre.trim();
        telefono = telefono == null ? null : telefono.trim();
        correo = correo == null ? null : correo.trim();

        if (identificacion.length() > 30 || nombre.length() > 100) {
            throw new IllegalArgumentException(
                    "La identificacion admite 30 caracteres "
                    + "y el nombre 100 como maximo."
            );
        }

        if ((telefono != null && telefono.length() > 20)
                || (correo != null && correo.length() > 100)) {
            throw new IllegalArgumentException(
                    "El telefono admite 20 caracteres "
                    + "y el correo 100 como maximo."
            );
        }

        String sql = "INSERT INTO clientes "
                + "(identificacion, nombre, telefono, correo) "
                + "VALUES (?, ?, ?, ?)";

        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setString(1, identificacion);
            sentencia.setString(2, nombre);
            sentencia.setString(3, telefono);
            sentencia.setString(4, correo);
            sentencia.executeUpdate();
        }
    }
}
