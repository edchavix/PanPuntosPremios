package panpuntospremios;

public class Cliente {

    private long idCliente;
    private String identificacion;
    private String nombre;
    private String telefono;
    private String correo;
    private long puntos;
    private boolean activo;

    public Cliente(String identificacion, String nombre,
            String telefono, String correo) {
        this.identificacion = identificacion;
        this.nombre = nombre;
        this.telefono = telefono;
        this.correo = correo;
        this.puntos = 0;
        this.activo = true;
    }

    public long getIdCliente() {
        return idCliente;
    }

    public void setIdCliente(long idCliente) {
        this.idCliente = idCliente;
    }

    public String getIdentificacion() {
        return identificacion;
    }

    public void setIdentificacion(String identificacion) {
        this.identificacion = identificacion;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public long getPuntos() {
        return puntos;
    }

    public void setPuntos(long puntos) {
        if (puntos < 0) {
            throw new IllegalArgumentException(
                    "Los puntos no pueden ser negativos."
            );
        }
        this.puntos = puntos;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }
}