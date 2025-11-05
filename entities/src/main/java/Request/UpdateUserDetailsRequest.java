package Request;

import java.io.Serializable;

public class UpdateUserDetailsRequest implements Serializable {
    private final int userId;
    private final String name;
    private final String email;
    private final String idNumber;

    public UpdateUserDetailsRequest(int userId, String name, String email, String idNumber) {
        this.userId = userId; this.name = name; this.email = email; this.idNumber = idNumber;
    }
    public int getUserId() { return userId; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getIdNumber() { return idNumber; }
}