package Request.reports;

public class RevenueReportRow {
    /** The grouping key. For ALL = "ALL"; for BY_TYPE = type value; for BY_ITEM = item name or id. */
    private String key;
    private double revenue;

    public RevenueReportRow() {}
    public RevenueReportRow(String key, double revenue) {
        this.key = key;
        this.revenue = revenue;
    }

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    public double getRevenue() { return revenue; }
    public void setRevenue(double revenue) { this.revenue = revenue; }
}
