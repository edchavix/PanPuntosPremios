package panpuntospremios;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.sql.Connection;
import java.sql.SQLException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class VentanaEstadosPedidos extends JDialog {

    private final PedidoDAO pedidoDAO;
    private final JTextField campoPedido = new JTextField(10);
    private final JLabel etiquetaEstado =
            new JLabel("Busca un pedido para ver su estado.");
    private final JButton botonEntregar = new JButton("Marcar entregado");
    private final JButton botonAnular = new JButton("Anular pendiente");

    private Long idPedidoSeleccionado;
    private String estadoSeleccionado;

    public VentanaEstadosPedidos(Connection conexion) {
        pedidoDAO = new PedidoDAO(conexion);

        setTitle("Pan, Puntos y Premios - Estados de pedidos");
        setModal(true);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        JPanel contenido = new JPanel(new BorderLayout(10, 15));
        contenido.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel busqueda = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton botonBuscar = new JButton("Buscar pedido");
        busqueda.add(new JLabel("Numero de pedido:"));
        busqueda.add(campoPedido);
        busqueda.add(botonBuscar);

        JPanel centro = new JPanel(new FlowLayout(FlowLayout.LEFT));
        centro.add(etiquetaEstado);

        JPanel botones = new JPanel();
        JButton botonCerrar = new JButton("Cerrar");
        botones.add(botonEntregar);
        botones.add(botonAnular);
        botones.add(botonCerrar);

        botonEntregar.setEnabled(false);
        botonAnular.setEnabled(false);

        botonBuscar.addActionListener(evento -> buscarPedido());
        campoPedido.addActionListener(evento -> buscarPedido());
        botonEntregar.addActionListener(evento -> cambiarEstado(true));
        botonAnular.addActionListener(evento -> cambiarEstado(false));
        botonCerrar.addActionListener(evento -> dispose());

        contenido.add(busqueda, BorderLayout.NORTH);
        contenido.add(centro, BorderLayout.CENTER);
        contenido.add(botones, BorderLayout.SOUTH);

        setContentPane(contenido);
        pack();
        setLocationRelativeTo(null);
    }

    private void buscarPedido() {
        idPedidoSeleccionado = null;
        estadoSeleccionado = null;
        botonEntregar.setEnabled(false);
        botonAnular.setEnabled(false);

        String texto = campoPedido.getText().trim();
        if (!texto.matches("[0-9]{1,18}")) {
            mostrarAviso("Escribe un numero de pedido positivo.");
            return;
        }

        long idPedido = Long.parseLong(texto);
        if (idPedido <= 0) {
            mostrarAviso("El numero de pedido debe ser mayor que cero.");
            return;
        }

        try {
            String estado = pedidoDAO.consultarEstado(idPedido);
            if (estado == null) {
                etiquetaEstado.setText("No existe el pedido " + idPedido + ".");
                return;
            }

            idPedidoSeleccionado = idPedido;
            estadoSeleccionado = estado;
            etiquetaEstado.setText(
                    "Pedido " + idPedido + " | Estado: " + estado
            );
            botonEntregar.setEnabled("PAGADO".equals(estado));
            botonAnular.setEnabled("PENDIENTE".equals(estado));

        } catch (SQLException error) {
            mostrarError(error);
        }
    }

    private void cambiarEstado(boolean entregar) {
        if (idPedidoSeleccionado == null) {
            mostrarAviso("Primero busca un pedido.");
            return;
        }

        String accion = entregar ? "marcar como entregado" : "anular";
        int confirmacion = JOptionPane.showConfirmDialog(
                this,
                "¿Confirmas " + accion + " el pedido "
                + idPedidoSeleccionado + "?",
                "Confirmar cambio de estado",
                JOptionPane.YES_NO_OPTION
        );

        if (confirmacion != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            if (entregar) {
                pedidoDAO.marcarEntregado(idPedidoSeleccionado);
            } else {
                pedidoDAO.anularPendiente(idPedidoSeleccionado);
            }

            JOptionPane.showMessageDialog(
                    this,
                    entregar
                    ? "El pedido se marco como ENTREGADO."
                    : "El pedido pendiente se anulo sin afectar puntos ni existencias.",
                    "Estado actualizado",
                    JOptionPane.INFORMATION_MESSAGE
            );
            buscarPedido();

        } catch (IllegalArgumentException error) {
            mostrarAviso(error.getMessage());
            buscarPedido();
        } catch (SQLException error) {
            mostrarError(error);
        }
    }

    private void mostrarAviso(String mensaje) {
        JOptionPane.showMessageDialog(
                this, mensaje, "Revisa el pedido", JOptionPane.WARNING_MESSAGE
        );
    }

    private void mostrarError(SQLException error) {
        JOptionPane.showMessageDialog(
                this,
                "No se pudo actualizar el pedido:\n" + error.getMessage(),
                "Error de base de datos",
                JOptionPane.ERROR_MESSAGE
        );
    }
}
