package mappings;

import enums.RoundType;


public class User {

    private String email;
    private String password;
    private RoundType roundType;

    public User(String email, String password, RoundType roundType) {

        this.email = email;
        this.password = password;
        this.roundType = roundType;
    }

    public String getEmail() {
        return this.email;
    }
    public String getPassword() {
        return this.password;
    }
    public RoundType getRoundType() {
        return this.roundType;
    }
}
