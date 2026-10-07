package panpuntospremios;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConexionBD {

    public static Connection conectar(String clave) throws SQLException {
        String url = "jdbc:oracle:thin:@//127.0.0.1:1521/FREEPDB1";
        return DriverManager.getConnection(url, "PAN_PREMIOS", clave);
    }
}