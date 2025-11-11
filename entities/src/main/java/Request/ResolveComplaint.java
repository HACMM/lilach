package Request;
import java.io.Serializable;

public class ResolveComplaint implements Serializable {
    public int complaintId;
    public String response;
    public Double compensation;
    public Integer managerUserId;

    public ResolveComplaint() {}
    public ResolveComplaint(int complaintId, String response, Double compensation) {
        this.complaintId = complaintId;
        this.response = response;
        this.compensation = compensation;
    }
    public ResolveComplaint(int complaintId, String response, Double compensation, Integer managerUserId) {
        this.complaintId = complaintId;
        this.response = response;
        this.compensation = compensation;
        this.managerUserId = managerUserId;
    }
}
