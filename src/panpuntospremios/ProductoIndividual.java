package panpuntospremios;

import java.math.BigDecimal;

public class ProductoIndividual extends ItemVenta {

    private final String categoria;

    public ProductoIndividual(long id, String codigo,
            String nombre, BigDecimal precio, String categoria) {

        super(id, codigo, nombre, precio);

        if (!"SANDWICH".equals(categoria)
                && !"BEBIDA".equals(categoria)
                && !"ACOMPANAMIENTO".equals(categoria)) {
            throw new IllegalArgumentException(
                    "La categoria del producto no es valida."
            );
        }

        this.categoria = categoria;
    }

    public String getCategoria() {
        return categoria;
    }

    @Override
    public int calcularPuntos() {
        if ("SANDWICH".equals(categoria)) {
            return 2;
        }

        return 0;
    }
}