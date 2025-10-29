package Request;

import java.io.Serializable;

public class SignupResult implements Serializable {
    public enum Code { OK, USERNAME_TAKEN, ERROR }

    private final Code code;
    private final String message;   // אופציונלי
    private final Integer userId;   // אופציונלי: מזהה משתמש שנוצר

    private SignupResult(Code code, String message, Integer userId) {
        this.code = code;
        this.message = message;
        this.userId = userId;
    }

    // מפעלונים נוחים לשימוש בשרת
    public static SignupResult ok(Integer userId) { return new SignupResult(Code.OK, null, userId); }
    public static SignupResult ok()               { return new SignupResult(Code.OK, null, null); }
    public static SignupResult usernameTaken()    { return new SignupResult(Code.USERNAME_TAKEN, null, null); }
    public static SignupResult error(String msg)  { return new SignupResult(Code.ERROR, msg, null); }
    public static SignupResult error()            { return new SignupResult(Code.ERROR, null, null); }

    // גטרים
    public boolean isOk()             { return code == Code.OK; }
    public boolean isUsernameTaken()  { return code == Code.USERNAME_TAKEN; }
    public String  getMessage()       { return message; }
    public Integer getUserId()        { return userId; }
    public Code    getCode()          { return code; }
}
