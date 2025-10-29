package il.cshaifasweng.OCSFMediatorExample.client.Events;

import Request.SignupResult;

public class SignupResponseEvent {
    private final boolean ok;
    private final boolean usernameTaken;
    private final String  errorMessage;
    private final Integer userId;

    public SignupResponseEvent(boolean ok, boolean usernameTaken, String errorMessage, Integer userId) {
        this.ok = ok;
        this.usernameTaken = usernameTaken;
        this.errorMessage = errorMessage;
        this.userId = userId;
    }

    public static SignupResponseEvent from(SignupResult r) {
        boolean ok    = r.isOk();
        boolean taken = r.isUsernameTaken();
        String  msg   = r.getMessage();  // יכול להיות null
        Integer id    = r.getUserId();   // יכול להיות null
        return new SignupResponseEvent(ok, taken, msg, id);
    }

    public boolean isOk() { return ok; }
    public boolean isUsernameTaken() { return usernameTaken; }
    public String  getErrorMessage() { return errorMessage; }
    public Integer getUserId() { return userId; }
}
