package panpuntospremios;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.SwingUtilities;

public class Principal {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> probarConexion());
    }

    private static void probarConexion() {
        JPasswordField campoClave = new JPasswordField();

        int respuesta = JOptionPane.showConfirmDialog(
                null,
                campoClave,
                "Escribe la contraseña de PAN_PREMIOS",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (respuesta != JOptionPane.OK_OPTION) {
            return;
        }

        char[] clave = campoClave.getPassword();

        String consulta = "SELECT USER AS usuario, "
                + "SYS_CONTEXT('USERENV', 'CON_NAME') AS base FROM dual";

        try (
                Connection conexion = ConexionBD.conectar(new String(clave));
                PreparedStatement sentencia = conexion.prepareStatement(consulta);
                ResultSet resultado = sentencia.executeQuery()
        ) {
            if (resultado.next()) {
                JOptionPane.showMessageDialog(
                        null,
                        "Conexión correcta con Oracle."
                        + "\nUsuario: " + resultado.getString("usuario")
                        + "\nBase: " + resultado.getString("base"),
                        "Pan, Puntos y Premios",
                        JOptionPane.INFORMATION_MESSAGE
                );
            }
        } catch (SQLException error) {
            JOptionPane.showMessageDialog(
                    null,
                    "No se pudo conectar:\n" + error.getMessage(),
                    "Error de conexión",
                    JOptionPane.ERROR_MESSAGE
            );
        } finally {
            Arrays.fill(clave, '\0');
            campoClave.setText("");
        }
    }
}
