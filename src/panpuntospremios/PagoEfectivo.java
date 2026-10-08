package panpuntospremios;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class PagoEfectivo {

    private final BigDecimal monto;
    private final BigDecimal recibido;
    private final BigDecimal cambio;

    public PagoEfectivo(BigDecimal monto, BigDecimal recibido) {
        this.monto = validarImporte(monto, "El total");
        this.recibido = validarImporte(recibido, "El efectivo recibido");

        if (this.recibido.compareTo(this.monto) < 0) {
            BigDecimal faltante = this.monto.subtract(this.recibido);

            throw new IllegalArgumentException(
                    "Efectivo insuficiente. Faltan "
                    + faltante.toPlainString() + "."
            );
        }

        this.cambio = this.recibido.subtract(this.monto);
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public BigDecimal getRecibido() {
        return recibido;
    }

    public BigDecimal getCambio() {
        return cambio;
    }

    private BigDecimal validarImporte(
            BigDecimal importe, String nombre) {

        if (importe == null) {
            throw new IllegalArgumentException(
                    nombre + " es obligatorio."
            );
        }

        if (importe.signum() < 0) {
            throw new IllegalArgumentException(
                    nombre + " no puede ser negativo."
            );
        }

        if (importe.compareTo(
                new BigDecimal("9999999999.99")) > 0) {
            throw new IllegalArgumentException(
                    nombre + " supera el limite permitido."
            );
        }

        try {
            return importe.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException error) {
            throw new IllegalArgumentException(
                    nombre + " admite como maximo dos decimales."
            );
        }
    }
}