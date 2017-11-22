package v1.post;

import play.Logger;
import play.libs.concurrent.Futures;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Results;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static play.mvc.Http.Status.*;

public class PostAction extends play.mvc.Action.Simple {
    private final Logger.ALogger logger = play.Logger.of("application.PostAction");

    private final HttpExecutionContext ec;
    private final Futures futures;

    @Singleton
    @Inject
    public PostAction(HttpExecutionContext ec, Futures futures) {
        this.ec = ec;
        this.futures = futures;
    }

    public CompletionStage<Result> call(Http.Context ctx) {
        if (logger.isTraceEnabled()) {
            logger.trace("call: ctx = " + ctx);
        }

/*
        if (ctx.request().accepts("application/json")) {
            return futures.timeout(doCall(ctx), 1L, TimeUnit.SECONDS).exceptionally(e -> {
                return (Results.status(GATEWAY_TIMEOUT, views.html.timeout.render()));
            }).whenComplete((r, e) -> System.out.println("toto"));
        } else {
        */
            return completedFuture(
                    status(NOT_ACCEPTABLE, "We only accept application/json")
            );
        //}
    }

    private CompletionStage<Result> doCall(Http.Context ctx) {
        return delegate.call(ctx).handleAsync((result, e) -> {
            if (e != null) {
                if (e instanceof CompletionException) {
                    Throwable completionException = e.getCause();

                        logger.error("Direct exception " + e.getMessage(), e);
                        return internalServerError();

                } else {
                    logger.error("Unknown exception " + e.getMessage(), e);
                    return internalServerError();
                }
            } else {
                return result;
            }
        }, ec.current());
    }
}
