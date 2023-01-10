package com.example.starter;

import io.vertx.core.json.*;
import io.vertx.jdbcclient.JDBCPool;
import io.vertx.sqlclient.Row;
import io.vertx.core.*;
import io.vertx.core.AbstractVerticle;

public class TestRequest extends AbstractVerticle {
    static final String DB_URL = "jdbc:sqlite:database/words.db";
    static final String SELECT = "SELECT text, value FROM words";
    @Override
    public void start(Promise<Void> start) {
        JsonObject config = new JsonObject();
        JDBCPool jdbcPool = JDBCPool.pool(vertx, config);
        config.put("url", String.format("jdbc:sqlite:%s/database/words.db", System.getProperty("user.dir")))
                .put("driver_class", "org.sqlite.JDBC");
        // self send from MainVerticle
        vertx.eventBus().consumer("text.analyze", msg -> {

            String text = (String) msg.body();
            String reducedtext = reduceText(text.toLowerCase());
            String lexical = concatenateValues(reducedtext);
            Integer sumOfTextCarValue = sumTextCharValue(reducedtext);
            final String SELECT_TEXT_FOR_LEXICAL = "SELECT * FROM words WHERE ABS(ROUND(value, 4) - ROUND('" + lexical + "', 4)) ="+
                    "(SELECT MIN(ABS(ROUND(value - '" + lexical
                    + "', 4))) FROM words) ORDER BY ABS(ROUND(value, 4) - ROUND('" + lexical + "', 4)) LIMIT 1";
            final String SELECT_TEXT_FOR_TEXT_SUM = "SELECT * FROM words WHERE ABS(textSum - '" + sumOfTextCarValue + "') =(SELECT MIN(ABS(textSum - '" + sumOfTextCarValue + "')) FROM words) ORDER BY ABS(textSum - '" + sumOfTextCarValue + "') LIMIT 1";
            final String INSERT = "INSERT INTO words ('text', 'value', 'textSum') VALUES ('" + reducedtext + "', '" + lexical
                    + "', " + sumOfTextCarValue + ")";
            JsonObject resObject = new JsonObject();
            selectLexical(SELECT_TEXT_FOR_LEXICAL, jdbcPool).onSuccess(res -> {
                resObject.put("lexical", res.getString("lexical"));
                
               
            }).onFailure(res -> {

                msg.reply(new JsonObject().put("msg", "cant execute the selectLexical query"));
                // do something with the json object
            });
            
            selectTextSum(SELECT_TEXT_FOR_TEXT_SUM, jdbcPool).onSuccess(res -> {
                resObject.put("value", res.getString("value"));
                msg.reply(resObject);
                insertIfNew(INSERT, jdbcPool);
            }).onFailure(res -> {

                msg.reply(new JsonObject().put("msg", "cant execute the selectTextSum query"));
                // do something with the json object
            });
            
            
        });
    }

    public static String concatenateValues(String text) {
        String result = "";
        String newChar = "";
        for (int i = 0; i < text.length(); i++) {
            newChar = Integer.toString((int) text.charAt(i));
            if (newChar.length() == 2) {
                newChar = "0" + newChar;
            }else if (newChar.length() == 1) {
                newChar = "00" + newChar;
            }
            result += newChar;
        }
        result = padWithZeros(result);
        return result;
    }

    public static Integer sumTextCharValue(String text) {
        Integer sumOfChar = 0;
        for (int i = 0; i < text.length(); i++) {
            sumOfChar = sumOfChar + (int) text.charAt(i);
        }
        System.out.println(sumOfChar);
        return sumOfChar;
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

    public static Future<JsonObject> selectLexical(String text, JDBCPool pool) {
        JsonObject object = new JsonObject();
        return pool.query(text)
                .execute()
                .map(rows -> {
                    if (rows.size() > 0) {
                        for (Row row : rows) {
                            object.put("lexical", row.getString("text"));
                        }
                    } else {
                        object.put("lexical", "null");
                    }
                    return object;
                });
    }

    public static Future<JsonObject> selectTextSum(String text, JDBCPool pool) {
        JsonObject object = new JsonObject();
        return pool.query(text)
                .execute()
                .map(rows -> {
                    if (rows.size() > 0) {
                        for (Row row : rows) {
                            object.put("value", row.getString("text"));
                        }
                    } else {
                        object.put("value", "null");
                    }
                    return object;
                });
    }

    public static void insertIfNew(String text, JDBCPool pool) {
        JsonObject object = new JsonObject();
        pool.query(text)
                .execute();
    }

    public static String removeTrailingZeros(String input) {
        int i = input.length() - 1;
        while (i >= 0 && input.charAt(i) == '0') {
            i--;
        }
        return input.substring(0, i + 1);
    }
}
