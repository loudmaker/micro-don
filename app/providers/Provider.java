package providers;

import mappings.Transaction;
import play.libs.F;
import play.mvc.Result;
import java.util.ArrayList;
import java.util.concurrent.CompletionStage;


public interface Provider {

    // Did better give an abstract Credential object instead of string
    public CompletionStage<F.Either<ArrayList<Transaction>, Result>> getAllTransaction();
}
