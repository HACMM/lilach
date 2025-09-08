package il.cshaifasweng.OCSFMediatorExample.client;

import il.cshaifasweng.OCSFMediatorExample.entities.User;

public class LoginResponseEvent {
    private final boolean success;
    private final User user; // המשתמש המחובר, אם הצליח

    public LoginResponseEvent(boolean success, User user) {
        this.success = success;
        this.user = user;
    }

    public boolean isSuccess() {
        return success;
    }

    public User getUser() {
        return user;
    }
}
