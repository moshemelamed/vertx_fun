package com.example.starter;

import io.vertx.jdbcclient.*;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.spi.Driver;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import io.vertx.core.json.*;
import io.netty.handler.codec.http.HttpContentEncoder.Result;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.*;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLClient;
import io.vertx.ext.web.*;
//AsyncMap
//loadbalencing
//test with Prometheus or Grafana 
public class MainVerticle extends AbstractVerticle {
  
  String url = "jdbc:sqlite:C:/vert.x-project/database/words.db";
  @Override
  public void start(Promise <Void> start) throws Exception {
    
    //creates many instances for the testRequest vertical
    DeploymentOptions opts = new DeploymentOptions()
      .setWorker(true)
      .setInstances(8);
    vertx.deployVerticle("com.example.starter.TestRequest", opts);
    //connect to sqlite
  // Create a Router
    Router router = Router.router(vertx);
    
    // Mount the handler for all incoming requests at every path and HTTP method
    router.post("/analyze").handler(this::analyzeText);
    
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
      .onSuccess(server ->
        System.out.println(
          "HTTP server started on port " + server.actualPort()
        )
      );
  }

  void analyzeText(RoutingContext context){
    MultiMap queryParams = context.queryParams();
    String text = queryParams.contains("text") ? queryParams.get("text") : "unknown";
    vertx.eventBus().request("text.analyze", text ,reply -> {
        context.json(
          reply.result().body()
        );
    });
  }
}