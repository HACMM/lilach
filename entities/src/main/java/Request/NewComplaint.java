package Request;
import java.io.Serializable;

public class NewComplaint implements Serializable {
    public Integer branchId;
    public String orderNumber;
    public String clientName;
    public String clientEmail;
    public String description;

    public NewComplaint() {}
    public NewComplaint(Integer branchId, String orderNumber, String clientName,
                        String clientEmail, String description) {
        this.branchId = branchId;
        this.orderNumber = orderNumber;
        this.clientName = clientName;
        this.clientEmail = clientEmail;
        this.description = description;
    }
}
