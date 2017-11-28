package controllers;

import play.mvc.Controller;
import play.mvc.Result;
import mappings.*;
import javax.inject.Inject;
import play.Configuration;
import java.lang.System;
import java.lang.String;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.*;

import play.libs.ws.*;
import play.libs.Json;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import utils.Url;

/**
 * This controller contains an action to handle HTTP requests
 * to the application's home page.
 */
public class HomeController extends Controller {

    @Inject Configuration config;
    @Inject WSClient ws;

    private String accessToken = "";
    private ArrayList<Transaction> transactionList = new ArrayList<Transaction>();


    public Result index() {
        return ok(views.html.index.render());
    }


    public CompletionStage<Result> roundedTransactions(String email, String password) {
        transactionList.clear();
        return getAllTransaction(email, password, null);
    }

    private CompletionStage<Result> getAllTransaction(String email, String password, String afterParam) {

        WSRequest request = ws.url("https://sync.bankin.com/v2/transactions")
                .setHeader("Bankin-Version", "2016-01-18")
                .setHeader("Authorization", "Bearer " + accessToken)
                .setQueryParameter("client_id",     config.getString("bankin.clientId"))
                .setQueryParameter("client_secret", config.getString("bankin.clientSecret"))
                .setRequestTimeout(10000);
        if(afterParam != null){
            request.setQueryParameter("after", afterParam);
        }

        return request.get().thenComposeAsync(response -> {

            //Bankin response has an unauthorized response, so token is bad, so renew token
            if (response.getStatus() == 401) {
                return renewBankinAccessToken(email, password).thenComposeAsync(atResp -> {

                    //Token has been released, replay transaction request
                    if (atResp.getStatus() == 200) {
                        return getAllTransaction(email, password, null);
                    }
                    //Token not released, render error with status
                    return CompletableFuture.supplyAsync(() -> status(atResp.getStatus(), atResp.asJson()).as("application/json"));
                });
            }

            JsonNode jsonResp = response.asJson();
            if (jsonResp.has("resources")) {

                System.out.println("add transaction to array "+afterParam);

                ArrayNode arrayRes = (ArrayNode) jsonResp.withArray("resources");
                Iterator<JsonNode> it = arrayRes.iterator();
                while (it.hasNext()) {
                    Transaction resource = Json.fromJson(it.next(), Transaction.class);
                    transactionList.add(resource);
                }

                if (jsonResp.has("pagination")) {
                    String jsonNextUri = jsonResp.findPath("pagination").findPath("next_uri").textValue();
                    if (jsonNextUri != null && jsonNextUri.length() > 0) {
                        try {
                            jsonNextUri = Url.getParamValueUrl(new URL("https://sync.bankin.com" + jsonNextUri), "after");
                        } catch(MalformedURLException e){}

                        //Throttle 10 requests per 1 sec max
                        try {Thread.sleep(100);} catch (InterruptedException e) {}

                        if(jsonNextUri != null){
                            return getAllTransaction(email, password, jsonNextUri);
                        }
                    }
                }
                return CompletableFuture.supplyAsync(() -> ok(Json.toJson(transactionList)).as("application/json"));
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
