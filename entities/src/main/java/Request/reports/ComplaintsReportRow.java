package Request.reports;

import java.util.Date;

public class ComplaintsReportRow {
    private int complaintId;
    private Integer branchId;
    private String branchName;
    private String status;
    private Date date;

    public ComplaintsReportRow() {}
    public ComplaintsReportRow(int complaintId, Integer branchId, String branchName, String status, Date date) {
        this.complaintId = complaintId;
        this.branchId = branchId;
        this.branchName = branchName;
        this.status = status;
        this.date = date;
    }

    public int getComplaintId() { return complaintId; }
    public void setComplaintId(int complaintId) { this.complaintId = complaintId; }
    public Integer getBranchId() { return branchId; }
    public void setBranchId(Integer branchId) { this.branchId = branchId; }
    public String getBranchName() { return branchName; }
    public void setBranchName(String branchName) { this.branchName = branchName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Date getDate() { return date; }
    public void setDate(Date date) { this.date = date; }
}
