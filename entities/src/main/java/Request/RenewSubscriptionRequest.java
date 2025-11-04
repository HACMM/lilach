package Request;

import java.io.Serializable;

public class RenewSubscriptionRequest implements Serializable {
    private final int userId;
    public RenewSubscriptionRequest(int userId) { this.userId = userId; }
    public int getUserId() { return userId; }
}