package com.example.starter;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import io.vertx.core.json.*;

import io.vertx.core.*;
import java.sql.*;
import io.vertx.core.AbstractVerticle;

public class TestRequest extends AbstractVerticle {
    static final String DB_URL = "jdbc:sqlite:database/words.db";
    static final String SELECT = "SELECT text, value FROM words";

    @Override
    public void start(Promise<Void> start) {
        vertx.eventBus().consumer("text.analyze", msg -> {
            String text = (String) msg.body();
            String reducedtext = reduceText(text.toLowerCase());
            String lexical = concatenateValues(reducedtext);
            // String reduceLexical = reduceValues(lexical);
            final String SELECT_TEXT = "SELECT * FROM words WHERE ABS(value - " + lexical
                    + ") = (SELECT MIN(ABS(value - " + lexical + ")) FROM words) ORDER BY ABS(value - " + lexical
                    + ") LIMIT 1";
            final String INSERT = "INSERT INTO words ('text', 'value') VALUES ('" + reducedtext + "', '" + lexical
                    + "')";
            try (Connection conn = DriverManager.getConnection(DB_URL, "", "");
            // Statement stmt = conn.createStatement();
            ) {
                // stmt.executeUpdate(INSERT);
                msg.reply(selectLexical(SELECT_TEXT, conn));
            } catch (SQLException e) {
                e.printStackTrace();
            }

            try (Connection conn = DriverManager.getConnection(DB_URL, "", "");
            Statement stmt = conn.createStatement();
            ) {
                stmt.executeUpdate(INSERT);
                // msg.reply(selectLexical(SELECT_TEXT, conn));
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public static String concatenateValues(String text) {
        String result = "";
        for (int i = 0; i < text.length(); i++) {
            result += Integer.toString((int) text.charAt(i));
            if(result.length() < 3){
                result = "0" + result;
            }
        }
        result = padWithZeros(result);
        return result;
    }

    public static String padWithZeros(String input) {
        StringBuilder sb = new StringBuilder(input);
        while (sb.length() < 45) {
            sb.append("0");
        }
        return sb.toString();
    }

    public static String reduceText(String text) {
        if (text.length() > 15) {
            return text.substring(0, 15);
        } else {
            return text;
        }
    }

    public static JsonObject selectLexical(String text,Connection conn) {
        try (PreparedStatement stmt = conn.prepareStatement(text)) {
            JsonObject object;
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String jsonString = "{\"value\": \"" + rs.getString("text") + "\", \"lexical\": \""
                        + removeTrailingZeros(rs.getString("value")) + "\"}";
                object = new JsonObject(jsonString);
            } else {
                String jsonString = "{\"value\": \"null\", \"lexical\": \"null\"}";
                object = new JsonObject(jsonString);
            }
            return object;
        } catch (SQLException e) {
            System.err.println("Error executing query: " + e.getMessage());
            String jsonString = "{\"foo\":\"bar\"}";
            JsonObject errobject = new JsonObject(jsonString);
            return errobject;
        }
        
    }

    public static String removeTrailingZeros(String input) {
        int i = input.length() - 1;
        while (i >= 0 && input.charAt(i) == '0') {
          i--;
        }
        return input.substring(0, i + 1);
      }
}
