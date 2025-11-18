package il.cshaifasweng.OCSFMediatorExample.client;

import Request.PublicUser;
import il.cshaifasweng.OCSFMediatorExample.entities.Branch;
import il.cshaifasweng.OCSFMediatorExample.entities.Sale;
import il.cshaifasweng.OCSFMediatorExample.entities.UserAccount;

public class AppSession {
    private static PublicUser currentUser;
    private static SearchCriteria lastSearchCriteria;
    private static Branch currentBranch;

    private static Sale selectedSale;

    public static void setCurrentUser(PublicUser user) {
        currentUser = user;
    }
    public static void setLastSearchCriteria(SearchCriteria criteria) {
        lastSearchCriteria = criteria;
    }
    public static void setCurrentBranch(Branch branch) { currentBranch = branch;}


    public static PublicUser getCurrentUser() {
        return currentUser;
    }
    public static SearchCriteria getLastSearchCriteria() {
        return lastSearchCriteria;
    }
    public static Branch getCurrentBranch() { return currentBranch;}
    public static Integer getActiveBranchId(){
        if (currentBranch != null) {
            return currentBranch.getId();
        }
        if (currentUser != null) {
            return currentUser.getBranchId();
        }
        return null;
    }

    public static void clear() {
        currentUser = null;
        lastSearchCriteria = null;
        currentBranch = null;
    }

    public static void setSelectedSale(Sale selected) {
        selectedSale = selected;
    }
}