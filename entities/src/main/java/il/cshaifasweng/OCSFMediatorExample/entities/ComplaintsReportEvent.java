package il.cshaifasweng.OCSFMediatorExample.entities;

import il.cshaifasweng.OCSFMediatorExample.entities.Complaint;
import java.util.List;

public class ComplaintsReportEvent {
    private final List<Complaint> complaints;

    public ComplaintsReportEvent(List<Complaint> complaints) {
        this.complaints = complaints;
    }

    public List<Complaint> getComplaints() {
        return complaints;
    }
}
