package il.cshaifasweng.OCSFMediatorExample.entities;

import il.cshaifasweng.OCSFMediatorExample.entities.Complaint;
import java.io.Serializable;
import java.util.List;

public class ComplaintsReportEvent implements Serializable {
    private static final long serialVersionUID = 1L;
    private final List<Complaint> complaints;

    public ComplaintsReportEvent(List<Complaint> complaints) {
        this.complaints = complaints;
    }

    public List<Complaint> getComplaints() {
        return complaints;
    }
}
