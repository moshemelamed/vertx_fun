package com.example.starter;

import io.vertx.core.json.*;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.*;
import io.vertx.ext.web.*;
import io.vertx.sqlclient.*;
import io.vertx.jdbcclient.JDBCPool;
import io.vertx.jdbcclient.JDBCConnectOptions;
import io.vertx.sqlclient.SqlClient;
// import io.vertx.sqlclient.JDBCClient;
// import io.vertx.sqlclient.jdbc.JDBCConnectOptions;
// import io.vertx.sqlclient.PoolOptions;
//AsyncMap
//loadbalencing
//test with Prometheus or Grafana 
public class MainVerticle extends AbstractVerticle {
  // private MSSQLServer server;
  protected JDBCPool client;
  String url = "jdbc:sqlite:/database/words.db";

  @Override
  public void start(Promise<Void> start) throws Exception {

    // creates many instances for the testRequest vertical
    DeploymentOptions opts = new DeploymentOptions()
        .setWorker(true)
        .setInstances(8);
    vertx.deployVerticle("com.example.starter.TestRequest", opts);
    // connect to sqlite
    
    // Create a Router
    Router router = Router.router(vertx);

    // Mount the handler for all incoming requests at every path and HTTP method
    router.post("/analyze").handler(this::analyzeText);
    router.post("/").handler(this::selectAll);

    ConfigStoreOptions defaultConfig = new ConfigStoreOptions()
        .setType("file")
        .setFormat("json")
        .setConfig(new JsonObject().put("path", "config.json"));
    // Create the HTTP server
    vertx.createHttpServer()
        // Handle every request using the router
        .requestHandler(router)
        // Start listening
        .listen(8080)
        // Print the port
        .onSuccess(server -> System.out.println(
            "HTTP server started on port " + server.actualPort()));
  }

  void analyzeText(RoutingContext context) {
    MultiMap queryParams = context.queryParams();
    String text = queryParams.contains("text") ? queryParams.get("text") : "unknown";
    vertx.eventBus().request("text.analyze", text, reply -> {
      context.json(
          reply.result().body());
    });
  }

  void selectAll(RoutingContext context) {
    
    JsonObject config = new JsonObject();
		config.put("url", String.format("jdbc:sqlite:%s/database/words.db", System.getProperty("user.dir")))
          .put("driver_class","org.sqlite.JDBC");
		JDBCPool jdbcPool = JDBCPool.pool(vertx, config);
    jdbcPool.query("SELECT * FROM words")
        .execute()
        .onFailure(e -> {
          System.out.println("failed to get the data");
          context.end();
        })
        .onSuccess(rows -> {
          for (Row row : rows) {
            System.out.println(row.getString("text"));
          }
        });
        context.end();
  }
}