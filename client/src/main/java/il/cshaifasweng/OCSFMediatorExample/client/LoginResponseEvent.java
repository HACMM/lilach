package il.cshaifasweng.OCSFMediatorExample.client;

import il.cshaifasweng.OCSFMediatorExample.entities.UserAccount;

public class LoginResponseEvent {
    private final boolean success;
    private final UserAccount user; // המשתמש המחובר, אם הצליח

    public LoginResponseEvent(boolean success, UserAccount user) {
        this.success = success;
        this.user = user;
    }

    public boolean isSuccess() {
        return success;
    }

    public UserAccount getUser() {
        return user;
    }
}
