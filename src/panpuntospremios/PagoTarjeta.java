package panpuntospremios;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class PagoTarjeta {

    private final BigDecimal monto;
    private final String referencia;
    private final boolean aprobado;

    public PagoTarjeta(BigDecimal monto, String referencia,
            boolean aprobado) {

        if (monto == null || monto.signum() < 0) {
            throw new IllegalArgumentException(
                    "El monto es obligatorio y no puede ser negativo."
            );
        }

        if (monto.compareTo(
                new BigDecimal("9999999999.99")) > 0) {
            throw new IllegalArgumentException(
                    "El monto supera el limite permitido."
            );
        }

        try {
            this.monto = monto.setScale(
                    2, RoundingMode.UNNECESSARY
            );
        } catch (ArithmeticException error) {
            throw new IllegalArgumentException(
                    "El monto admite como maximo dos decimales."
            );
        }

        if (referencia == null || referencia.isBlank()) {
            throw new IllegalArgumentException(
                    "La referencia de la simulacion es obligatoria."
            );
        }

        if (referencia.trim().length() > 100) {
            throw new IllegalArgumentException(
                    "La referencia admite un maximo de 100 caracteres."
            );
        }

        this.referencia = referencia.trim();
        this.aprobado = aprobado;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public String getReferencia() {
        return referencia;
    }

    public boolean isAprobado() {
        return aprobado;
    }

    public void exigirAprobacion() {
        if (!aprobado) {
            throw new IllegalArgumentException(
                    "Pago con tarjeta simulado rechazado. "
                    + "Puedes intentar el pago nuevamente."
            );
        }
    }
}