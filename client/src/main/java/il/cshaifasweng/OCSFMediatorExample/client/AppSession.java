package il.cshaifasweng.OCSFMediatorExample.client;

import Request.PublicUser;
import il.cshaifasweng.OCSFMediatorExample.entities.UserAccount;

public class AppSession {
    private static PublicUser currentUser;
    private static SearchCriteria lastSearchCriteria;

    public static void setCurrentUser(PublicUser user) {
        currentUser = user;
    }

    public static PublicUser getCurrentUser() {
        return currentUser;
    }

    public static SearchCriteria getLastSearchCriteria() {
        return lastSearchCriteria;
    }

    public static void setLastSearchCriteria(SearchCriteria criteria) {
        lastSearchCriteria = criteria;
    }

    public static void clear() {
        currentUser = null;
    }
}