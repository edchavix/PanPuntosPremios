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

public class VentanaClientes extends JDialog {

    private final ClienteDAO clienteDAO;
    private Cliente clienteSeleccionado;

    private final JTextField campoIdentificacion = new JTextField(20);
    private final JTextField campoNombre = new JTextField();
    private final JTextField campoTelefono = new JTextField();
    private final JTextField campoCorreo = new JTextField();
    private final JTextField campoPuntos = new JTextField();
    private final JTextField campoActivo = new JTextField();

    private final JButton botonRegistrar = new JButton("Registrar");
    private final JButton botonModificar = new JButton("Modificar");

    public VentanaClientes(Connection conexion) {
        clienteDAO = new ClienteDAO(conexion);

        setTitle("Pan, Puntos y Premios - Clientes");
        setModal(true);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        JPanel contenido = new JPanel(new BorderLayout(10, 15));
        contenido.setBorder(
                BorderFactory.createEmptyBorder(20, 20, 20, 20)
        );

        JPanel busqueda = new JPanel();
        JButton botonBuscar = new JButton("Buscar");

        busqueda.add(new JLabel("Identificacion:"));
        busqueda.add(campoIdentificacion);
        busqueda.add(botonBuscar);

        JPanel datos = new JPanel(new GridLayout(5, 2, 10, 10));

        datos.add(new JLabel("Nombre:"));
        datos.add(campoNombre);
        datos.add(new JLabel("Telefono:"));
        datos.add(campoTelefono);
        datos.add(new JLabel("Correo:"));
        datos.add(campoCorreo);
        datos.add(new JLabel("Puntos:"));
        datos.add(campoPuntos);
        datos.add(new JLabel("Activo:"));
        datos.add(campoActivo);

        campoPuntos.setEditable(false);
        campoActivo.setEditable(false);
        habilitarDatos(false);
        botonRegistrar.setEnabled(false);
        botonModificar.setEnabled(false);

        JPanel botones = new JPanel();
        JButton botonNuevo = new JButton("Nuevo");
        JButton botonCerrar = new JButton("Cerrar");

        botones.add(botonNuevo);
        botones.add(botonRegistrar);
        botones.add(botonModificar);
        botones.add(botonCerrar);

        botonBuscar.addActionListener(evento -> buscarCliente());
        campoIdentificacion.addActionListener(evento -> buscarCliente());
        botonNuevo.addActionListener(evento -> nuevoCliente());
        botonRegistrar.addActionListener(evento -> registrarCliente());
        botonModificar.addActionListener(evento -> modificarCliente());
        botonCerrar.addActionListener(evento -> dispose());

        contenido.add(busqueda, BorderLayout.NORTH);
        contenido.add(datos, BorderLayout.CENTER);
        contenido.add(botones, BorderLayout.SOUTH);

        setContentPane(contenido);
        pack();
        setLocationRelativeTo(null);
    }

    private void buscarCliente() {
        clienteSeleccionado = null;
        limpiarResultado();
        habilitarDatos(false);
        botonRegistrar.setEnabled(false);
        botonModificar.setEnabled(false);

        try {
            Cliente cliente = clienteDAO.buscarPorIdentificacion(
                    campoIdentificacion.getText()
            );

            if (cliente == null) {
                JOptionPane.showMessageDialog(
                        this,
                        "No existe un cliente con esa identificacion."
                );
                return;
            }

            clienteSeleccionado = cliente;
            mostrarCliente(cliente);
            habilitarDatos(true);
            botonModificar.setEnabled(true);

        } catch (IllegalArgumentException error) {
            mostrarValidacion(error.getMessage());
        } catch (SQLException error) {
            mostrarError(error);
        }
    }

    private void nuevoCliente() {
        clienteSeleccionado = null;
        campoIdentificacion.setText("");
        limpiarResultado();
        habilitarDatos(true);
        botonRegistrar.setEnabled(true);
        botonModificar.setEnabled(false);
        campoPuntos.setText("0");
        campoActivo.setText("Si");
        campoIdentificacion.requestFocusInWindow();
    }

    private void registrarCliente() {
        Cliente cliente = new Cliente(
                campoIdentificacion.getText().trim(),
                campoNombre.getText().trim(),
                campoTelefono.getText().trim(),
                campoCorreo.getText().trim()
        );

        try {
            clienteDAO.registrar(cliente);
        } catch (IllegalArgumentException error) {
            mostrarValidacion(error.getMessage());
            return;
        } catch (SQLException error) {
            if (error.getErrorCode() == 1) {
                mostrarValidacion(
                        "Ya existe un cliente con esa identificacion."
                );
            } else {
                mostrarError(error);
            }
            return;
        }

        clienteSeleccionado = null;
        mostrarCliente(cliente);
        habilitarDatos(false);
        botonRegistrar.setEnabled(false);
        botonModificar.setEnabled(false);

        JOptionPane.showMessageDialog(
                this,
                "Cliente registrado correctamente. "
                + "Para modificarlo, pulsa Buscar."
        );
    }

    private void modificarCliente() {
        if (clienteSeleccionado == null) {
            mostrarValidacion("Primero busca un cliente.");
            return;
        }

        String identificacion = campoIdentificacion.getText().trim();

        if (!identificacion.equals(
                clienteSeleccionado.getIdentificacion())) {
            mostrarValidacion(
                    "La identificacion cambio. Pulsa Buscar "
                    + "para seleccionar el cliente correspondiente."
            );
            return;
        }

        Cliente actualizado = new Cliente(
                clienteSeleccionado.getIdentificacion(),
                campoNombre.getText().trim(),
                campoTelefono.getText().trim(),
                campoCorreo.getText().trim()
        );

        actualizado.setIdCliente(clienteSeleccionado.getIdCliente());
        actualizado.setPuntos(clienteSeleccionado.getPuntos());
        actualizado.setActivo(clienteSeleccionado.isActivo());

        try {
            clienteDAO.actualizar(actualizado);

            clienteSeleccionado = actualizado;
            mostrarCliente(actualizado);

            JOptionPane.showMessageDialog(
                    this,
                    "Datos del cliente actualizados correctamente."
            );

        } catch (IllegalArgumentException error) {
            mostrarValidacion(error.getMessage());
        } catch (SQLException error) {
            mostrarError(error);
        }
    }

    private void mostrarCliente(Cliente cliente) {
        campoIdentificacion.setText(cliente.getIdentificacion());
        campoNombre.setText(cliente.getNombre());
        campoTelefono.setText(cliente.getTelefono());
        campoCorreo.setText(cliente.getCorreo());
        campoPuntos.setText(String.valueOf(cliente.getPuntos()));
        campoActivo.setText(cliente.isActivo() ? "Si" : "No");
    }

    private void habilitarDatos(boolean habilitado) {
        campoNombre.setEditable(habilitado);
        campoTelefono.setEditable(habilitado);
        campoCorreo.setEditable(habilitado);
    }

    private void limpiarResultado() {
        campoNombre.setText("");
        campoTelefono.setText("");
        campoCorreo.setText("");
        campoPuntos.setText("");
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