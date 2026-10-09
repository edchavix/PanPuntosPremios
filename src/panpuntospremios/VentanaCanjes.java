package panpuntospremios;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class VentanaCanjes extends JDialog {

    private final Connection conexion;
    private final ClienteDAO clienteDAO;
    private final CanjeDAO canjeDAO;
    private final Map<String, Long> recompensas = new LinkedHashMap<>();

    private Cliente clienteSeleccionado;

    private final JTextField campoIdentificacion = new JTextField(16);
    private final JLabel etiquetaCliente =
            new JLabel("Primero busca un cliente.");

    private final JComboBox<String> campoRecompensa =
            new JComboBox<>();

    private final JTextField campoCodigo = new JTextField(16);

    private final JButton botonCanjear = new JButton("Canjear");

    public VentanaCanjes(Connection conexion) {
        this.conexion = conexion;
        clienteDAO = new ClienteDAO(conexion);
        canjeDAO = new CanjeDAO(conexion);

        setTitle("Pan, Puntos y Premios - Canjes");
        setModal(true);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        JPanel contenido = new JPanel(new BorderLayout(10, 15));
        contenido.setBorder(
                BorderFactory.createEmptyBorder(20, 20, 20, 20)
        );

        JPanel busqueda = new JPanel();
        JButton botonBuscar = new JButton("Buscar cliente");

        busqueda.add(new JLabel("Identificacion:"));
        busqueda.add(campoIdentificacion);
        busqueda.add(botonBuscar);

        JPanel datos = new JPanel(new GridLayout(3, 2, 10, 10));
        datos.add(new JLabel("Cliente y saldo:"));
        datos.add(etiquetaCliente);
        datos.add(new JLabel("Recompensa:"));
        datos.add(campoRecompensa);
        datos.add(new JLabel("Codigo del articulo:"));
        datos.add(campoCodigo);

        JPanel botones = new JPanel();
        JButton botonCerrar = new JButton("Cerrar");

        botones.add(botonCanjear);
        botones.add(botonCerrar);

        botonCanjear.setEnabled(false);

        botonBuscar.addActionListener(evento -> buscarCliente());
        campoIdentificacion.addActionListener(evento -> buscarCliente());
        botonCanjear.addActionListener(evento -> canjear());
        botonCerrar.addActionListener(evento -> dispose());

        contenido.add(busqueda, BorderLayout.NORTH);
        contenido.add(datos, BorderLayout.CENTER);
        contenido.add(botones, BorderLayout.SOUTH);

        setContentPane(contenido);
        pack();
        setLocationRelativeTo(null);

        cargarRecompensas();
    }

    private void cargarRecompensas() {
        String sql = "SELECT id_recompensa, nombre, puntos_requeridos "
                + "FROM recompensas WHERE activo = 'S' "
                + "ORDER BY puntos_requeridos";

        try (PreparedStatement sentencia =
                conexion.prepareStatement(sql);
             ResultSet resultado = sentencia.executeQuery()) {

            while (resultado.next()) {
                long id = resultado.getLong("id_recompensa");
                String nombre = resultado.getString("nombre");
                long puntos = resultado.getLong("puntos_requeridos");
                String etiqueta = nombre + " (" + puntos + " puntos)";

                recompensas.put(etiqueta, id);
                campoRecompensa.addItem(etiqueta);
            }

            if (recompensas.isEmpty()) {
                botonCanjear.setEnabled(false);
                mostrarAviso("No hay recompensas activas.");
            }

        } catch (SQLException error) {
            botonCanjear.setEnabled(false);
            mostrarError(error);
        }
    }

    private void buscarCliente() {
        clienteSeleccionado = null;
        botonCanjear.setEnabled(false);
        etiquetaCliente.setText("Primero busca un cliente.");

        try {
            Cliente cliente = clienteDAO.buscarPorIdentificacion(
                    campoIdentificacion.getText()
            );

            if (cliente == null) {
                mostrarAviso(
                        "No existe un cliente con esa identificacion."
                );
                return;
            }

            if (!cliente.isActivo()) {
                mostrarAviso("El cliente esta desactivado.");
                return;
            }

            clienteSeleccionado = cliente;
            etiquetaCliente.setText(
                    cliente.getNombre() + " | Puntos: "
                    + cliente.getPuntos()
            );
            botonCanjear.setEnabled(true);

        } catch (IllegalArgumentException error) {
            mostrarAviso(error.getMessage());
        } catch (SQLException error) {
            mostrarError(error);
        }
    }

    private void canjear() {
        if (clienteSeleccionado == null) {
            mostrarAviso("Primero busca un cliente activo.");
            return;
        }

        if (!campoIdentificacion.getText().trim().equals(
                clienteSeleccionado.getIdentificacion())) {
            mostrarAviso("Vuelve a buscar al cliente.");
            return;
        }

        String recompensa = (String) campoRecompensa.getSelectedItem();
        Long idRecompensa = recompensas.get(recompensa);

        if (idRecompensa == null) {
            mostrarAviso("Selecciona una recompensa.");
            return;
        }

        String codigo = campoCodigo.getText().trim();

        if (codigo.isEmpty()) {
            mostrarAviso("Escribe el codigo del articulo.");
            return;
        }

        int confirmacion = JOptionPane.showConfirmDialog(
                this,
                "¿Confirmas canjear " + recompensa
                + " para " + clienteSeleccionado.getNombre()
                + " usando el articulo " + codigo + "?",
                "Confirmar canje",
                JOptionPane.YES_NO_OPTION
        );

        if (confirmacion != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            long idCanje = canjeDAO.canjear(
                    clienteSeleccionado.getIdCliente(),
                    idRecompensa,
                    codigo
            );

            Cliente actualizado =
                    clienteDAO.buscarPorIdentificacion(
                            clienteSeleccionado.getIdentificacion()
                    );

            clienteSeleccionado = actualizado;
            etiquetaCliente.setText(
                    actualizado.getNombre() + " | Puntos: "
                    + actualizado.getPuntos()
            );
            campoCodigo.setText("");

            JOptionPane.showMessageDialog(
                    this,
                    "Canje registrado correctamente."
                    + "\nNumero de canje: " + idCanje
                    + "\nPuntos restantes: " + actualizado.getPuntos(),
                    "Canje exitoso",
                    JOptionPane.INFORMATION_MESSAGE
            );

        } catch (IllegalArgumentException error) {
            mostrarAviso(error.getMessage());
        } catch (SQLException error) {
            mostrarError(error);
        }
    }

    private void mostrarAviso(String mensaje) {
        JOptionPane.showMessageDialog(
                this,
                mensaje,
                "Revisa el canje",
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