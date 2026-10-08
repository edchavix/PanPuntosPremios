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
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

public class VentanaPedidos extends JDialog {

    private final ClienteDAO clienteDAO;
    private final CatalogoDAO catalogoDAO;
    private final ValidadorStock validadorStock;
    private final PedidoDAO pedidoDAO;

    private Pedido pedido;
    private Long idPedidoGuardado;

    private final JTextField campoCliente = new JTextField(15);
    private final JLabel etiquetaCliente =
            new JLabel("Primero busca un cliente.");

    private final JComboBox<String> campoTipo =
            new JComboBox<>(new String[]{"PRODUCTO", "MENU"});

    private final JTextField campoCodigo = new JTextField(10);
    private final JTextField campoCantidad = new JTextField("1", 5);

    private final JLabel etiquetaTotal = new JLabel("Total: 0.00");
    private final JLabel etiquetaPuntos =
            new JLabel("Puntos posibles al pagar: 0");
    private final JLabel etiquetaEstado =
            new JLabel("Borrador: aun no se guarda ni descuenta stock.");

    private final JButton botonCliente = new JButton("Buscar cliente");
    private final JButton botonAgregar = new JButton("Agregar");
    private final JButton botonQuitar = new JButton("Quitar seleccionado");
    private final JButton botonGuardar = new JButton("Guardar pedido");

    private final DefaultTableModel modelo = new DefaultTableModel(
            new Object[]{
                "Tipo", "Codigo", "Nombre", "Cantidad",
                "Precio", "Subtotal", "Puntos"
            }, 0
    ) {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false;
        }
    };

    private final JTable tabla = new JTable(modelo);

    public VentanaPedidos(Connection conexion) {
        clienteDAO = new ClienteDAO(conexion);
        catalogoDAO = new CatalogoDAO(conexion);
        validadorStock = new ValidadorStock(conexion);
        pedidoDAO = new PedidoDAO(conexion);

        setTitle("Pan, Puntos y Premios - Preparar pedido");
        setModal(true);
        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent evento) {
                cerrar();
            }
        });

        JPanel contenido = new JPanel(new BorderLayout(10, 15));
        contenido.setBorder(
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
        );

        JPanel cabecera = new JPanel(new GridLayout(3, 1, 5, 5));

        JPanel cliente = new JPanel();
        cliente.add(new JLabel("Identificacion:"));
        cliente.add(campoCliente);
        cliente.add(botonCliente);

        JPanel articulos = new JPanel();
        articulos.add(campoTipo);
        articulos.add(new JLabel("Codigo:"));
        articulos.add(campoCodigo);
        articulos.add(new JLabel("Cantidad:"));
        articulos.add(campoCantidad);
        articulos.add(botonAgregar);

        cabecera.add(cliente);
        cabecera.add(etiquetaCliente);
        cabecera.add(articulos);

        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JPanel pie = new JPanel(new GridLayout(3, 1, 5, 5));
        JPanel totales = new JPanel();
        totales.add(etiquetaTotal);
        totales.add(etiquetaPuntos);

        JPanel botones = new JPanel();
        JButton botonCerrar = new JButton("Cerrar");

        botones.add(botonQuitar);
        botones.add(botonGuardar);
        botones.add(botonCerrar);

        botonGuardar.setEnabled(false);

        pie.add(totales);
        pie.add(etiquetaEstado);
        pie.add(botones);

        botonCliente.addActionListener(evento -> buscarCliente());
        botonAgregar.addActionListener(evento -> agregarArticulo());
        botonQuitar.addActionListener(evento -> quitarArticulo());
        botonGuardar.addActionListener(evento -> guardarPedido());
        botonCerrar.addActionListener(evento -> cerrar());

        contenido.add(cabecera, BorderLayout.NORTH);
        contenido.add(new JScrollPane(tabla), BorderLayout.CENTER);
        contenido.add(pie, BorderLayout.SOUTH);

        setContentPane(contenido);
        setSize(900, 500);
        setLocationRelativeTo(null);
    }

    private void buscarCliente() {
        try {
            Cliente cliente = clienteDAO.buscarPorIdentificacion(
                    campoCliente.getText()
            );

            if (cliente == null) {
                mostrarAviso("No existe un cliente con esa identificacion.");
                return;
            }

            Pedido nuevoPedido = new Pedido(cliente);

            if (pedido != null && !pedido.estaVacio()) {
                int respuesta = JOptionPane.showConfirmDialog(
                        this,
                        "Seleccionar un cliente quitara los articulos "
                        + "del borrador actual. ¿Continuar?",
                        "Nuevo pedido",
                        JOptionPane.YES_NO_OPTION
                );

                if (respuesta != JOptionPane.YES_OPTION) {
                    return;
                }
            }

            pedido = nuevoPedido;
            campoCliente.setText(cliente.getIdentificacion());
            etiquetaCliente.setText(
                    "Cliente seleccionado: " + cliente.getNombre()
                    + " | Identificacion: " + cliente.getIdentificacion()
                    + " | Saldo de puntos: " + cliente.getPuntos()
            );

            actualizarTabla();

        } catch (IllegalArgumentException | SQLException error) {
            mostrarAviso(error.getMessage());
        }
    }

    private boolean clienteCoincide() {
        if (pedido == null) {
            mostrarAviso("Primero busca y selecciona un cliente.");
            return false;
        }

        if (!campoCliente.getText().trim().equals(
                pedido.getCliente().getIdentificacion())) {
            mostrarAviso(
                    "La identificacion cambio. Busca el cliente "
                    + "o restaura la identificacion seleccionada."
            );
            return false;
        }

        return true;
    }

    private void agregarArticulo() {
        if (!clienteCoincide()) {
            return;
        }

        String textoCantidad = campoCantidad.getText().trim();

        if (!textoCantidad.matches("[0-9]{1,9}")) {
            mostrarAviso(
                    "Escribe una cantidad entera positiva "
                    + "de hasta 9 digitos."
            );
            return;
        }

        int cantidad = Integer.parseInt(textoCantidad);

        if (cantidad <= 0) {
            mostrarAviso("La cantidad debe ser mayor que cero.");
            return;
        }

        try {
            ItemVenta articulo = catalogoDAO.buscar(
                    (String) campoTipo.getSelectedItem(),
                    campoCodigo.getText()
            );

            if (articulo == null) {
                mostrarAviso("No existe ese codigo en el tipo seleccionado.");
                return;
            }

            int nuevaFila = pedido.getDetalles().size();
            pedido.agregarArticulo(articulo, cantidad);

            try {
                validadorStock.validar(pedido);
            } catch (SQLException | RuntimeException error) {
                pedido.quitarDetalle(nuevaFila);
                throw error;
            }

            actualizarTabla();
            campoCodigo.setText("");
            campoCantidad.setText("1");
            campoCodigo.requestFocusInWindow();

        } catch (IllegalArgumentException | SQLException error) {
            mostrarAviso(error.getMessage());
        }
    }

    private void quitarArticulo() {
        int fila = tabla.getSelectedRow();

        if (pedido == null || fila < 0) {
            mostrarAviso("Selecciona una fila de la tabla.");
            return;
        }

        pedido.quitarDetalle(tabla.convertRowIndexToModel(fila));
        actualizarTabla();
    }

    private void guardarPedido() {
        if (idPedidoGuardado != null) {
            mostrarAviso("Este pedido ya esta guardado.");
            return;
        }

        if (!clienteCoincide()) {
            return;
        }

        if (pedido.estaVacio()) {
            mostrarAviso("Agrega al menos un articulo.");
            return;
        }

        int respuesta = JOptionPane.showConfirmDialog(
                this,
                "¿Guardar el pedido de "
                + pedido.getCliente().getNombre()
                + " por " + dinero(pedido.calcularTotal()) + "?",
                "Confirmar pedido pendiente",
                JOptionPane.YES_NO_OPTION
        );

        if (respuesta != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            idPedidoGuardado = pedidoDAO.guardar(pedido);
        } catch (IllegalArgumentException | SQLException error) {
            mostrarAviso(error.getMessage());
            return;
        }

        bloquearEdicion();

        etiquetaEstado.setText(
                "Pedido " + idPedidoGuardado
                + " guardado en Oracle. Estado: PENDIENTE."
        );

        JOptionPane.showMessageDialog(
                this,
                "Pedido numero " + idPedidoGuardado
                + " guardado correctamente."
                + "\nEstado: PENDIENTE."
                + "\nAun no se descuentan existencias ni se acreditan puntos.",
                "Pedido guardado",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    private void bloquearEdicion() {
        campoCliente.setEditable(false);
        campoCodigo.setEditable(false);
        campoCantidad.setEditable(false);
        campoTipo.setEnabled(false);
        botonCliente.setEnabled(false);
        botonAgregar.setEnabled(false);
        botonQuitar.setEnabled(false);
        botonGuardar.setEnabled(false);
    }

    private void actualizarTabla() {
        modelo.setRowCount(0);

        for (DetallePedido detalle : pedido.getDetalles()) {
            ItemVenta articulo = detalle.getArticulo();

            modelo.addRow(new Object[]{
                articulo instanceof MenuCompleto ? "MENU" : "PRODUCTO",
                articulo.getCodigo(),
                articulo.getNombre(),
                detalle.getCantidad(),
                dinero(articulo.getPrecio()),
                dinero(detalle.calcularSubtotal()),
                detalle.calcularPuntos()
            });
        }

        etiquetaTotal.setText("Total: " + dinero(pedido.calcularTotal()));
        etiquetaPuntos.setText(
                "Puntos posibles al pagar: " + pedido.calcularPuntos()
        );

        botonGuardar.setEnabled(
                !pedido.estaVacio() && idPedidoGuardado == null
        );
    }

    private void cerrar() {
        if (idPedidoGuardado == null
                && pedido != null && !pedido.estaVacio()) {

            int respuesta = JOptionPane.showConfirmDialog(
                    this,
                    "El borrador no esta guardado. ¿Deseas descartarlo?",
                    "Cerrar pedido",
                    JOptionPane.YES_NO_OPTION
            );

            if (respuesta != JOptionPane.YES_OPTION) {
                return;
            }
        }

        dispose();
    }

    private String dinero(BigDecimal valor) {
        return valor.setScale(2).toPlainString();
    }

    private void mostrarAviso(String mensaje) {
        JOptionPane.showMessageDialog(
                this,
                mensaje,
                "Revision del pedido",
                JOptionPane.WARNING_MESSAGE
        );
    }
}