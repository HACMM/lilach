package il.cshaifasweng.OCSFMediatorExample.server.EntityManagers;

import Request.reports.ComplaintsReportRow;
import Request.reports.ReportRequest;
import Request.reports.ReportScope;
import il.cshaifasweng.OCSFMediatorExample.entities.Branch;
import il.cshaifasweng.OCSFMediatorExample.entities.Complaint;
import il.cshaifasweng.OCSFMediatorExample.entities.ComplaintEvent;
import il.cshaifasweng.OCSFMediatorExample.entities.ComplaintStatus;
import il.cshaifasweng.OCSFMediatorExample.entities.Order;
import org.hibernate.Hibernate;
import org.hibernate.SessionFactory;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

public class ReportManager extends BaseManager {
    public ReportManager(SessionFactory sf) { super(sf); }

    /** Build complaints report rows from existing Complaint/ComplaintEvent entities. */
    public List<ComplaintsReportRow> complaintsReport(ReportRequest req) {
        Objects.requireNonNull(req.getFrom(), "from is null");
        Objects.requireNonNull(req.getTo(), "to is null");

        // 1) Fetch complaints in window (and by branch if requested)
        List<Complaint> complaints = read(s -> {
            String base = "select c from Complaint c " +
                    "left join fetch c.branch b " +        // fetch branch for name/id access
                    "where c.createdAt between :f and :t";
            if (req.getScope() == ReportScope.BRANCH && req.getBranchId() != null) {
                base += " and b.id = :b";
            }
            var q = s.createQuery(base, Complaint.class)
                    .setParameter("f", toLocalDateTime(req.getFrom()))
                    .setParameter("t", toLocalDateTime(req.getTo()));
            if (req.getScope() == ReportScope.BRANCH && req.getBranchId() != null) {
                q.setParameter("b", req.getBranchId());
            }
            List<Complaint> list = q.getResultList();

            // Initialize complaintHistory lazily for each complaint (we need latest status)
            for (Complaint c : list) {
                Hibernate.initialize(c.getComplaintHistory());
            }
            return list;
        });

        // 2) Map to DTO rows
        List<ComplaintsReportRow> rows = new ArrayList<>(complaints.size());
        for (Complaint c : complaints) {
            Branch b = c.getBranch();
            Integer branchId = (b != null) ? getBranchIdSafe(b) : null;
            String branchName = (b != null) ? getBranchNameSafe(b) : null;

            // Pick latest event by max id (since ComplaintEvent has no timestamp)
            ComplaintStatus latestStatus = null;
            if (c.getComplaintHistory() != null && !c.getComplaintHistory().isEmpty()) {
                ComplaintEvent latest = c.getComplaintHistory()
                        .stream()
                        .max(Comparator.comparingInt(this::eventIdSafe))
                        .orElse(null);
                if (latest != null) {
                    latestStatus = latestStatusSafe(latest);
                }
            }

            // Fallback if no events
            String statusStr = (latestStatus != null) ? latestStatus.name() : "Filed";

            rows.add(new ComplaintsReportRow(
                    c.getComplaintId(),
                    branchId,
                    branchName,
                    statusStr,
                    toDate(c.getCreatedAt())
            ));
        }
        return rows;
    }

    /** Return raw complaints list in range, optionally filtered by branch. */
    public List<Complaint> complaintsInRange(Date from, Date to, ReportScope scope, Integer branchId) {
        Objects.requireNonNull(from, "from is null");
        Objects.requireNonNull(to, "to is null");
        return read(s -> {
            String hql = "select c from Complaint c left join fetch c.branch b " +
                    "where c.createdAt between :f and :t";
            if (scope == ReportScope.BRANCH && branchId != null) {
                hql += " and b.id = :b";
            }
            var q = s.createQuery(hql, Complaint.class)
                    .setParameter("f", toLocalDateTime(from))
                    .setParameter("t", toLocalDateTime(to));
            if (scope == ReportScope.BRANCH && branchId != null) {
                q.setParameter("b", branchId);
            }
            List<Complaint> list = q.getResultList();
            // initialize history lazily for potential client use
            for (Complaint c : list) {
                Hibernate.initialize(c.getComplaintHistory());
            }
            return list;
        });
    }

    /** Orders (with lines and items) in range, optionally by branch - used for Orders and Revenue reports. */
    public List<Order> ordersInRange(Date from, Date to, ReportScope scope, Integer branchId) {
        Objects.requireNonNull(from, "from is null");
        Objects.requireNonNull(to, "to is null");
        return read(s -> {
            String hql = "select distinct o from Order o " +
                    "left join fetch o.orderLines ol " +
                    "left join fetch ol.item it " +
                    "left join fetch o.branch b " +
                    "where o.createdAt between :f and :t";
            if (scope == ReportScope.BRANCH && branchId != null) {
                hql += " and b.id = :b";
            }
            var q = s.createQuery(hql, Order.class)
                    .setParameter("f", toLocalDateTime(from))
                    .setParameter("t", toLocalDateTime(to));
            if (scope == ReportScope.BRANCH && branchId != null) {
                q.setParameter("b", branchId);
            }
            return q.getResultList();
        });
    }

    /* ================= helpers ================= */

    private static LocalDateTime toLocalDateTime(Date d) {
        // ReportRequest uses java.util.Date; Complaint uses LocalDateTime
        return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    private static Date toDate(LocalDateTime ldt) {
        return (ldt == null) ? null : Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant());
    }

    // Defensive accessors in case Branch getters differ
    private Integer getBranchIdSafe(Branch b) {
        try {
            return (Integer) Branch.class.getMethod("getId").invoke(b);
        } catch (Exception e) {
            return null;
        }
    }

    private String getBranchNameSafe(Branch b) {
        try {
            Object v = Branch.class.getMethod("getName").invoke(b);
            return (v == null) ? null : String.valueOf(v);
        } catch (Exception e) {
            return null;
        }
    }

    private int eventIdSafe(ComplaintEvent ce) {
        try {
            return (int) ComplaintEvent.class.getDeclaredField("id").getInt(ce);
        } catch (Exception e) {
            // fall back to 0 if reflection fails
            return 0;
        }
    }

    private ComplaintStatus latestStatusSafe(ComplaintEvent ce) {
        try {
            return (ComplaintStatus) ComplaintEvent.class.getMethod("getStatus").invoke(ce);
        } catch (Exception e) {
            return null;
        }
    }
}


// TODO: define the types of reports we want to create


    // TODO: add method RevenueReport(from, to, branch/network, type of item/all items)

    // TODO: add OrderReport(from, to, branch/network, type of item/all items)

    // TODO: add ComplaintsReport(from, to, branch/network, type of item/all items)

