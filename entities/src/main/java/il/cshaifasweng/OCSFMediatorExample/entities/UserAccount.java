package il.cshaifasweng.OCSFMediatorExample.entities;

import java.io.Serializable;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.persistence.*;
import javax.persistence.Enumerated;
import javax.persistence.EnumType;
import il.cshaifasweng.OCSFMediatorExample.entities.Role;


/* A utility class to hash passwords and check passwords vs hashed values. It uses a combination of hashing and unique
 * salt. The algorithm used is PBKDF2WithHmacSHA1 which, although not the best for hashing password (vs. bcrypt) is
 * still considered robust and <a href="https://security.stackexchange.com/a/6415/12614"> recommended by NIST </a>.
 * The hashed value has 256 bits.
 */
class Passwords {

    private static final Random RANDOM = new SecureRandom();
    private static final int ITERATIONS = 10000;
    private static final int KEY_LENGTH = 256;

    /**
     * static utility class
     */
    private Passwords() { }

    /**
     * Returns a random salt to be used to hash a password.
     *
     * @return a 16 bytes random salt
     */
    public static byte[] getNextSalt() {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        return salt;
    }

    /**
     * Returns a salted and hashed password using the provided hash.<br>
     * Note - side effect: the password is destroyed (the char[] is filled with zeros)
     *
     * @param password the password to be hashed
     * @param salt     a 16 bytes salt, ideally obtained with the getNextSalt method
     *
     * @return the hashed password with a pinch of salt
     */
    public static byte[] hash(char[] password, byte[] salt) {
        PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH);
        Arrays.fill(password, Character.MIN_VALUE);
        try {
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            return skf.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new AssertionError("Error while hashing a password: " + e.getMessage(), e);
        } finally {
            spec.clearPassword();
        }
    }

    /**
     * Returns true if the given password and salt match the hashed value, false otherwise.<br>
     * Note - side effect: the password is destroyed (the char[] is filled with zeros)
     *
     * @param password     the password to check
     * @param salt         the salt used to hash the password
     * @param expectedHash the expected hashed value of the password
     *
     * @return true if the given password and salt match the hashed value, false otherwise
     */
    public static boolean isExpectedPassword(char[] password, byte[] salt, byte[] expectedHash) {
        byte[] pwdHash = hash(password, salt);
        Arrays.fill(password, Character.MIN_VALUE);
        if (pwdHash.length != expectedHash.length) return false;
        for (int i = 0; i < pwdHash.length; i++) {
            if (pwdHash[i] != expectedHash[i]) return false;
        }
        return true;
    }

    /**
     * Generates a random password of a given length, using letters and digits.
     *
     * @param length the length of the password
     *
     * @return a random password
     */
    public static String generateRandomPassword(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int c = RANDOM.nextInt(62);
            if (c <= 9) {
                sb.append(String.valueOf(c));
            } else if (c < 36) {
                sb.append((char) ('a' + c - 10));
            } else {
                sb.append((char) ('A' + c - 36));
            }
        }
        return sb.toString();
    }
}

@Entity
@Table(name = "UserAccount")
public class UserAccount implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id", nullable = false, unique = true)
    private int userId;
    @Column(name = "login") private String login;
    @Column(name = "hash") private String hash;
    @Column(name = "salt") private String salt;
    @Column(name = "is_active") private boolean is_active;
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role;   // CUSTOMER / EMPLOYEE / MANAGER

    @Enumerated(EnumType.STRING)
    @Column(name = "branch_type", nullable = false)
    private UserBranchType userBranchType;   // BRANCH / ALL_BRANCHES / SUBSCRIPTION

    // If a user created an accout for a specific branch - this field should be filled
    @OneToOne(cascade = CascadeType.ALL,
        optional = true)
    @JoinColumn(name = "branch_id", referencedColumnName = "branch_id")
    private Branch branch;

    @Embedded
    private PaymentMethod defaultPaymentMethod;

    @OneToMany(mappedBy = "userAccount", orphanRemoval = false)
    private Set<Order> orderSet = new HashSet<>();

    @OneToMany(mappedBy = "userAccount", orphanRemoval = false)
    private Set<Complaint> complaintSet = new HashSet<>();

    // TODO: should we make this set specific for a ManagerAccount?
    @OneToMany(mappedBy = "managerAccount", orphanRemoval = false)
    private  Set<Complaint> managerComplaintSet = new HashSet<>();

    public UserAccount(String login, String password, PaymentMethod defaultPaymentMethod) {
        this.login = login;
        // Generate salt, calculate hash, set salt and hash
        var generatedSalt = Passwords.getNextSalt();
        var hash = Passwords.hash(password.toCharArray(), generatedSalt);
        this.hash = new String(hash);
        this.salt = new String(generatedSalt);
        this.defaultPaymentMethod = defaultPaymentMethod;
        this.is_active = true;
        this.role = Role.CUSTOMER;
        // TODO: check
    }
    public UserAccount(String login, String password) {
        this(login, password, null);
    }

    protected UserAccount() {

    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }

    public PaymentMethod getDefaultPaymentMethod() {
        return defaultPaymentMethod;
    }

    public void setDefaultPaymentMethod(PaymentMethod defaultPaymentMethod) {
        this.defaultPaymentMethod = defaultPaymentMethod;
    }

    public Set<Order> getOrderSet() {
        return orderSet;
    }

    public void setOrderSet(Set<Order> orderSet) {
        this.orderSet = orderSet;
    }

    public Set<Complaint> getComplaintSet() {
        return complaintSet;
    }

    public void setComplaintSet(Set<Complaint> complaintSet) {
        this.complaintSet = complaintSet;
    }

    public boolean isIs_active() {
        return is_active;
    }

    public void setIs_active(boolean is_active) {
        this.is_active = is_active;
    }

    public Set<Complaint> getManagerComplaintSet() {
        return managerComplaintSet;
    }

    public void setManagerComplaintSet(Set<Complaint> managerComplaintSet) {
        this.managerComplaintSet = managerComplaintSet;
    }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
}
