package controllers;

import enums.RoundType;
import mappings.Transaction;
import play.mvc.Controller;
import play.mvc.Result;
import javax.inject.Inject;
import play.Configuration;
import java.lang.String;
import java.sql.Timestamp;
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
import utils.Transformation;
import utils.Url;

/**
 * This controller contains an action to handle HTTP requests
 * to the application's home page.
 */
public class HomeController extends Controller {

    @Inject Configuration config;
    @Inject WSClient ws;


    public CompletionStage<Result> roundedTransactions(String email, String password, String roundType) {

        //Few checks before retrieving all user transaction
        RoundType roundTypeEnum = RoundType.exists(roundType);
        if(roundTypeEnum == null){
            return CompletableFuture.supplyAsync(() ->
                badRequest("bad enum type '" + roundType + "', please provide enum as : " +
                        RoundType.list.stream().map(Object::toString).collect(Collectors.joining(", ")))
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
            }
            //Or errors
            return either.right.get();
        });
    }


    public CompletionStage<Result> roundedTransactionsAllUser(Long dateBegin, Long dateEnd) {

        ArrayList<User> users = new ArrayList<User>();
        users.add(new User("user1@mail.com", "a!Strongp#assword1", RoundType.UPPER_TEN_DECIMAL));
        users.add(new User("user2@mail.com", "a!Strongp#assword2", RoundType.UPPER_TEN_DECIMAL));
        users.add(new User("user3@mail.com", "a!Strongp#assword3", RoundType.UPPER_TEN_DECIMAL));


        //Map over users to create a list of completable future
        List<CompletableFuture<WSResponse>> collect = users.stream().map(x -> {

            //I did better not calling my own api to retrieve transactions, there is an easier way..
            WSRequest request = ws.url(Url.getCurrentHostName() + "/rounded-transactions")
                .setQueryParameter("email",     x.getEmail())
                .setQueryParameter("passwd",    x.getPassword())
                .setQueryParameter("roundType", x.getRoundType().toString())
                .setRequestTimeout(30000);
            return request.get().toCompletableFuture();

        }).collect(Collectors.toList());


        //Then have a completable future of transaction list
        CompletableFuture<List<WSResponse>> allDone = Transformation.sequence(collect);
        CompletableFuture<List<Transaction>> transactions =
                allDone.thenApply(relevances -> relevances.stream().map(wsResp -> {

            ArrayList<Transaction> transactionList = new ArrayList<Transaction>();


            //Having 200 request means having no errors so transaction array retrievement
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


        //Aggregate all rounded amounts of all my transactions
        return transactions.thenApply(t -> {

            ObjectNode result = Json.newObject();
            result.put(
                "totalRoundedAmount",
                t.stream()

                 //I did better avoid retrieve all transaction pages and stop it
                 // when transactions are not on the date range
                 .filter(x -> {
                     long millisecTsDate = Timestamp.valueOf(x.getDate() + " 00:00:00").getTime();
                     //System.out.println(dateBegin + " " + millisecTsDate + " " + dateEnd);
                     return (dateBegin <= millisecTsDate && dateEnd >= millisecTsDate);
                 })                                 //filter transactions between dates given on params
                 .map(x -> x.getRounded_amount())   //Retrieve only rounded amounts
                 .reduce(0.0f, (i, j) -> i + j)     //Sum all rounded amounts
            );

            return ok(result);
        });
    }
}
