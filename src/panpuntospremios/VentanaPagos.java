package panpuntospremios;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class VentanaPagos extends JDialog {

    private final PagoDAO pagoDAO;

    private final JTextField campoPedido = new JTextField();
    private final JTextField campoRecibido = new JTextField();
    private final JTextArea resultado = new JTextArea(10, 38);

    public VentanaPagos(Connection conexion) {
        pagoDAO = new PagoDAO(conexion);

        setTitle("Pan, Puntos y Premios - Pago en efectivo");
        setModal(true);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        JPanel contenido = new JPanel(new BorderLayout(10, 15));
        contenido.setBorder(
                BorderFactory.createEmptyBorder(20, 20, 20, 20)
        );

        JPanel datos = new JPanel(new GridLayout(2, 2, 10, 10));
        datos.add(new JLabel("Numero de pedido:"));
        datos.add(campoPedido);
        datos.add(new JLabel("Efectivo recibido:"));
        datos.add(campoRecibido);

        resultado.setEditable(false);
        resultado.setLineWrap(true);
        resultado.setWrapStyleWord(true);

        JPanel botones = new JPanel();
        JButton botonCobrar = new JButton("Cobrar");
        JButton botonCerrar = new JButton("Cerrar");

        botones.add(botonCobrar);
        botones.add(botonCerrar);

        botonCobrar.addActionListener(evento -> cobrar());
        botonCerrar.addActionListener(evento -> dispose());

        contenido.add(datos, BorderLayout.NORTH);
        contenido.add(new JScrollPane(resultado), BorderLayout.CENTER);
        contenido.add(botones, BorderLayout.SOUTH);

        setContentPane(contenido);
        pack();
        setLocationRelativeTo(null);
    }

    private void cobrar() {
        String textoPedido = campoPedido.getText().trim();
        String textoRecibido = campoRecibido.getText().trim();

        if (!textoPedido.matches("[0-9]{1,18}")) {
            mostrarAviso("Escribe un numero de pedido valido.");
            return;
        }

        long idPedido = Long.parseLong(textoPedido);

        if (idPedido <= 0) {
            mostrarAviso("El numero de pedido debe ser mayor que cero.");
            return;
        }

        if (!textoRecibido.matches("[0-9]{1,10}(\\.[0-9]{1,2})?")) {
            mostrarAviso(
                    "Escribe el efectivo recibido con hasta dos "
                    + "decimales. Ejemplo: 100.00"
            );
            return;
        }

        BigDecimal recibido = new BigDecimal(textoRecibido);

        int respuesta = JOptionPane.showConfirmDialog(
                this,
                "¿Confirmas cobrar el pedido " + idPedido
                + " con " + recibido.toPlainString()
                + " de efectivo recibido?",
                "Confirmar cobro",
                JOptionPane.YES_NO_OPTION
        );

        if (respuesta != JOptionPane.YES_OPTION) {
            return;
        }

        resultado.setText("");

        try {
            PagoEfectivo pago = pagoDAO.cobrarEfectivo(
                    idPedido, recibido
            );

            resultado.setText(
                    "PAGO REGISTRADO"
                    + "\n\nPedido: " + idPedido
                    + "\nEstado: PAGADO"
                    + "\nMetodo: EFECTIVO"
                    + "\nTotal: " + pago.getMonto().toPlainString()
                    + "\nRecibido: " + pago.getRecibido().toPlainString()
                    + "\nCambio: " + pago.getCambio().toPlainString()
                    + "\n\nExistencias y puntos actualizados en Oracle."
            );

            campoPedido.setText("");
            campoRecibido.setText("");

            JOptionPane.showMessageDialog(
                    this,
                    "Pago registrado correctamente."
                    + "\nCambio: " + pago.getCambio().toPlainString(),
                    "Cobro exitoso",
                    JOptionPane.INFORMATION_MESSAGE
            );

        } catch (IllegalArgumentException error) {
            mostrarAviso(error.getMessage());
        } catch (SQLException error) {
            JOptionPane.showMessageDialog(
                    this,
                    "No se pudo completar la operacion:\n"
                    + error.getMessage()
                    + "\nConsulta el estado del pedido antes de reintentar.",
                    "Error de base de datos",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void mostrarAviso(String mensaje) {
        JOptionPane.showMessageDialog(
                this,
                mensaje,
                "Revisa el pago",
                JOptionPane.WARNING_MESSAGE
        );
    }
}