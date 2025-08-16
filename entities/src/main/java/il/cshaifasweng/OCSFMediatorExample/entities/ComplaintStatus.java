package il.cshaifasweng.OCSFMediatorExample.entities;

public enum ComplaintStatus {
    Filed,              // Complaint is filed by a customer and is waiting for review
    UnderReview,        // Complaint is under review
    WaitingForResponse, // Additional information from a customer is required
    Rejected,           // Complaint is rejected
    Satisfied           // Complaint is satisfied
}

