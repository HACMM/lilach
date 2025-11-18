package Request;

import il.cshaifasweng.OCSFMediatorExample.entities.PaymentMethod;
import il.cshaifasweng.OCSFMediatorExample.entities.Role;
import il.cshaifasweng.OCSFMediatorExample.entities.UserBranchType;

import java.io.Serializable;
import java.time.LocalDate;

public class PublicUser implements Serializable {
    private int userId;
    private String login;
    private String name;
    private String email;
    private String idNumber;
    private Role role;

    private UserBranchType branchType;

    private Integer branchId;          // nullable
    private String  branchName;        // optional, nullable

    private boolean   subscriptionUser;
    private LocalDate subscriptionExpirationDate;
    private PaymentMethod defaultPaymentMethod;

    public PublicUser(int userId,
                      String login,
                      String name,
                      String email,
                      String idNumber,
                      Role role,
                      UserBranchType branchType,
                      Integer branchId,
                      boolean subscriptionUser,
                      LocalDate subscriptionExpirationDate,
                      PaymentMethod defaultPaymentMethod) {
        this.userId = userId;
        this.login = login;
        this.name = name;
        this.email = email;
        this.idNumber = idNumber;
        this.role = role;
        this.branchType = branchType;
        this.branchId = branchId;
        this.subscriptionUser = subscriptionUser;
        this.subscriptionExpirationDate = subscriptionExpirationDate;
        this.defaultPaymentMethod = defaultPaymentMethod;
    }


    // Getters
    public int getUserId() { return userId; }
    public String getLogin() { return login; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getIdNumber() { return idNumber; }
    public Role getRole() { return role; }
    public UserBranchType getBranchType() { return branchType; }
    public Integer getBranchId() { return branchId; }     // <— use this instead of getBranch()
    public String getBranchName() { return branchName; }
    public boolean isSubscriptionUser() { return subscriptionUser; }
    public LocalDate getSubscriptionExpirationDate() { return subscriptionExpirationDate; }
    public PaymentMethod getDefaultPaymentMethod() { return defaultPaymentMethod; }

    // Setters
    public void setUserId(int userId) { this.userId = userId; }
    public void setLogin(String login) { this.login = login; }
    public void setName(String name) { this.name = name; }
    public void setEmail(String email) { this.email = email; }
    public void setIdNumber(String idNumber) { this.idNumber = idNumber; }
    public void setRole(Role role) { this.role = role; }
    public void setBranchType(UserBranchType branchType) { this.branchType = branchType; }
    public void setBranchId(Integer branchId) { this.branchId = branchId; }
    public void setBranchName(String branchName) { this.branchName = branchName; }
    public void setSubscriptionUser(boolean subscriptionUser) { this.subscriptionUser = subscriptionUser; }
    public void setSubscriptionExpirationDate(LocalDate subscriptionExpirationDate) { this.subscriptionExpirationDate = subscriptionExpirationDate; }
    public void setDefaultPaymentMethod(PaymentMethod defaultPaymentMethod) { this.defaultPaymentMethod = defaultPaymentMethod; }



    public boolean isNetworkUser() {
        return branchType == UserBranchType.ALL_BRANCHES
                || branchType == UserBranchType.SUBSCRIPTION;
    }
}
