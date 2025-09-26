package il.cshaifasweng.OCSFMediatorExample.client;

public final class CurrentUser {
    private static boolean owner = false; // set this from your login flow
    private CurrentUser() {}
    public static boolean isOwner() { return owner; }
    public static void setOwner(boolean isOwner) { owner = isOwner; }
}


