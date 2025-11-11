package il.cshaifasweng.OCSFMediatorExample.entities;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.time.LocalDate;
import java.util.*;
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
    @Column(name = "login", unique = true, nullable = false)
    private String login;
    @Column(name = "hash") private String hash;
    @Column(name = "salt") private String salt;
    @Column(name = "is_active") private boolean is_active;
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role = Role.CUSTOMER;   // CUSTOMER / EMPLOYEE / MANAGER
    @Column(name = "Name") private String Name;
    @Column(name = "email") private String email;
    @Column(name = "id_number") private String idNumber;  // ת"ז
    @Column(name = "credit_card") private String creditCardNumber;

    @Column(name = "subscription_start_date")
    private LocalDate subscriptionStartDate;

    @Column(name = "subscription_expiration_date")
    private LocalDate subscriptionExpirationDate;


    @Enumerated(EnumType.STRING)
    @Column(name = "branch_type", nullable = false)
    private UserBranchType userBranchType;   // BRANCH / ALL_BRANCHES / SUBSCRIPTION

    // If a user created an accout for a specific branch - this field should be filled
    @ManyToOne(cascade = CascadeType.ALL,
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

    public UserAccount(String login, String password, String Name,String email, PaymentMethod defaultPaymentMethod, UserBranchType BranchType) {
        this.login = login;
        // Generate salt, calculate hash, set salt and hash
        var generatedSalt = Passwords.getNextSalt();
        var hash = Passwords.hash(password.toCharArray(), generatedSalt);
        this.hash = Base64.getEncoder().encodeToString(hash);
        this.salt = Base64.getEncoder().encodeToString(generatedSalt);
        this.defaultPaymentMethod = defaultPaymentMethod;
        this.is_active = true;
        this.role = Role.CUSTOMER;
        this.userBranchType = BranchType;
        if (BranchType == UserBranchType.SUBSCRIPTION) {
            activateSubscription();
        }

        this.Name = Name;
        this.email = email;
    }
    public UserAccount(String login, String password, String Name,String email) {
        this(login, password,Name, email, null, UserBranchType.ALL_BRANCHES);
    }

    public UserAccount() {}

    public UserAccount(String login, String password) {
        this(login, password,null , null, null, UserBranchType.ALL_BRANCHES);
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
    public String getName() {
        return Name;
    }
    public void setName(String name) {
        Name = name;
    }
    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    public String getIdNumber() {
        return idNumber;
    }
    public void setIdNumber(String idNumber) {
        this.idNumber = idNumber;
    }


    // ממיר מחרוזת מה-DB לבייטים: קודם Base64; אם נכשל – ISO-8859-1 לתמיכה בנתונים ישנים
    private static byte[] decodeFromStorage(String stored) {
        try { return Base64.getDecoder().decode(stored); }
        catch (IllegalArgumentException e) { return stored.getBytes(StandardCharsets.ISO_8859_1); }
    }

    // אימות סיסמה גולמית מול ה-hash+salt השמורים למשתמש
    public boolean verifyPassword(String plainPassword) {
        byte[] salt = decodeFromStorage(this.salt);
        byte[] expected = decodeFromStorage(this.hash);
        return Passwords.isExpectedPassword(plainPassword.toCharArray(), salt, expected);
    }

    public void activateSubscription() {
        this.userBranchType = UserBranchType.SUBSCRIPTION;
        this.subscriptionStartDate = LocalDate.now();
        this.subscriptionExpirationDate = subscriptionStartDate.plusYears(1);
    }

    public boolean isSubscriptionUser() {
        return this.userBranchType == UserBranchType.SUBSCRIPTION;
    }

    public double calculateDiscount(double totalAmount) {
        if (isSubscriptionUser() && totalAmount >= 50) {
            return totalAmount * 0.10;
        }
        return 0;
    }


    public LocalDate getSubscriptionExpirationDate() {
        return this.subscriptionExpirationDate;
    }

    public UserBranchType getUserBranchType() {
        return this.userBranchType;
    }

    public Branch getBranch() {
        return this.branch;
    }

    public void setBranch(Branch branch) {
        this.branch = branch;
    }
}
