package il.cshaifasweng.OCSFMediatorExample.server.EntityManagers;

import il.cshaifasweng.OCSFMediatorExample.entities.EmailSender;
import il.cshaifasweng.OCSFMediatorExample.entities.UserAccount;
import org.hibernate.SessionFactory;
import java.util.List;

public class NewsletterManager {

    private final UserAccountManager userManager;

    public NewsletterManager(SessionFactory sessionFactory) {
        this.userManager = new UserAccountManager(sessionFactory);
    }

    public void sendNewsletterToAll(String subject, String body) {

        List<UserAccount> subscribers = userManager.listNewsletterSubscribers();

        System.out.println("📨 Sending newsletter to " + subscribers.size() + " customers...");

        for (UserAccount user : subscribers) {

            String email = user.getEmail();

            try {
                EmailSender.sendEmail(subject, body, email);
                System.out.println("✔ Sent to: " + email);

            } catch (Exception e) {
                System.err.println("❌ Failed sending to " + email);
            }
        }

        System.out.println("✅ Newsletter finished sending.");
    }
}
