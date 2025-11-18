package Request;

import il.cshaifasweng.OCSFMediatorExample.entities.PaymentMethod;
import il.cshaifasweng.OCSFMediatorExample.entities.UserBranchType;

import java.io.Serializable;

public class SignupRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String username;
    private String password;
    private String name;
    private String email;
    private String idNumber;
    private PaymentMethod paymentMethod;
    private UserBranchType branchType;
    private Integer branchId;

    public SignupRequest() {
    }

    public SignupRequest(String username, String password, String name, String email,String idNumber, PaymentMethod payment, UserBranchType branchType
            , Integer branchId) {
        this.username = username;
        this.password = password;
        this.name = name;
        this.email = email;
        this.idNumber = idNumber;
        this.paymentMethod = payment;
        this.branchType = branchType;
        this.branchId = branchId;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getIdNumber() {return idNumber;}

    public PaymentMethod getPayment() {
        return paymentMethod;
    }

    public UserBranchType getBranchType() {
        return branchType;
    }

    public Integer getBranchId() { return branchId; }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setIdNumber(String idNumber) { this.idNumber = idNumber; }

    public void setPayment(PaymentMethod payment) {
        this.paymentMethod = payment;
    }

    public void setBranchType(UserBranchType branchType) {
        this.branchType = branchType;
    }

    public void setBranchId(Integer branchId) { this.branchId = branchId; }

    @Override
    public String toString() {
        return "SignupRequest{username='" + username + "', name='" + name + "', email='" + email + "', payment='" + paymentMethod
                + "', branchType='" + branchType + "', branchId='" + branchId + "'}";
    }
}