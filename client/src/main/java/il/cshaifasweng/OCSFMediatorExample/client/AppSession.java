package il.cshaifasweng.OCSFMediatorExample.client;

import Request.PublicUser;

public class AppSession {
    private static UserAccount currentUser;
    private static SearchCriteria lastSearchCriteria;

    public static void setCurrentUser(PublicUser user) {
        currentUser = user;
    }

    public static PublicUser getCurrentUser() {
        return currentUser;
    }

    public static void clear() {
        currentUser = null;
    }

    public static void setLastSearchCriteria(SearchCriteria criteria) {
        lastSearchCriteria = criteria;
    }

    public static SearchCriteria getLastSearchCriteria() {
        return lastSearchCriteria;
    }
}
