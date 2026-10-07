package panpuntospremios;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.SwingUtilities;

public class Principal {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> iniciar());
    }

    private static void iniciar() {
        JPasswordField campoClave = new JPasswordField();

        int respuesta = JOptionPane.showConfirmDialog(
                null,
                campoClave,
                "Escribe la contraseña de PAN_PREMIOS",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (respuesta != JOptionPane.OK_OPTION) {
            campoClave.setText("");
            return;
        }

        char[] clave = campoClave.getPassword();

        try (Connection conexion =
                ConexionBD.conectar(new String(clave))) {

            Arrays.fill(clave, '\0');
            campoClave.setText("");

            VentanaClientes ventana = new VentanaClientes(conexion);
            ventana.setVisible(true);

        } catch (SQLException error) {
            JOptionPane.showMessageDialog(
                    null,
                    "Error de conexion:\n" + error.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );
        } finally {
            Arrays.fill(clave, '\0');
            campoClave.setText("");
        }
    }
}