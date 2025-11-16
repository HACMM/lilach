package Request;

import il.cshaifasweng.OCSFMediatorExample.entities.PaymentMethod;
import java.io.Serializable;

//a
public class PurchaseSubscriptionRequest implements Serializable {
    private final int userId;
    private final PaymentMethod paymentMethod;

    public PurchaseSubscriptionRequest(int userId, PaymentMethod paymentMethod) {
        this.userId = userId;
        this.paymentMethod = paymentMethod;
    }

    public int getUserId() {
        return userId;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }
}

