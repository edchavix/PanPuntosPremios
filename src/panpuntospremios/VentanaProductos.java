package panpuntospremios;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class VentanaProductos extends JDialog {

    private final ProductoDAO productoDAO;
    private Producto productoSeleccionado;

    private final JTextField campoCodigo = new JTextField(20);
    private final JTextField campoNombre = new JTextField();
    private final JComboBox<String> campoCategoria = new JComboBox<>(
            new String[]{"SANDWICH", "BEBIDA", "ACOMPANAMIENTO"}
    );
    private final JTextField campoPrecio = new JTextField();
    private final JTextField campoStock = new JTextField();
    private final JTextField campoActivo = new JTextField();

    private final JButton botonRegistrar = new JButton("Registrar");
    private final JButton botonModificar = new JButton("Modificar");
    private final JButton botonEstado = new JButton("Activar/Desactivar");

    public VentanaProductos(Connection conexion) {
        productoDAO = new ProductoDAO(conexion);

        setTitle("Pan, Puntos y Premios - Productos");
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

        JPanel datos = new JPanel(new GridLayout(5, 2, 10, 10));

        datos.add(new JLabel("Nombre:"));
        datos.add(campoNombre);
        datos.add(new JLabel("Categoria:"));
        datos.add(campoCategoria);
        datos.add(new JLabel("Precio:"));
        datos.add(campoPrecio);
        datos.add(new JLabel("Existencias:"));
        datos.add(campoStock);
        datos.add(new JLabel("Activo:"));
        datos.add(campoActivo);

        campoStock.setEditable(false);
        campoActivo.setEditable(false);
        prepararBusqueda();

        JPanel botones = new JPanel();
        JButton botonNuevo = new JButton("Nuevo");
        JButton botonCerrar = new JButton("Cerrar");

        botones.add(botonNuevo);
        botones.add(botonRegistrar);
        botones.add(botonModificar);
        botones.add(botonEstado);
        botones.add(botonCerrar);

        botonBuscar.addActionListener(evento -> buscarProducto());
        campoCodigo.addActionListener(evento -> buscarProducto());
        botonNuevo.addActionListener(evento -> nuevoProducto());
        botonRegistrar.addActionListener(evento -> registrarProducto());
        botonModificar.addActionListener(evento -> modificarProducto());
        botonEstado.addActionListener(evento -> cambiarEstado());
        botonCerrar.addActionListener(evento -> dispose());

        contenido.add(busqueda, BorderLayout.NORTH);
        contenido.add(datos, BorderLayout.CENTER);
        contenido.add(botones, BorderLayout.SOUTH);

        setContentPane(contenido);
        pack();
        setLocationRelativeTo(null);
    }

    private void prepararBusqueda() {
        productoSeleccionado = null;
        limpiarResultado();
        campoNombre.setEditable(false);
        campoPrecio.setEditable(false);
        campoCategoria.setEnabled(false);
        botonRegistrar.setEnabled(false);
        botonModificar.setEnabled(false);
        botonEstado.setEnabled(false);
        botonEstado.setText("Activar/Desactivar");
    }

    private void buscarProducto() {
        prepararBusqueda();

        try {
            Producto producto = productoDAO.buscarPorCodigo(
                    campoCodigo.getText()
            );

            if (producto == null) {
                JOptionPane.showMessageDialog(
                        this,
                        "No existe un producto con ese codigo."
                );
                return;
            }

            productoSeleccionado = producto;
            mostrarProducto(producto);
            campoNombre.setEditable(true);
            campoPrecio.setEditable(true);
            botonModificar.setEnabled(true);
            botonEstado.setEnabled(true);

        } catch (IllegalArgumentException error) {
            mostrarValidacion(error.getMessage());
        } catch (SQLException error) {
            mostrarError(error);
        }
    }

    private void nuevoProducto() {
        prepararBusqueda();
        campoCodigo.setText("");
        campoNombre.setEditable(true);
        campoPrecio.setEditable(true);
        campoCategoria.setEnabled(true);
        botonRegistrar.setEnabled(true);
        campoStock.setText("0");
        campoActivo.setText("Si");
        campoCodigo.requestFocusInWindow();
    }

    private BigDecimal leerPrecio() {
        String texto = campoPrecio.getText().trim();

        if (texto.isEmpty()) {
            throw new IllegalArgumentException(
                    "Escribe el precio del producto."
            );
        }

        if (!texto.matches("-?[0-9]+(\\.[0-9]{1,2})?")) {
            throw new IllegalArgumentException(
                    "Escribe un precio numerico con punto decimal "
                    + "y hasta dos decimales. Ejemplo: 8.50"
            );
        }

        return new BigDecimal(texto);
    }

    private void registrarProducto() {
        Producto producto;

        try {
            producto = new Producto(
                    campoCodigo.getText().trim(),
                    (String) campoCategoria.getSelectedItem(),
                    campoNombre.getText().trim(),
                    leerPrecio()
            );

            productoDAO.registrar(producto);

        } catch (IllegalArgumentException error) {
            mostrarValidacion(error.getMessage());
            return;
        } catch (SQLException error) {
            if (error.getErrorCode() == 1) {
                mostrarValidacion(
                        "Ya existe un producto con ese codigo."
                );
            } else {
                mostrarError(error);
            }
            return;
        }

        prepararBusqueda();
        mostrarProducto(producto);

        JOptionPane.showMessageDialog(
                this,
                "Producto registrado correctamente. "
                + "Pulsa Buscar para modificarlo."
        );
    }

    private boolean seleccionValida() {
        if (productoSeleccionado == null) {
            mostrarValidacion("Primero busca un producto.");
            return false;
        }

        if (!campoCodigo.getText().trim().equals(
                productoSeleccionado.getCodigo())) {
            mostrarValidacion(
                    "El codigo cambio. Pulsa Buscar para seleccionar "
                    + "el producto correspondiente."
            );
            return false;
        }

        return true;
    }

    private void modificarProducto() {
        if (!seleccionValida()) {
            return;
        }

        try {
            Producto actualizado = new Producto(
                    productoSeleccionado.getCodigo(),
                    productoSeleccionado.getCategoria(),
                    campoNombre.getText().trim(),
                    leerPrecio()
            );

            actualizado.setIdProducto(
                    productoSeleccionado.getIdProducto()
            );
            actualizado.setStock(productoSeleccionado.getStock());
            actualizado.setActivo(productoSeleccionado.isActivo());

            productoDAO.actualizar(actualizado);
            productoSeleccionado = actualizado;
            mostrarProducto(actualizado);

            JOptionPane.showMessageDialog(
                    this,
                    "Producto actualizado correctamente."
            );

        } catch (IllegalArgumentException error) {
            mostrarValidacion(error.getMessage());
        } catch (SQLException error) {
            mostrarError(error);
        }
    }

    private void cambiarEstado() {
        if (!seleccionValida()) {
            return;
        }

        boolean nuevoEstado = !productoSeleccionado.isActivo();
        String accion = nuevoEstado ? "activar" : "desactivar";

        int respuesta = JOptionPane.showConfirmDialog(
                this,
                "¿Deseas " + accion + " el producto "
                + productoSeleccionado.getNombre() + "?\n"
                + "Esta accion solo cambia su estado; no guarda "
                + "las ediciones de nombre o precio.",
                "Confirmar cambio de estado",
                JOptionPane.YES_NO_OPTION
        );

        if (respuesta != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            productoDAO.cambiarEstado(
                    productoSeleccionado.getIdProducto(),
                    nuevoEstado
            );

            productoSeleccionado.setActivo(nuevoEstado);
            campoActivo.setText(nuevoEstado ? "Si" : "No");
            botonEstado.setText(
                    nuevoEstado ? "Desactivar" : "Activar"
            );

            JOptionPane.showMessageDialog(
                    this,
                    "Estado del producto actualizado correctamente."
            );

        } catch (SQLException error) {
            mostrarError(error);
        }
    }

    private void mostrarProducto(Producto producto) {
        campoCodigo.setText(producto.getCodigo());
        campoNombre.setText(producto.getNombre());
        campoCategoria.setSelectedItem(producto.getCategoria());
        campoPrecio.setText(
                producto.getPrecio()
                        .setScale(2, RoundingMode.UNNECESSARY)
                        .toPlainString()
        );
        campoStock.setText(String.valueOf(producto.getStock()));
        campoActivo.setText(producto.isActivo() ? "Si" : "No");
        botonEstado.setText(
                producto.isActivo() ? "Desactivar" : "Activar"
        );
    }

    private void limpiarResultado() {
        campoNombre.setText("");
        campoCategoria.setSelectedIndex(-1);
        campoPrecio.setText("");
        campoStock.setText("");
        campoActivo.setText("");
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