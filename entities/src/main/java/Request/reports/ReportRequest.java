package Request.reports;

import java.util.Date;

public class ReportRequest {
    private Date from;
    private Date to;
    private ReportScope scope = ReportScope.NETWORK;
    private Integer branchId;            // used when scope == BRANCH
    private ItemGrouping grouping = ItemGrouping.ALL;
    private String itemType;             // optional filter (e.g., "Flower")
    private Integer itemId;              // optional filter

    public ReportRequest() {}

    public ReportRequest(Date from, Date to, ReportScope scope, Integer branchId,
                         ItemGrouping grouping, String itemType, Integer itemId) {
        this.from = from;
        this.to = to;
        this.scope = scope;
        this.branchId = branchId;
        this.grouping = grouping;
        this.itemType = itemType;
        this.itemId = itemId;
    }

    public Date getFrom() { return from; }
    public void setFrom(Date from) { this.from = from; }
    public Date getTo() { return to; }
    public void setTo(Date to) { this.to = to; }
    public ReportScope getScope() { return scope; }
    public void setScope(ReportScope scope) { this.scope = scope; }
    public Integer getBranchId() { return branchId; }
    public void setBranchId(Integer branchId) { this.branchId = branchId; }
    public ItemGrouping getGrouping() { return grouping; }
    public void setGrouping(ItemGrouping grouping) { this.grouping = grouping; }
    public String getItemType() { return itemType; }
    public void setItemType(String itemType) { this.itemType = itemType; }
    public Integer getItemId() { return itemId; }
    public void setItemId(Integer itemId) { this.itemId = itemId; }
}
