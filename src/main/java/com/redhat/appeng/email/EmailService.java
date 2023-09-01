package com.redhat.appeng.email;

import java.net.MalformedURLException;
import java.util.Properties;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

public class EmailService {
    static final Logger logger = Logger.getLogger(EmailService.class.getName());

    private String host;
    private int port;
    private String username;
    private String password;
    private String email;

    public EmailService(String host, int port, String username, String password, String email)
            throws MalformedURLException {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.email = email;
    }

    public void sendMail() throws Exception {
        Properties prop = initMailProperties();

        Session session = Session.getInstance(prop, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(email));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(email));
        message.setSubject("Test Email");
        message.setText("This is a test email sent using " + host + " endpoint.");

        Transport transport = session.getTransport("smtp");

        transport.connect(username, password);
        System.out.println("Connected to " + host + " as " + username);
        // Uncomment to actually try to send email
        // Transport.send(message);
        // System.out.println("Message sent to " + message.getAllRecipients());
    }

    private Properties initMailProperties() {
        Properties prop = new Properties();
        prop.put("mail.smtp.auth", true);
        prop.put("mail.smtp.host", host);
        prop.put("mail.smtp.port", port);
        prop.put("mail.smtp.ssl.enable", "true");
        prop.put("mail.smtp.starttls.enable", "true");
        String supportedSslProtocols = SSLProvider.getSupportedSslProtocols();
        prop.put("mail.smtp.ssl.protocols", supportedSslProtocols);
        prop.put("mail.smtp.ssl.socketFactory.class", "org.bouncycastle.jsse.provider.ProvSSLSocketFactory");
        prop.put("mail.smtp.socketFactory.fallback", "false");

        logger.info("Mail properties: \n%s" + prop);
        return prop;
    }

    private static String argsOrEnv(String envVarName, int argsIndex, String[] args) {
        if (args.length > argsIndex) {
            logger.info(envVarName + " from args");
            return args[argsIndex];
        }
        logger.info(envVarName + " from env");
        return System.getenv(envVarName);
    }

    private static void initLogger() {
        // Use java.util.logging for compatibility with angus-mail logger
        java.util.logging.Logger rootLogger = java.util.logging.Logger.getLogger("");

        rootLogger.setLevel(java.util.logging.Level.FINEST);

        final ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setFormatter(new EmailService.NoDateFormatter());
        consoleHandler.setLevel(java.util.logging.Level.FINEST);
        rootLogger.addHandler(consoleHandler);
    }

    public static void main(String... args) {
        initLogger();
        SSLProvider.initProviders();

        try {
            new EmailService(argsOrEnv("SMTP", 0, args), 465,
                    argsOrEnv("SMTP_USER", 1, args),
                    argsOrEnv("SMTP_PWD", 2, args),
                    argsOrEnv("EMAIL", 3, args))
                    .sendMail();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static class NoDateFormatter extends Formatter {
        @Override
        public String format(LogRecord record) {
            return record.getLevel() + " - " + record.getMessage() + "\n";
        }
    }
}