package il.cshaifasweng.OCSFMediatorExample.client;

import Request.PublicUser;
import il.cshaifasweng.OCSFMediatorExample.entities.*;

import java.util.List;

public class AppSession {
    private static PublicUser currentUser;
    private static SearchCriteria lastSearchCriteria;
    private static Branch currentBranch;
    private static List<Category> categories;
    private static Sale selectedSale;
    private static Integer lastSelectedCategory;

    public static void setLastSelectedCategory(int id) {
        lastSelectedCategory = id;
    }

    public static Integer getLastSelectedCategory() {
        return lastSelectedCategory;
    }


    public static void setCurrentUser(PublicUser user) {
        currentUser = user;
    }
    public static void setLastSearchCriteria(SearchCriteria criteria) {
        lastSearchCriteria = criteria;
    }
    public static void setCurrentBranch(Branch branch) { currentBranch = branch;}
    private static List<Item> lastItemList;
    private static boolean cameFromCategory = false;

    public static void setLastItemList(List<Item> items) {
        lastItemList = items;
    }

    public static List<Item> getLastItemList() {
        return lastItemList;
    }


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
        categories = null;
        selectedSale = null;
        lastSelectedCategory = null;
    }

    public static void setSelectedSale(Sale selected) {
        selectedSale = selected;
    }

    public static void setCategories(List<Category> catList) {
        categories = catList;
    }

    public static List<Category> getCategories() {
        return categories;
    }

    public static boolean isCameFromCategory() {
        return cameFromCategory;
    }

    public static void setCameFromCategory(boolean value) {
        cameFromCategory = value;
    }

}