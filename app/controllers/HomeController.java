package controllers;

import play.mvc.Controller;
import play.mvc.Result;

import javax.inject.Inject;
import play.Configuration;
import java.lang.System;
import java.lang.String;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import play.libs.ws.*;
import play.mvc.BodyParser.Json;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
//import static java.util.concurrent.CompletableFuture;

/**
 * This controller contains an action to handle HTTP requests
 * to the application's home page.
 */
public class HomeController extends Controller {

    @Inject Configuration config;
    @Inject WSClient ws;
    private String accessToken = "";


    public Result index() {
        return ok(views.html.index.render());
    }

    public CompletionStage<Result> roundedTransactions(String email, String password) {

        return getAllTransaction(email, password, null);
    }

    private CompletionStage<Result> getAllTransaction(String email, String password, String nextUri) {

        WSRequest request = ws.url("https://sync.bankin.com/v2/transactions")
                .setHeader("Bankin-Version", "2016-01-18")
                .setHeader("Authorization", "Bearer "+accessToken)
                .setQueryParameter("client_id",     config.getString("bankin.clientId"))
                .setQueryParameter("client_secret", config.getString("bankin.clientSecret"))
                .setRequestTimeout(5000);

        if(nextUri != null){
            request = ws.url(nextUri)
                    .setHeader("Bankin-Version", "2016-01-18")
                    .setHeader("Authorization", "Bearer "+accessToken)
                    .setRequestTimeout(5000);
        }

        return request.get().thenCompose(response -> {

            //Bankin response has an unauthorized response, so token is bad, so renew token
            if (response.getStatus() == 401) {
                return renewBankinAccessToken(email, password).thenCompose(atResp -> {

                    //Token has been released, replay transaction request
                    if (atResp.getStatus() == 200) {
                        return getAllTransaction(email, password, null);
                    }
                    //Token not released, render error with status
                    return CompletableFuture.supplyAsync(() -> status(atResp.getStatus(), atResp.asJson()).as("application/json"));
                });
            }

            JsonNode jsonResp = response.asJson();
            if(jsonResp.has("resources"){
                ObjRes toto = jsonResp.findPath("resources");

                if(jsonResp.has("pagination")){
                    String jsonNextUri = jsonResp.findPath("pagination").findPath("next_uri").textValue();
                    if(jsonNextUri != null){
                        return getAllTransaction(email, password, jsonNextUri);
                    }
                    System.out.println("pagination is null");
                }

                return CompletableFuture.supplyAsync(() -> ok(toto).as("application/json"));
            }

            return CompletableFuture.supplyAsync(() -> status(response.getStatus(), jsonResp).as("application/json"));
        });
    }

    private CompletionStage<WSResponse> renewBankinAccessToken(String email, String password) {

        WSRequest request = ws.url("https://sync.bankin.com/v2/authenticate")
                .setHeader("Bankin-Version", "2016-01-18")
                .setQueryParameter("password", password)
                .setQueryParameter("email", email)
                .setQueryParameter("client_id", config.getString("bankin.clientId"))
                .setQueryParameter("client_secret", config.getString("bankin.clientSecret"))
                .setRequestTimeout(5000);

        return request.post("").whenComplete((response, ex) -> {
            if (response.getStatus() == 200) {
                accessToken = response.asJson().findPath("access_token").textValue();
                if(accessToken == null){
                    accessToken = "";
                }
            }
        });
    }
}
