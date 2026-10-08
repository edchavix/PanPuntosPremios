package panpuntospremios;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.SwingUtilities;

public class PruebaStock {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> probar());
    }

    private static void probar() {
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

            ClienteDAO clienteDAO = new ClienteDAO(conexion);
            CatalogoDAO catalogoDAO = new CatalogoDAO(conexion);
            ProductoDAO productoDAO = new ProductoDAO(conexion);
            ValidadorStock validador = new ValidadorStock(conexion);

            Cliente cliente =
                    clienteDAO.buscarPorIdentificacion("PRUEBA001");

            ItemVenta sandwich = catalogoDAO.buscar("PRODUCTO", "SAN001");
            ItemVenta menu = catalogoDAO.buscar("MENU", "MEN001");

            if (cliente == null || sandwich == null || menu == null) {
                throw new IllegalArgumentException(
                        "Deben existir PRUEBA001, SAN001 y MEN001."
                );
            }

            if (!(menu instanceof MenuCompleto menuCompleto)
                    || menuCompleto.getSandwich().getId()
                    != sandwich.getId()) {
                throw new IllegalArgumentException(
                        "MEN001 debe contener el sandwich SAN001 "
                        + "para esta prueba."
                );
            }

            Producto producto =
                    productoDAO.buscarPorCodigo("SAN001");
            long stockAntes = producto.getStock();

            if (stockAntes < 1 || stockAntes > Integer.MAX_VALUE) {
                throw new IllegalArgumentException(
                        "La prueba requiere existencias de SAN001 "
                        + "entre 1 y " + Integer.MAX_VALUE + "."
                );
            }

            Pedido pedido = new Pedido(cliente);
            pedido.agregarArticulo(sandwich, (int) stockAntes);

            validador.validar(pedido);

            pedido.agregarArticulo(menu, 1);

            String rechazo = null;

            try {
                validador.validar(pedido);
            } catch (IllegalArgumentException error) {
                rechazo = error.getMessage();
            }

            if (rechazo == null) {
                throw new IllegalStateException(
                        "La prueba fallo: se permitio superar "
                        + "las existencias disponibles."
                );
            }

            pedido.quitarDetalle(1);
            validador.validar(pedido);

            long stockDespues = productoDAO
                    .buscarPorCodigo("SAN001").getStock();

            if (stockAntes != stockDespues) {
                throw new IllegalStateException(
                        "Las existencias cambiaron durante la prueba."
                );
            }

            JOptionPane.showMessageDialog(
                    null,
                    "PRUEBA COMPLETADA"
                    + "\n\nPedido con " + stockAntes
                    + " sandwiches: validado."
                    + "\nAl agregar un menu: rechazado."
                    + "\nMotivo: " + rechazo
                    + "\nAl quitar el menu: validado nuevamente."
                    + "\nExistencias antes y despues: " + stockDespues
                    + "\n\nNo se guardaron pedidos ni se desconto stock.",
                    "Prueba de existencias compartidas",
                    JOptionPane.INFORMATION_MESSAGE
            );

        } catch (SQLException | RuntimeException error) {
            JOptionPane.showMessageDialog(
                    null,
                    "No se pudo completar la prueba:\n"
                    + error.getMessage(),
                    "Resultado de la prueba",
                    JOptionPane.ERROR_MESSAGE
            );
        } finally {
            Arrays.fill(clave, '\0');
            campoClave.setText("");
        }
    }
}