package panpuntospremios;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
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

    private final JComboBox<String> campoMetodo = new JComboBox<>(
            new String[]{"EFECTIVO", "TARJETA SIMULADA"}
    );

    private final JTextField campoRecibido = new JTextField();
    private final JTextField campoReferencia = new JTextField();

    private final JComboBox<String> campoResultado = new JComboBox<>(
            new String[]{"RECHAZADA", "APROBADA"}
    );

    private final JTextArea resultado = new JTextArea(12, 42);

    public VentanaPagos(Connection conexion) {
        pagoDAO = new PagoDAO(conexion);

        setTitle("Pan, Puntos y Premios - Pagos");
        setModal(true);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        JPanel contenido = new JPanel(new BorderLayout(10, 15));
        contenido.setBorder(
                BorderFactory.createEmptyBorder(20, 20, 20, 20)
        );

        JPanel datos = new JPanel(new GridLayout(5, 2, 10, 10));

        datos.add(new JLabel("Numero de pedido:"));
        datos.add(campoPedido);
        datos.add(new JLabel("Metodo de pago:"));
        datos.add(campoMetodo);
        datos.add(new JLabel("Efectivo recibido:"));
        datos.add(campoRecibido);
        datos.add(new JLabel("Referencia simulada:"));
        datos.add(campoReferencia);
        datos.add(new JLabel("Resultado de simulacion:"));
        datos.add(campoResultado);

        resultado.setEditable(false);
        resultado.setLineWrap(true);
        resultado.setWrapStyleWord(true);

        JPanel botones = new JPanel();
        JButton botonCobrar = new JButton("Cobrar");
        JButton botonCerrar = new JButton("Cerrar");

        botones.add(botonCobrar);
        botones.add(botonCerrar);

        campoMetodo.addActionListener(evento -> actualizarMetodo());
        botonCobrar.addActionListener(evento -> cobrar());
        botonCerrar.addActionListener(evento -> dispose());

        contenido.add(datos, BorderLayout.NORTH);
        contenido.add(new JScrollPane(resultado), BorderLayout.CENTER);
        contenido.add(botones, BorderLayout.SOUTH);

        actualizarMetodo();

        setContentPane(contenido);
        pack();
        setLocationRelativeTo(null);
    }

    private void actualizarMetodo() {
        boolean efectivo = campoMetodo.getSelectedIndex() == 0;

        campoRecibido.setEnabled(efectivo);
        campoReferencia.setEnabled(!efectivo);
        campoResultado.setEnabled(!efectivo);
    }

    private void cobrar() {
        String textoPedido = campoPedido.getText().trim();

        if (!textoPedido.matches("[0-9]{1,18}")) {
            mostrarAviso("Escribe un numero de pedido valido.");
            return;
        }

        long idPedido = Long.parseLong(textoPedido);

        if (idPedido <= 0) {
            mostrarAviso("El numero de pedido debe ser mayor que cero.");
            return;
        }

        boolean efectivo = campoMetodo.getSelectedIndex() == 0;
        BigDecimal recibido = null;
        String referencia = campoReferencia.getText().trim();
        boolean aprobado = "APROBADA".equals(
                campoResultado.getSelectedItem()
        );

        if (efectivo) {
            String textoRecibido = campoRecibido.getText().trim();

            if (!textoRecibido.matches("[0-9]{1,10}(\\.[0-9]{1,2})?")) {
                mostrarAviso(
                        "Escribe el efectivo recibido con hasta dos "
                        + "decimales. Ejemplo: 100.00"
                );
                return;
            }

            recibido = new BigDecimal(textoRecibido);

        } else if (referencia.isBlank() || referencia.length() > 100) {
            mostrarAviso(
                    "Escribe una referencia simulada de hasta "
                    + "100 caracteres. Ejemplo: SIM-001"
            );
            return;
        }

        String mensaje;

        if (efectivo) {
            mensaje = "¿Cobrar el pedido " + idPedido
                    + " con " + recibido.toPlainString()
                    + " de efectivo recibido?";
        } else {
            mensaje = "¿Procesar el pedido " + idPedido
                    + " con tarjeta simulada?"
                    + "\nReferencia: " + referencia
                    + "\nResultado simulado: "
                    + (aprobado ? "APROBADA" : "RECHAZADA");
        }

        int respuesta = JOptionPane.showConfirmDialog(
                this,
                mensaje,
                "Confirmar operacion",
                JOptionPane.YES_NO_OPTION
        );

        if (respuesta != JOptionPane.YES_OPTION) {
            return;
        }

        resultado.setText("");

        try {
            if (efectivo) {
                PagoEfectivo pago = pagoDAO.cobrarEfectivo(
                        idPedido, recibido
                );

                resultado.setText(
                        "PAGO REGISTRADO"
                        + "\n\nPedido: " + idPedido
                        + "\nEstado: PAGADO"
                        + "\nMetodo: EFECTIVO"
                        + "\nTotal: " + pago.getMonto().toPlainString()
                        + "\nRecibido: "
                        + pago.getRecibido().toPlainString()
                        + "\nCambio: " + pago.getCambio().toPlainString()
                        + "\n\nExistencias y puntos actualizados en Oracle."
                );

            } else {
                PagoTarjeta pago = pagoDAO.cobrarTarjeta(
                        idPedido, referencia, aprobado
                );

                resultado.setText(
                        "PAGO REGISTRADO"
                        + "\n\nPedido: " + idPedido
                        + "\nEstado: PAGADO"
                        + "\nMetodo: TARJETA SIMULADA"
                        + "\nTotal: " + pago.getMonto().toPlainString()
                        + "\nReferencia: " + pago.getReferencia()
                        + "\nResultado: APROBADA"
                        + "\n\nExistencias y puntos actualizados en Oracle."
                );
            }

            campoPedido.setText("");
            campoRecibido.setText("");
            campoReferencia.setText("");
            campoResultado.setSelectedIndex(0);

            JOptionPane.showMessageDialog(
                    this,
                    "Pago registrado correctamente.",
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