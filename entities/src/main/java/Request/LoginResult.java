package Request;

import java.io.Serializable;

public class LoginResult implements Serializable {
    public enum Status { USER_FOUND, USER_NOT_FOUND, ERROR }

    private Status status;
    private String message;

    public LoginResult() { }

    public LoginResult(Status status, String message) {
        this.status = status;
        this.message = message;
    }

    public Status getStatus() { return status; }
    public String getMessage() { return message; }

    public boolean isSuccess() {
        return status == Status.USER_FOUND;
    }
}
