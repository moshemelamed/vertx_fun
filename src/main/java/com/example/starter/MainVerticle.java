package com.example.starter;

import io.vertx.core.json.*;
import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.*;
import io.vertx.ext.web.*;
import io.vertx.sqlclient.*;
import io.vertx.jdbcclient.JDBCPool;

//AsyncMap
//loadbalencing
//test with Prometheus or Grafana 
public class MainVerticle extends AbstractVerticle {
  // private MSSQLServer server;
  protected JDBCPool client;
  //link to DB need to implement config.json
  String url = "jdbc:sqlite:/database/words.db";

  @Override
  //Starts the vertx instance/thread - need to implement complete() and fail() on the method 
  public void start(Promise<Void> start) throws Exception {
    Router router = Router.router(vertx);
    router.post("/analyze").handler(this::analyzeText);
    //remove -- test connection to DB and data in DB - testing not for production
    router.post("/").handler(this::selectAll);
    DeploymentOptions opts = new DeploymentOptions()
        .setWorker(true)
        //setting 8 instances of vertical TestRequest so each is handled separately 
        .setInstances(8);
    vertx.deployVerticle("com.example.starter.TestRequest", opts);

    doConfig(start, router);
  }
    // creates many instances for the testRequest vertical
    
    // connect to sqlite
    Future<JsonObject> doConfig(Promise<Void> start, Router router) {
      ConfigStoreOptions defaultConfig = new ConfigStoreOptions()
      .setType("file")
      .setFormat("json")
      .setConfig(new JsonObject().put("path", String.format("%s/src/main/java/com/example/resources/config.json", System.getProperty("user.dir"))));

    ConfigRetrieverOptions copts = new ConfigRetrieverOptions()
      .addStore(defaultConfig);
    
    ConfigRetriever cr = ConfigRetriever.create(vertx, copts);

    Handler<AsyncResult<JsonObject>> handler = AsyncResult -> this.handleConfigResults(start, router, AsyncResult);
    cr.getConfig(handler);

      return Future.future(promise -> cr.getConfig(promise));
  }

    // Create a Router
    

    

    
    // Mount the handler for all incoming requests at every path and HTTP method to the analyzeText function
    
    //TODO: implement the config file for all final configurations
    // ConfigStoreOptions defaultConfig = new ConfigStoreOptions()
    //     .setType("file")
    //     .setFormat("json")
    //     .setConfig(new JsonObject().put("path", "config.json")); 
    //-----------------------------------------------------------------
    // Create the HTTP server -- ?should we add the port number as a variable
    //? to increase the number of ports that will handle the requests to the server
    

  void handleConfigResults(Promise<Void> start, Router router, AsyncResult <JsonObject> asyncResult){
    if(asyncResult.succeeded()){
      JsonObject config = asyncResult.result();
      JsonObject http = config.getJsonObject("http");
      int httpPort = http.getInteger("port");
      vertx.createHttpServer()
      // Handle every request using the router
      .requestHandler(router)
      // Start listening
      .listen(httpPort)
      // Print the port
      .onSuccess(server -> System.out.println(
          "HTTP server started on port " + server.actualPort()));
      start.complete();
    }else{
      start.fail("problem with connecting to the config file");
    }
  }
  
  void analyzeText(RoutingContext context) {
    //maps the posted parameter from the post request 
    MultiMap queryParams = context.queryParams();
    //we exctract the "text" field from the post request
    //TODO: handle the case without a text field value
    String text = queryParams.contains("text") ? queryParams.get("text") : "unknown";
    //internuly sending a request to the text.analyze route  
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