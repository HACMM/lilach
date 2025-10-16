package il.cshaifasweng.OCSFMediatorExample.client.Events;
import il.cshaifasweng.OCSFMediatorExample.entities.Branch;
import java.util.List;

public class BranchListEvent {
    private final List<Branch> branches;

    public BranchListEvent(List<Branch> branches) {
        this.branches = branches;
    }

    public List<Branch> getBranches() {
        return branches;
    }
}
