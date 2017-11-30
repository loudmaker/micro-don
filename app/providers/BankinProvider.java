package providers;

import mappings.Transaction;
import play.Configuration;
import play.libs.F;
import play.libs.Json;
import play.libs.ws.WSClient;
import play.libs.ws.WSRequest;
import play.mvc.Result;
import play.mvc.Results;
import utils.Url;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;


public class BankinProvider extends Results implements Provider {


    private static Configuration config;
    private static WSClient ws;
    private String email;
    private String password;
    private String accessToken = "";
    private String afterParam = null;


    //Sure its not the best way to retrieve conf and ws
    public BankinProvider(Configuration config, WSClient ws, String email, String password) {
        this.config = config;
        this.ws = ws;
        this.email = email;
        this.password = password;
    }

    public CompletionStage<F.Either<ArrayList<Transaction>, Result>> getAllTransaction() {

        ArrayList<Transaction> transactionList = new ArrayList<Transaction>();

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
                return renewBankinAccessToken(email, password).thenComposeAsync(hasAccessToken -> {

                    //Token has been released, replay transaction request
                    if (hasAccessToken.left.isPresent()) {
                        accessToken = hasAccessToken.left.get();
                        return getAllTransaction();
                    } else {
                        //Token not released, render error with status
                        return CompletableFuture.supplyAsync(() -> F.Either.Right(hasAccessToken.right.get()));
                    }
                });
            }

            JsonNode jsonResp = response.asJson();
            if (jsonResp.has("resources")) {

                //System.out.println("add transaction to array " + afterParam);

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
                            afterParam = Url.getParamValueUrl(new URL("https://sync.bankin.com" + jsonNextUri), "after");
                        } catch (MalformedURLException e) {
                        }

                        //Throttle 10 requests per 1 sec max
                        //NOT a good solution but the async way do not block controller thread
                        //Play 2.6 has a delay option on CompletionStage ;)
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                        }

                        if (afterParam != null) {

                            //Too complex to deal with either values, have to find a better way
                            return getAllTransaction().thenApply(either -> {
                                if (either.left.isPresent()) {
                                    either.left.get().addAll(transactionList);
                                    return F.Either.Left(either.left.get());
                                } else {
                                    return F.Either.Right(either.right.get());
                                }
                            });
                        }
                    }
                }
                return CompletableFuture.supplyAsync(() -> F.Either.Left(transactionList));
            }
            return CompletableFuture.supplyAsync(() -> F.Either.Right(status(response.getStatus(), jsonResp).as("application/json")));
        });
    }


    private CompletionStage<F.Either<String, Result>> renewBankinAccessToken(String email, String password) {

        WSRequest request = ws.url("https://sync.bankin.com/v2/authenticate")
                .setHeader("Bankin-Version", "2016-01-18")
                .setQueryParameter("password", password)
                .setQueryParameter("email", email)
                .setQueryParameter("client_id", config.getString("bankin.clientId"))
                .setQueryParameter("client_secret", config.getString("bankin.clientSecret"))
                .setRequestTimeout(5000);

        return request.post("").thenApplyAsync(response -> {
            if (response.getStatus() == 200) {
                String at = response.asJson().findPath("access_token").textValue();
                if(at != null){
                    return F.Either.Left(at);
                }
            }
            return F.Either.Right(status(response.getStatus(), response.asJson()));
        });
    }
}
