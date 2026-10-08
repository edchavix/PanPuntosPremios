package panpuntospremios;

import java.math.BigDecimal;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public class PruebaPedido {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> probar());
    }

    private static void probar() {
        Cliente cliente = new Cliente(
                "PRUEBA_LOCAL",
                "Cliente de prueba",
                null,
                null
        );
        cliente.setIdCliente(1);

        ProductoIndividual sandwich = new ProductoIndividual(
                1, "SAN001", "Sandwich de pollo",
                new BigDecimal("25.00"), "SANDWICH"
        );

        ProductoIndividual bebida = new ProductoIndividual(
                2, "BEB001", "Refresco",
                new BigDecimal("8.00"), "BEBIDA"
        );

        ProductoIndividual acompanamiento = new ProductoIndividual(
                3, "ACO001", "Papas fritas",
                new BigDecimal("12.00"), "ACOMPANAMIENTO"
        );

        MenuCompleto menu = new MenuCompleto(
                1, "MEN001", "Menu de pollo",
                new BigDecimal("40.00"),
                sandwich, bebida, acompanamiento
        );

        Pedido pedido = new Pedido(cliente);
        pedido.agregarArticulo(sandwich, 2);
        pedido.agregarArticulo(bebida, 1);
        pedido.agregarArticulo(menu, 1);

        verificar(pedido, "98.00", 12);
        String inicial = describir(pedido);

        pedido.cambiarCantidad(0, 1);
        verificar(pedido, "73.00", 10);
        String cambio = describir(pedido);

        pedido.quitarDetalle(1);
        verificar(pedido, "65.00", 10);
        String eliminado = describir(pedido);

        boolean rechazoCero = false;

        try {
            pedido.cambiarCantidad(0, 0);
        } catch (IllegalArgumentException error) {
            rechazoCero = true;
        }

        if (!rechazoCero) {
            throw new IllegalStateException(
                    "La prueba fallo: se permitio cantidad cero."
            );
        }

        verificar(pedido, "65.00", 10);

        JOptionPane.showMessageDialog(
                null,
                "PRUEBAS CORRECTAS"
                + "\n\nPedido inicial: " + inicial
                + "\nAl dejar un sandwich: " + cambio
                + "\nAl quitar la bebida individual: " + eliminado
                + "\nCantidad cero: rechazada"
                + "\nPuntos actuales del cliente: " + cliente.getPuntos()
                + "\n\nPrueba en memoria: no modifica Oracle.",
                "Prueba de calculos del pedido",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    private static void verificar(
            Pedido pedido, String totalEsperado, long puntosEsperados) {

        if (pedido.calcularTotal().compareTo(
                new BigDecimal(totalEsperado)) != 0
                || pedido.calcularPuntos() != puntosEsperados) {

            throw new IllegalStateException(
                    "Los calculos del pedido no coinciden "
                    + "con el resultado esperado."
            );
        }
    }

    private static String describir(Pedido pedido) {
        return pedido.calcularTotal().toPlainString()
                + " | Puntos posibles: " + pedido.calcularPuntos();
    }
}