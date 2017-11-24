package mappings;


public class Transaction {

    private int id;
    private String date;
    private float amount;
    private float rounded_amount;

    public Transaction() {
    }

    public Transaction(int id, String date, float amount, float rounded_amount) {
        this.id = id;
        this.date = date;
        this.amount = amount;
        this.rounded_amount = rounded_amount;
    }

    public int getId() {
        return id;
    }

    public String getDate() {
        return date;
    }

    public float getAmount() {
        return amount;
    }

    public float getRounded_amount() {
        return rounded_amount;
    }
}


