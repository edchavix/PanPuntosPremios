package panpuntospremios;

import java.math.BigDecimal;

public class Producto {

    private long idProducto;
    private String codigo;
    private String categoria;
    private String nombre;
    private BigDecimal precio;
    private long stock;
    private boolean activo;

    public Producto(String codigo, String categoria,
            String nombre, BigDecimal precio) {
        this.codigo = codigo;
        this.categoria = categoria;
        this.nombre = nombre;
        setPrecio(precio);
        this.stock = 0;
        this.activo = true;
    }

    public long getIdProducto() {
        return idProducto;
    }

    public void setIdProducto(long idProducto) {
        this.idProducto = idProducto;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public void setPrecio(BigDecimal precio) {
        if (precio == null || precio.signum() < 0) {
            throw new IllegalArgumentException(
                    "El precio es obligatorio y no puede ser negativo."
            );
        }
        this.precio = precio;
    }

    public long getStock() {
        return stock;
    }

    public void setStock(long stock) {
        if (stock < 0) {
            throw new IllegalArgumentException(
                    "Las existencias no pueden ser negativas."
            );
        }
        this.stock = stock;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }
}