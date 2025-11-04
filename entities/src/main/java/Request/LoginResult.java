package Request;

import il.cshaifasweng.OCSFMediatorExample.entities.UserAccount;

import java.io.Serializable;

public class LoginResult implements Serializable {
    public enum Status { USER_FOUND, USER_NOT_FOUND, ERROR }

    private Status status;
    private String message;
    private PublicUser user;  // << add this

    public LoginResult() {}

    public LoginResult(Status status, String message) {
        this.status = status; this.message = message;
    }

    public LoginResult(Status status, String message, PublicUser user) {
        this.status = status; this.message = message; this.user = user;
    }

    public static LoginResult ok(PublicUser u) {
        return new LoginResult(Status.USER_FOUND, "OK", u);
    }
    public static LoginResult notFound(String msg) {
        return new LoginResult(Status.USER_NOT_FOUND, msg, null);
    }
    public static LoginResult error(String msg) {
        return new LoginResult(Status.ERROR, msg, null);
    }

    public Status getStatus() { return status; }
    public String getMessage() { return message; }
    public PublicUser getUser() { return user; }
    public boolean isSuccess() { return status == Status.USER_FOUND; }
}
