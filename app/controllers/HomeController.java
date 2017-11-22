package controllers;

import play.mvc.Controller;
import play.mvc.Result;

import javax.inject.Inject;
import play.Configuration;
import java.lang.System;
import java.lang.String;
import java.util.concurrent.CompletionStage;
import play.libs.ws.*;
//import com.fasterxml.jackson.databind.JsonNode;
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


        WSRequest request = ws.url("https://sync.bankin.com/v2/transactions?client_id="+config.getString("bankin.clientId")+"&client_secret="+config.getString("bankin.clientSecret"))
                .setHeader("Bankin-Version", "2016-01-18")
                .setHeader("Authorization", "Bearer "+accessToken)
                .setRequestTimeout(5000);

        //CompletionStage<JsonNode> jsonPromise = request.get().thenApply(WSResponse::asJson);

        /*
        ObjectNode result = Json.newObject();
    result.put("exampleField1", "foobar");
    result.put("exampleField2", "Hello world!");
    return ok(result);
         */
        //jsonPromise.thenApplyAsync()
        return request.get().thenApply(response -> {
            //WSResponseHeaders responseHeaders = response. getHeaders();
            /*
            System.out.println("responnnnnnse : "+response.getStatus());
            if (response.getStatus() == 200) {

            }
            */
            //return ok("");
            return new Result(401);
        });
        /*
        return CompletableFuture.supplyAsync(() -> returnTrois() )
                .thenApply(i -> ok("Got result: " + i));

        //https://sync.bankin.com/v2/transactions?client_id=775683bc70d94beaa8044c81b2f16006&client_secret=sMgdYUzUPpo1DxbR67qP2ZbuTmU7H9gikvWPigDnQro9fk0PsRcb4EvI0iRheAJr
        return ok();
        */
    }
/*
    private int returnTrois() {
        System.out.println("email : '" + email + "' password : '" + password + "'");
        System.out.println("AT : "+accessToken);
        System.out.println(
            config.getString("bankin.clientId") + " " +
            config.getString("bankin.clientSecret")
        );
        return 3;
    }
    */
}
