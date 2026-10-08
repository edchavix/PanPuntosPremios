package panpuntospremios;

import java.math.BigDecimal;

public class MenuCompleto extends ItemVenta {

    private final ProductoIndividual sandwich;
    private final ProductoIndividual bebida;
    private final ProductoIndividual acompanamiento;

    public MenuCompleto(long id, String codigo, String nombre,
            BigDecimal precio, ProductoIndividual sandwich,
            ProductoIndividual bebida,
            ProductoIndividual acompanamiento) {

        super(id, codigo, nombre, precio);

        if (sandwich == null || bebida == null
                || acompanamiento == null) {
            throw new IllegalArgumentException(
                    "El menu debe tener sus tres componentes."
            );
        }

        if (!"SANDWICH".equals(sandwich.getCategoria())) {
            throw new IllegalArgumentException(
                    "El primer componente debe ser un sandwich."
            );
        }

        if (!"BEBIDA".equals(bebida.getCategoria())) {
            throw new IllegalArgumentException(
                    "El segundo componente debe ser una bebida."
            );
        }

        if (!"ACOMPANAMIENTO".equals(
                acompanamiento.getCategoria())) {
            throw new IllegalArgumentException(
                    "El tercer componente debe ser un acompanamiento."
            );
        }

        this.sandwich = sandwich;
        this.bebida = bebida;
        this.acompanamiento = acompanamiento;
    }

    public ProductoIndividual getSandwich() {
        return sandwich;
    }

    public ProductoIndividual getBebida() {
        return bebida;
    }

    public ProductoIndividual getAcompanamiento() {
        return acompanamiento;
    }

    @Override
    public int calcularPuntos() {
        return 8;
    }
}