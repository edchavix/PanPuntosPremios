package panpuntospremios;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.SpinnerDateModel;
import javax.swing.SpinnerModel;

public class VentanaReportes extends JDialog {

    private final ReporteDAO reporteDAO;
    private final JComboBox<String> campoReporte = new JComboBox<>(new String[]{
        "Ventas por periodo",
        "Productos mas vendidos",
        "Productos agotados o con bajo inventario",
        "Puntos acumulados por cliente",
        "Historial de canjes",
        "Ventas de menus completos",
        "Pedidos",
        "Pagos",
        "Existencias",
        "Movimientos de inventario"
    });
    private final JSpinner campoDesde = crearSelectorFecha(
            LocalDate.now().withDayOfMonth(1)
    );
    private final JSpinner campoHasta = crearSelectorFecha(LocalDate.now());
    private final JTable tabla = new JTable();

    public VentanaReportes(Connection conexion) {
        reporteDAO = new ReporteDAO(conexion);

        setTitle("Pan, Puntos y Premios - Reportes");
        setModal(true);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        JPanel contenido = new JPanel(new BorderLayout(10, 10));
        contenido.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel controles = new JPanel(new GridLayout(2, 1, 5, 5));
        JPanel filaReporte = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JPanel filaFechas = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton botonConsultar = new JButton("Consultar");
        JButton botonCerrar = new JButton("Cerrar");

        filaReporte.add(new JLabel("Reporte:"));
        filaReporte.add(campoReporte);
        filaReporte.add(botonConsultar);
        filaReporte.add(botonCerrar);

        filaFechas.add(new JLabel("Desde:"));
        filaFechas.add(campoDesde);
        filaFechas.add(new JLabel("Hasta:"));
        filaFechas.add(campoHasta);

        controles.add(filaReporte);
        controles.add(filaFechas);

        campoDesde.setEditor(new JSpinner.DateEditor(campoDesde, "dd/MM/yyyy"));
        campoHasta.setEditor(new JSpinner.DateEditor(campoHasta, "dd/MM/yyyy"));
        tabla.setAutoCreateRowSorter(true);
        tabla.setFillsViewportHeight(true);

        contenido.add(controles, BorderLayout.NORTH);
        contenido.add(new JScrollPane(tabla), BorderLayout.CENTER);

        botonConsultar.addActionListener(evento -> consultar());
        botonCerrar.addActionListener(evento -> dispose());
        campoReporte.addActionListener(evento -> actualizarCamposFecha());

        setContentPane(contenido);
        setSize(1050, 540);
        setLocationRelativeTo(null);
        actualizarCamposFecha();
    }

    private static JSpinner crearSelectorFecha(LocalDate fecha) {
        Date valor = Date.from(fecha.atStartOfDay(ZoneId.systemDefault()).toInstant());
        SpinnerModel modelo = new SpinnerDateModel(valor, null, null,
                java.util.Calendar.DAY_OF_MONTH);
        return new JSpinner(modelo);
    }

    private void actualizarCamposFecha() {
        boolean usarFechas = "Ventas por periodo".equals(
                campoReporte.getSelectedItem()
        );
        campoDesde.setEnabled(usarFechas);
        campoHasta.setEnabled(usarFechas);
    }

    private void consultar() {
        String reporte = (String) campoReporte.getSelectedItem();

        try {
            if ("Ventas por periodo".equals(reporte)) {
                LocalDate desde = obtenerFecha(campoDesde);
                LocalDate hasta = obtenerFecha(campoHasta);
                if (hasta.isBefore(desde)) {
                    throw new IllegalArgumentException(
                            "La fecha Hasta no puede ser anterior a Desde."
                    );
                }

                Timestamp inicio = Timestamp.valueOf(desde.atStartOfDay());
                Timestamp finExclusivo = Timestamp.valueOf(
                        hasta.plusDays(1).atStartOfDay()
                );
                tabla.setModel(reporteDAO.consultarVentasPorPeriodo(
                        inicio, finExclusivo
                ));
            } else {
                tabla.setModel(reporteDAO.consultar(reporte));
            }

            if (tabla.getRowCount() == 0) {
                JOptionPane.showMessageDialog(
                        this,
                        "No hay datos para mostrar en este reporte.",
                        "Reporte",
                        JOptionPane.INFORMATION_MESSAGE
                );
            }
        } catch (IllegalArgumentException error) {
            JOptionPane.showMessageDialog(
                    this,
                    error.getMessage(),
                    "Revisa el reporte",
                    JOptionPane.WARNING_MESSAGE
            );
        } catch (SQLException error) {
            JOptionPane.showMessageDialog(
                    this,
                    "No se pudo consultar el reporte:\n" + error.getMessage(),
                    "Error de base de datos",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private LocalDate obtenerFecha(JSpinner campo) {
        Date fecha = (Date) campo.getValue();
        return fecha.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }
}
