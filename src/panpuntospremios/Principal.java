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

            mostrarMenu(conexion);

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

    private static void mostrarMenu(Connection conexion) {
        String[] opciones = {
            "Clientes", "Productos", "Inventario", "Pedidos", "Salir"
        };

        while (true) {
            int opcion = JOptionPane.showOptionDialog(
                    null,
                    "Selecciona el modulo que deseas abrir.",
                    "Pan, Puntos y Premios",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    opciones,
                    opciones[0]
            );

            if (opcion == 0) {
                new VentanaClientes(conexion).setVisible(true);
            } else if (opcion == 1) {
                new VentanaProductos(conexion).setVisible(true);
            } else if (opcion == 2) {
                new VentanaInventario(conexion).setVisible(true);
            } else if (opcion == 3) {
                new VentanaPedidos(conexion).setVisible(true);
            } else {
                return;
            }
        }
    }
}