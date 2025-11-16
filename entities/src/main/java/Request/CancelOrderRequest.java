package Request;

import java.io.Serializable;

//a
public class CancelOrderRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private int userId;
    private int orderId;

    public CancelOrderRequest() {}

    public CancelOrderRequest(int userId, int orderId) {
        this.userId = userId;
        this.orderId = orderId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }
}

