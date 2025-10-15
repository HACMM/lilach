package Request;

import java.io.Serializable;

public class SignupResult implements Serializable {
    public enum Status { OK, USERNAME_TAKEN, ERROR }

    private Status status;

    public SignupResult() {}           // לסריאליזציה
    private SignupResult(Status status) { this.status = status; }

    public Status getStatus() { return status; }
    public boolean isSuccess() { return status == Status.OK; }

    public static SignupResult ok()            { return new SignupResult(Status.OK); }
    public static SignupResult usernameTaken() { return new SignupResult(Status.USERNAME_TAKEN); }
    public static SignupResult error()         { return new SignupResult(Status.ERROR); }

    @Override public String toString() { return "SignupResult{status=" + status + "}"; }
}
