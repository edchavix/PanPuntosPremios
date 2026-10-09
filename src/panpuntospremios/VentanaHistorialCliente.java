package panpuntospremios;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.sql.Connection;
import java.sql.SQLException;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

public class VentanaHistorialCliente extends JDialog {

    private final ReporteDAO reporteDAO;
    private final long idCliente;
    private final JComboBox<String> campoConsulta = new JComboBox<>(
            new String[]{"Pedidos del cliente", "Historial de puntos"}
    );
    private final JTable tabla = new JTable();

    public VentanaHistorialCliente(
            Connection conexion, long idCliente, String nombreCliente) {

        reporteDAO = new ReporteDAO(conexion);
        this.idCliente = idCliente;

        setTitle("Historial de " + nombreCliente);
        setModal(true);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        JPanel contenido = new JPanel(new BorderLayout(10, 10));
        contenido.setBorder(javax.swing.BorderFactory.createEmptyBorder(
                15, 15, 15, 15
        ));

        JPanel controles = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton botonConsultar = new JButton("Consultar");
        JButton botonCerrar = new JButton("Cerrar");
        controles.add(new JLabel("Historial:"));
        controles.add(campoConsulta);
        controles.add(botonConsultar);
        controles.add(botonCerrar);

        tabla.setAutoCreateRowSorter(true);
        tabla.setFillsViewportHeight(true);

        contenido.add(controles, BorderLayout.NORTH);
        contenido.add(new JScrollPane(tabla), BorderLayout.CENTER);

        botonConsultar.addActionListener(evento -> consultar());
        botonCerrar.addActionListener(evento -> dispose());

        setContentPane(contenido);
        setSize(850, 400);
        setLocationRelativeTo(null);
    }

    private void consultar() {
        try {
            if ("Pedidos del cliente".equals(
                    campoConsulta.getSelectedItem())) {
                tabla.setModel(reporteDAO.consultarPedidosCliente(idCliente));
            } else {
                tabla.setModel(reporteDAO.consultarHistorialPuntos(idCliente));
            }

            if (tabla.getRowCount() == 0) {
                JOptionPane.showMessageDialog(
                        this,
                        "Este cliente aun no tiene movimientos de este tipo.",
                        "Historial del cliente",
                        JOptionPane.INFORMATION_MESSAGE
                );
            }
        } catch (SQLException error) {
            JOptionPane.showMessageDialog(
                    this,
                    "No se pudo consultar el historial:\n" + error.getMessage(),
                    "Error de base de datos",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }
}
