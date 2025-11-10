package il.cshaifasweng.OCSFMediatorExample.client.Events;

import Request.PublicUser;

public class LoginResponseEvent {
    private final boolean success;
    private final String message;
    private final PublicUser user;

    public LoginResponseEvent(boolean success, String message, PublicUser user) {
        this.success = success;
        this.message = message;
        this.user = user;
    }

    public boolean isSuccess() {
        return success;
    }
    public String getMessage() { return message;}
    public PublicUser getUser() {
        return user;
    }
}
