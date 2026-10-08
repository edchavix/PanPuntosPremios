package panpuntospremios;

import java.math.BigDecimal;
import java.util.ArrayList;

public class Pedido {

    private final Cliente cliente;
    private final ArrayList<DetallePedido> detalles;

    public Pedido(Cliente cliente) {
        if (cliente == null || cliente.getIdCliente() <= 0) {
            throw new IllegalArgumentException(
                    "Selecciona un cliente registrado."
            );
        }

        if (!cliente.isActivo()) {
            throw new IllegalArgumentException(
                    "El cliente debe estar activo."
            );
        }

        this.cliente = cliente;
        this.detalles = new ArrayList<>();
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void agregarArticulo(ItemVenta articulo, int cantidad) {
        detalles.add(new DetallePedido(articulo, cantidad));
    }

    public void quitarDetalle(int indice) {
        validarIndice(indice);
        detalles.remove(indice);
    }

    public void cambiarCantidad(int indice, int cantidad) {
        validarIndice(indice);
        detalles.get(indice).setCantidad(cantidad);
    }

    public ArrayList<DetallePedido> getDetalles() {
        ArrayList<DetallePedido> copia = new ArrayList<>();

        for (DetallePedido detalle : detalles) {
            copia.add(new DetallePedido(
                    detalle.getArticulo(),
                    detalle.getCantidad()
            ));
        }

        return copia;
    }

    public boolean estaVacio() {
        return detalles.isEmpty();
    }

    public BigDecimal calcularTotal() {
        BigDecimal total = BigDecimal.ZERO;

        for (DetallePedido detalle : detalles) {
            total = total.add(detalle.calcularSubtotal());
        }

        return total;
    }

    public long calcularPuntos() {
        long puntos = 0;

        for (DetallePedido detalle : detalles) {
            puntos = Math.addExact(puntos, detalle.calcularPuntos());
        }

        return puntos;
    }

    private void validarIndice(int indice) {
        if (indice < 0 || indice >= detalles.size()) {
            throw new IllegalArgumentException(
                    "Selecciona una linea valida del pedido."
            );
        }
    }
}