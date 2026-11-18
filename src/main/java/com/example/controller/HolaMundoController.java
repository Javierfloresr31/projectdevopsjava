package com.example.controller;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HolaMundoController {

    @Value("${azure.sql.server}")
    private String sqlServer;

    @Value("${azure.sql.database}")
    private String sqlDatabase;

    @GetMapping("/")
    public String holaMundo() {
        return "¡Hola haciendo prueba de Jenkins";
    }

    @GetMapping("/saludo")
    public String saludo() {
        return "Hola " + System.getenv("usuariodev") + ". Saludos.";
    }

    @GetMapping("/secreto")
    public String secreto() {
        String secretodev = System.getenv("secretodev");
        if (secretodev == null) {
            return "La variable de entorno secretodev no está definida";
        }
        return secretodev;
    }

    /*@GetMapping("/basedatos")
    public Object basedatos() {
        String user = System.getenv("USER");
        String password = System.getenv("PASSWORD");
        if (user == null || user.isBlank()) {
            return "La variable de entorno USER no está definida";
        }
        if (password == null || password.isBlank()) {
            return "La variable de entorno PASSWORD no está definida";
        }

        String url = "jdbc:sqlserver://" + sqlServer + ":1433"
                + ";databaseName=" + sqlDatabase
                + ";encrypt=true"
                + ";trustServerCertificate=false"
                + ";hostNameInCertificate=*.database.windows.net"
                + ";loginTimeout=30";

        try (Connection connection = DriverManager.getConnection(url, user, password);
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT * FROM dbo.Users")) {

            ResultSetMetaData meta = resultSet.getMetaData();
            int columns = meta.getColumnCount();
            List<Map<String, Object>> rows = new ArrayList<>();

            while (resultSet.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= columns; i++) {
                    row.put(meta.getColumnLabel(i), resultSet.getObject(i));
                }
                rows.add(row);
            }
            return rows;
        } catch (SQLException e) {
            return "Error al consultar la base de datos: " + e.getMessage();
        }
    }*/
}

