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
        //self send from MainVerticle
        vertx.eventBus().consumer("text.analyze", msg -> {
            JsonObject config = new JsonObject();
            String text = (String) msg.body();
            String reducedtext = reduceText(text.toLowerCase());
            String lexical = concatenateValues(reducedtext);
            // String reduceLexical = reduceValues(lexical);
            final String SELECT_TEXT = "SELECT * FROM words WHERE ABS(ROUND(value, 4) - ROUND('"+lexical+"', 4)) =" + 
            "(SELECT MIN(ABS(ROUND(value - '"+lexical+"', 4))) FROM words) ORDER BY ABS(ROUND(value, 4) - ROUND('"+lexical+"', 4))";
            final String INSERT = "INSERT INTO words ('text', 'value') VALUES ('" + reducedtext + "', '" + lexical
                    + "')";//INSERT INTO words (text, value) SELECT 'a', '097' WHERE NOT EXISTS (SELECT 1 FROM words WHERE text = 'a')
            config.put("url", String.format("jdbc:sqlite:%s/database/words.db", System.getProperty("user.dir")))
                  .put("driver_class","org.sqlite.JDBC");
                  JDBCPool jdbcPool = JDBCPool.pool(vertx, config);
                  selectLexical(SELECT_TEXT, jdbcPool, ar -> {
                    if (ar.succeeded()) {
                      JsonObject json = ar.result();
                      msg.reply(json);
                      // do something with the json object
                    } else {
                        System.out.println("-----------"+ar.failed());
                    }
                  });
                  insertIfNew(INSERT, jdbcPool);
        });
    }

    public static String concatenateValues(String text) {
        String result = "";
        String newChar = "";
        for (int i = 0; i < text.length(); i++) {
            newChar = Integer.toString((int) text.charAt(i));
            if (newChar.length() < 3) {
                newChar = "0" + newChar;
            }
            result += newChar;
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

    public static JsonObject selectLexical(String text, JDBCPool pool,  Handler<AsyncResult<JsonObject>> handler) {
        JsonObject object = new JsonObject();
        pool.query(text)
            .execute()
            .onFailure(e -> {
                handler.handle(Future.failedFuture(e.getCause()));
            })
            .onSuccess(rows -> {
                if (rows.size() > 0) {
                    for (Row row : rows) {
                        object.put("value", removeTrailingZeros(row.getString("value")))
                              .put("lexical", row.getString("text"));
                    }
                }else{
                    object.put("value", "null")
                            .put("lexical", "null");
                }
                
                handler.handle(Future.succeededFuture(object));
            });
            return object;
    }

    public static JsonObject insertIfNew(String text, JDBCPool pool) {
        JsonObject object = new JsonObject();
        pool.query(text)
            .execute()
            .onFailure(e -> {
                ///handler.handle(Future.failedFuture(e.getCause()));
            })
            .onSuccess(rows -> {
                System.out.println("----------------successfully updated the database");
            });    
        return object;
    }

    public static String removeTrailingZeros(String input) {
        int i = input.length() - 1;
        while (i >= 0 && input.charAt(i) == '0') {
            i--;
        }
        return input.substring(0, i + 1);
    }
}
