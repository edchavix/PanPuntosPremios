package panpuntospremios;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.sql.Connection;
import java.sql.SQLException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class VentanaInventario extends JDialog {

    private final ProductoDAO productoDAO;
    private final InventarioDAO inventarioDAO;
    private Producto productoSeleccionado;

    private final JTextField campoCodigo = new JTextField(20);
    private final JTextField campoNombre = new JTextField();
    private final JTextField campoStock = new JTextField();
    private final JTextField campoCantidad = new JTextField();

    private final JButton botonReponer = new JButton("Registrar reposicion");

    public VentanaInventario(Connection conexion) {
        productoDAO = new ProductoDAO(conexion);
        inventarioDAO = new InventarioDAO(conexion);

        setTitle("Pan, Puntos y Premios - Inventario");
        setModal(true);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        JPanel contenido = new JPanel(new BorderLayout(10, 15));
        contenido.setBorder(
                BorderFactory.createEmptyBorder(20, 20, 20, 20)
        );

        JPanel busqueda = new JPanel();
        JButton botonBuscar = new JButton("Buscar");

        busqueda.add(new JLabel("Codigo:"));
        busqueda.add(campoCodigo);
        busqueda.add(botonBuscar);

        JPanel datos = new JPanel(new GridLayout(3, 2, 10, 10));

        datos.add(new JLabel("Producto:"));
        datos.add(campoNombre);
        datos.add(new JLabel("Existencias actuales:"));
        datos.add(campoStock);
        datos.add(new JLabel("Cantidad a ingresar:"));
        datos.add(campoCantidad);

        campoNombre.setEditable(false);
        campoStock.setEditable(false);
        campoCantidad.setEditable(false);
        botonReponer.setEnabled(false);

        JPanel botones = new JPanel();
        JButton botonCerrar = new JButton("Cerrar");

        botones.add(botonReponer);
        botones.add(botonCerrar);

        botonBuscar.addActionListener(evento -> buscarProducto());
        campoCodigo.addActionListener(evento -> buscarProducto());
        botonReponer.addActionListener(evento -> reponer());
        botonCerrar.addActionListener(evento -> dispose());

        contenido.add(busqueda, BorderLayout.NORTH);
        contenido.add(datos, BorderLayout.CENTER);
        contenido.add(botones, BorderLayout.SOUTH);

        setContentPane(contenido);
        pack();
        setLocationRelativeTo(null);
    }

    private void buscarProducto() {
        productoSeleccionado = null;
        campoNombre.setText("");
        campoStock.setText("");
        campoCantidad.setText("");
        campoCantidad.setEditable(false);
        botonReponer.setEnabled(false);

        try {
            Producto producto = productoDAO.buscarPorCodigo(
                    campoCodigo.getText()
            );

            if (producto == null) {
                mostrarValidacion(
                        "No existe un producto con ese codigo."
                );
                return;
            }

            campoCodigo.setText(producto.getCodigo());
            campoNombre.setText(producto.getNombre());
            campoStock.setText(String.valueOf(producto.getStock()));

            if (!producto.isActivo()) {
                mostrarValidacion(
                        "Activa el producto antes de registrar "
                        + "una reposicion."
                );
                return;
            }

            productoSeleccionado = producto;
            campoCantidad.setEditable(true);
            botonReponer.setEnabled(true);
            campoCantidad.requestFocusInWindow();

        } catch (IllegalArgumentException error) {
            mostrarValidacion(error.getMessage());
        } catch (SQLException error) {
            mostrarError(error);
        }
    }

    private void reponer() {
        if (productoSeleccionado == null) {
            mostrarValidacion("Primero busca un producto activo.");
            return;
        }

        if (!campoCodigo.getText().trim().equals(
                productoSeleccionado.getCodigo())) {
            mostrarValidacion(
                    "El codigo cambio. Pulsa Buscar nuevamente."
            );
            return;
        }

        String textoCantidad = campoCantidad.getText().trim();

        if (!textoCantidad.matches("[0-9]{1,10}")) {
            mostrarValidacion(
                    "Escribe una cantidad entera positiva "
                    + "de hasta 10 digitos."
            );
            return;
        }

        long cantidad = Long.parseLong(textoCantidad);

        if (cantidad == 0) {
            mostrarValidacion("La cantidad debe ser mayor que cero.");
            return;
        }

        int respuesta = JOptionPane.showConfirmDialog(
                this,
                "¿Deseas agregar " + cantidad + " unidades de "
                + productoSeleccionado.getNombre() + "?",
                "Confirmar reposicion",
                JOptionPane.YES_NO_OPTION
        );

        if (respuesta != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            inventarioDAO.reponer(
                    productoSeleccionado.getCodigo(),
                    cantidad
            );
        } catch (IllegalArgumentException error) {
            mostrarValidacion(error.getMessage());
            return;
        } catch (SQLException error) {
            mostrarError(error);
            return;
        }

        campoCantidad.setText("");

        JOptionPane.showMessageDialog(
                this,
                "Reposicion registrada correctamente."
        );

        buscarProducto();
    }

    private void mostrarValidacion(String mensaje) {
        JOptionPane.showMessageDialog(
                this,
                mensaje,
                "Revisa los datos",
                JOptionPane.WARNING_MESSAGE
        );
    }

    private void mostrarError(SQLException error) {
        JOptionPane.showMessageDialog(
                this,
                "No se pudo completar la operacion:\n"
                + error.getMessage(),
                "Error de base de datos",
                JOptionPane.ERROR_MESSAGE
        );
    }
}