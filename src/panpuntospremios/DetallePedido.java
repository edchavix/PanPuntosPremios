package panpuntospremios;

import java.math.BigDecimal;

public class DetallePedido {

    private final ItemVenta articulo;
    private int cantidad;

    public DetallePedido(ItemVenta articulo, int cantidad) {
        if (articulo == null) {
            throw new IllegalArgumentException(
                    "Debes seleccionar un producto o menu."
            );
        }

        this.articulo = articulo;
        setCantidad(cantidad);
    }

    public ItemVenta getArticulo() {
        return articulo;
    }

    public int getCantidad() {
        return cantidad;
    }

    public void setCantidad(int cantidad) {
        if (cantidad <= 0) {
            throw new IllegalArgumentException(
                    "La cantidad debe ser mayor que cero."
            );
        }

        this.cantidad = cantidad;
    }

    public BigDecimal calcularSubtotal() {
        return articulo.getPrecio().multiply(
                BigDecimal.valueOf(cantidad)
        );
    }

    public long calcularPuntos() {
        return (long) articulo.calcularPuntos() * cantidad;
    }
}