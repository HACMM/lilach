package il.cshaifasweng.OCSFMediatorExample.entities;

public enum SaleStatus {
    Stashed,    // The sale is not started yet or already has finished, no advertisement and no influence on the cost
                // of the order
    Announced,  // The sale is not started yet but its advertisement is visible
    Ongoing     // The sale has started, its advertisement is visible, the sale is taken into account when calculating
                // the cost of the order
}
