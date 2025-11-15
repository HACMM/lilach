package il.cshaifasweng.OCSFMediatorExample.entities;
import javax.mail.*;
import javax.mail.internet.*;
import java.util.Properties;


public class EmailSender {

    public static void sendEmail(String subject,String body,String to) {
        final String smtpHost = "in-v3.mailjet.com";
        final String smtpPort = "587"; // Use 465 for SSL
        final String username = "b2fbaa6f8eddc0fa273fa35b3d3b676b"; // Your API Key
        final String password = "bbf35fb81a3ee13a2a53707b9eb4ee11"; // Your Secret Key

        Properties properties = new Properties();
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.smtp.host", smtpHost);
        properties.put("mail.smtp.port", smtpPort);

        Session session = Session.getInstance(properties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress("kanarmohana@gmail.com"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to)); // Recipient
            message.setSubject(subject);
            message.setText(body);

            Transport.send(message);
            System.out.println("Email sent successfully to: " + to);
        } catch (MessagingException e) {
            System.err.println("Email sending failed to " + to + ": " + e.getMessage());
            e.printStackTrace();
            // Re-throw the exception so calling code can handle it
            throw new RuntimeException("Failed to send email: " + e.getMessage(), e);
        }
    }
}