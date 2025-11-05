package il.cshaifasweng.OCSFMediatorExample.client;

import il.cshaifasweng.OCSFMediatorExample.entities.UserAccount;

public class AppSession {
    private static UserAccount currentUser;
    private static SearchCriteria lastSearchCriteria;

    public static void setCurrentUser(UserAccount user) {
        currentUser = user;
    }

    public static UserAccount getCurrentUser() {
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
