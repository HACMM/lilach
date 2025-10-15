package Request;
import java.io.Serializable;

public class SignupRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String username;
    private String password;
    private String name;
    private String email;

    public SignupRequest() { }

    public SignupRequest(String username, String password, String name, String email) {
        this.username = username;
        this.password = password;
        this.name = name;
        this.email = email;
    }

    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getName()     { return name; }
    public String getEmail()    { return email; }

    public void setUsername(String username) { this.username = username; }
    public void setPassword(String password) { this.password = password; }
    public void setName(String name)         { this.name = name; }
    public void setEmail(String email)       { this.email = email; }

    @Override
    public String toString() {
        return "SignupRequest{username='" + username + "', name='" + name + "', email='" + email + "'}";
    }
}