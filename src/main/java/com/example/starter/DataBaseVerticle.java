package com.example.starter;

// import org.flywaydb.core.Flyway;
// import org.flywaydb.core.api.FlywayException;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLClient;
import io.vertx.ext.sql.SQLConnection;

public class DataBaseVerticle extends AbstractVerticle{

    // private static final String LIST_ALL_TODOS = "SELECT * FROM todos ORDER BY created ASC";
    // private static final String GET_TODO_BY_ID = "SELECT * FROM todos WHERE id = ?";
    // private static final String UPDATE_TODO = "UPDATE todos SET title = ?, description = ?, due_date = ?, complete = ? WHERE id = ? RETURNING *";
    // private static final String ADD_TODO = "INSERT INTO todos (title, description, due_date, complete) VALUES (?, ?, ?, ?) RETURNING *";

    SQLClient client;

  @Override
  public void start(Promise<Void> start) throws Exception {
      
  }

  
}