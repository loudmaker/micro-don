package controllers;

import enums.RoundType;
import mappings.Transaction;
import play.mvc.Controller;
import play.mvc.Result;
import javax.inject.Inject;
import play.Configuration;
import java.lang.String;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import play.libs.ws.*;
import play.libs.Json;
import providers.BankinProvider;
import mappings.User;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * This controller contains an action to handle HTTP requests
 * to the application's home page.
 */
public class HomeController extends Controller {

    @Inject Configuration config;
    @Inject WSClient ws;


    public CompletionStage<Result> roundedTransactions(String email, String password, String roundType) {

        System.out.println("NEW BANKIN PROVIDER FOR " +email + " " +password);
        //Few checks before processing
        RoundType roundTypeEnum = RoundType.exists(roundType);
        if(roundTypeEnum == null){
            return CompletableFuture.supplyAsync(() ->
                badRequest("bad enum type '" + roundType + "', please provide enum as : " + RoundType.list.stream().map(Object::toString).collect(Collectors.joining(", ")))
            );
        }

        BankinProvider bp = new BankinProvider(config, ws, email, password);
        return bp.getAllTransaction().thenApplyAsync( either -> {

            //Array list transactions
            if(either.left.isPresent()) {

                return ok(Json.toJson(either.left.get().stream().map(x -> {

                    //Processing the rounded amount
                    x.setRounded_amount(-x.getAmount() - roundTypeEnum.round(-x.getAmount()));
                    return x;
                }).collect(Collectors.toList())));
            //Or errors
            } else {
                return either.right.get();
            }
        });
    }


    public CompletionStage<Result> roundedTransactionsAllUser(Long dateBegin, Long dateEnd) {

        ArrayList<User> users = new ArrayList<User>();
        users.add(new User("user1@mail.com", "a!Strongp#assword1", RoundType.UPPER_TEN_DECIMAL));
        users.add(new User("user2@mail.com", "a!Strongp#assword2", RoundType.UPPER_TEN_DECIMAL));
        users.add(new User("user3@mail.com", "a!Strongp#assword3", RoundType.UPPER_TEN_DECIMAL));


        List<CompletableFuture<WSResponse>> collect = users.stream().map(x -> {

            WSRequest request = ws.url("http://localhost:9000/rounded-transactions")
                    .setQueryParameter("email", x.getEmail())
                    .setQueryParameter("passwd", x.getPassword())
                    .setQueryParameter("roundType", x.getRoundType().toString())
                    .setRequestTimeout(10000);
            return request.get().toCompletableFuture();

        }).collect(Collectors.toList());


        CompletableFuture<List<WSResponse>> allDone = sequence(collect);
        CompletableFuture<List<Transaction>> transactions = allDone.thenApply(relevances -> relevances.stream().map(wsResp -> {


             //System.out.println("add transaction to array " + afterParam);
             ArrayList<Transaction> transactionList = new ArrayList<Transaction>();

            System.out.println("have transaction !");
            System.out.println(wsResp.getBody());
             if (wsResp.getStatus() == 200) {

                 ArrayNode arrayRes = (ArrayNode) wsResp.asJson();
                 Iterator<JsonNode> it = arrayRes.iterator();
                 while (it.hasNext()) {
                    Transaction resource = Json.fromJson(it.next(), Transaction.class);
                    transactionList.add(resource);
                 }
             }

             return transactionList;

        }).flatMap( x -> x.stream()).collect(Collectors.toList()));

        //Aggregate all rounded amounts
        return transactions.thenApply(t -> {

            ObjectNode result = Json.newObject();
            result.put(
                "totalRoundedAmount",
                t.stream().map(x -> x.getRounded_amount()).reduce(0.0f, (i, j) -> i + j)
            );

            return ok(result);
        });
    }

    private static <T> CompletableFuture<List<T>> sequence(List<CompletableFuture<T>> futures) {
        CompletableFuture<Void> allDoneFuture =
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]));
        return allDoneFuture.thenApply(v ->
            futures.stream().
                map(future -> future.join()).
                collect(Collectors.<T>toList())
        );
    }
}
