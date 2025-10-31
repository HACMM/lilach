package Request.reports;

import java.util.Date;

public class OrderReportRow {
    private int orderId;
    private Integer branchId;
    private String branchName;
    private Date date;           // Order date field
    private double amount;       // total order amount

    public OrderReportRow() {}
    public OrderReportRow(int orderId, Integer branchId, String branchName, Date date, double amount) {
        this.orderId = orderId;
        this.branchId = branchId;
        this.branchName = branchName;
        this.date = date;
        this.amount = amount;
    }

    public int getOrderId() { return orderId; }
    public void setOrderId(int orderId) { this.orderId = orderId; }
    public Integer getBranchId() { return branchId; }
    public void setBranchId(Integer branchId) { this.branchId = branchId; }
    public String getBranchName() { return branchName; }
    public void setBranchName(String branchName) { this.branchName = branchName; }
    public Date getDate() { return date; }
    public void setDate(Date date) { this.date = date; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
}
