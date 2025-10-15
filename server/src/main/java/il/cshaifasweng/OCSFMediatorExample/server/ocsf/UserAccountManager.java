package il.cshaifasweng.OCSFMediatorExample.server.ocsf;

import il.cshaifasweng.OCSFMediatorExample.entities.UserAccount;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import java.util.Optional;

public class UserAccountManager {
    private final SessionFactory sessionFactory;

    public UserAccountManager(SessionFactory sf) {
        this.sessionFactory = sf;
    }

    /** בדיקה מהירה: האם יש משתמש עם login כזה */
    public boolean userExists(String login) {
        try (Session s = sessionFactory.openSession()) {
            Long cnt = s.createQuery(
                    "select count(u) from UserAccount u where u.login = :login",
                    Long.class
            ).setParameter("login", login).uniqueResult();
            return cnt != null && cnt > 0;
        }
    }

    /** שליפה מלאה לפי login (לשלב האימות) */
    public Optional<UserAccount> findByLogin(String login) {
        try (Session s = sessionFactory.openSession()) {
            return Optional.ofNullable(
                    s.createQuery("from UserAccount where login = :login", UserAccount.class)
                            .setParameter("login", login)
                            .uniqueResult()
            );
        }
    }

    /** תמיכה גם ברשומות ותיקות שנשמרו כ-new String(bytes) ולא Base64 */
    private static byte[] decodeFromStorage(String stored) {
        try {
            return Base64.getDecoder().decode(stored);
        } catch (IllegalArgumentException e) {
            return stored.getBytes(StandardCharsets.ISO_8859_1);
        }
    }

    /** חישוב PBKDF2 כמו אצלכם (10,000 איטרציות, 256 ביט) */
    private static byte[] pbkdf2(char[] password, byte[] salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, 10000, 256);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
                    .generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException ex) {
            throw new RuntimeException("PBKDF2 error: " + ex.getMessage(), ex);
        }
    }

    /** השוואת סיסמה גולמית מול מה ששמור ב-DB */
    public boolean verifyPassword(UserAccount user, String plainPassword) {
        byte[] salt = decodeFromStorage(user.getSalt());
        byte[] expected = decodeFromStorage(user.getHash());
        byte[] actual = pbkdf2(plainPassword.toCharArray(), salt);
        if (actual.length != expected.length) return false;
        for (int i = 0; i < actual.length; i++) {
            if (actual[i] != expected[i]) return false;
        }
        return true;
    }

    /** אימות מלא: login+password → Optional<UserAccount> אם הצליח */
    public Optional<UserAccount> authenticate(String login, String plainPassword) {
        var opt = findByLogin(login);
        if (opt.isEmpty()) return Optional.empty();
        return verifyPassword(opt.get(), plainPassword) ? opt : Optional.empty();
    }
}
