package il.cshaifasweng.OCSFMediatorExample.entities;

import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
public class PaymentMethod
{
    @Column(name = "accountNumber")
    private String accountNumber;

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        // TODO: check for validity
        this.accountNumber = accountNumber;
    }
    // TODO: add relevant fields
}
