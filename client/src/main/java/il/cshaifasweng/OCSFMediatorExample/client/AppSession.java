package il.cshaifasweng.OCSFMediatorExample.client;

import Request.PublicUser;

public class AppSession {
    private static PublicUser currentUser;

    public static void setCurrentUser(PublicUser user) {
        currentUser = user;
    }

    public static PublicUser getCurrentUser() {
        return currentUser;
    }

    public static void clear() {
        currentUser = null;
    }
}
