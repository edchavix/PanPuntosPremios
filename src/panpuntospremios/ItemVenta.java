package panpuntospremios;

import java.math.BigDecimal;

public abstract class ItemVenta {

    private final long id;
    private final String codigo;
    private final String nombre;
    private final BigDecimal precio;

    protected ItemVenta(long id, String codigo,
            String nombre, BigDecimal precio) {

        if (id <= 0) {
            throw new IllegalArgumentException(
                    "El articulo debe estar registrado."
            );
        }

        if (codigo == null || codigo.isBlank()
                || nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException(
                    "El codigo y el nombre son obligatorios."
            );
        }

        if (precio == null || precio.signum() < 0) {
            throw new IllegalArgumentException(
                    "El precio no puede ser negativo."
            );
        }

        this.id = id;
        this.codigo = codigo;
        this.nombre = nombre;
        this.precio = precio;
    }

    public long getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public abstract int calcularPuntos();
}