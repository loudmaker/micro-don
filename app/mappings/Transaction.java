package mappings;


public class Transaction {

    private long id;
    private String date;
    private float amount;
    private float rounded_amount;

    public Transaction() {
    }

    public Transaction(long id, String date, float amount, float rounded_amount) {
        this.id = id;
        this.date = date;
        this.amount = amount;
        this.rounded_amount = rounded_amount;
    }

    public long getId() {
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

    public void setId(long id) {
        this.id = id;
    }
    public void setDate(String date) {
        this.date = date;
    }
    public void setAmount(float amount) {
        this.amount = amount;
    }
    public void setRounded_amount(float rounded_amount) {
        this.rounded_amount = rounded_amount;
    }
}


