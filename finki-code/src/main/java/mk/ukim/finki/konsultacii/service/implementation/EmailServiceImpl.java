package mk.ukim.finki.konsultacii.service.implementation;

import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.AllArgsConstructor;
import mk.ukim.finki.konsultacii.model.dtos.MailSendingStatus;
import mk.ukim.finki.konsultacii.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;


@Service
@AllArgsConstructor
public class EmailServiceImpl implements EmailService {
    private static final Logger logger = LoggerFactory.getLogger(EmailServiceImpl.class);
    private final JavaMailSender mailSender;
    private final TemplateEngine emailTemplateEngine;

    @Async
    @Override
    public CompletableFuture<MailSendingStatus> sendMail(String[] to, String subject, String template, List<String> cc, Map<String, Object> model, File attachment) {
        try {
            final MimeMessage mimeMessage = mailSender.createMimeMessage();
            final MimeMessageHelper message = new MimeMessageHelper(mimeMessage, true, StandardCharsets.UTF_8.name());
            message.setFrom("noreply@finki.ukim.mk", "FINKI Consultations (noreply)");
            message.setTo(to);
            message.setSubject(subject);

            final Context ctx = new Context(Locale.getDefault());
            ctx.setVariables(model);
            final String htmlContent = emailTemplateEngine.process(template, ctx);
            message.setText(htmlContent, true);

            if (attachment != null) {
                FileSystemResource file = new FileSystemResource(attachment);
                message.addAttachment(Objects.requireNonNull(file.getFilename()), file);
            }
            logger.info("Sending email...");
            // saveMailToDisk(mimeMessage);
            mailSender.send(mimeMessage);
            return CompletableFuture.completedFuture(new MailSendingStatus(true, to, subject, null));
        } catch (Exception e) {
            logger.error("Failed to send email to {} with subject '{}'. Error: {}", String.join(", ", to), subject, e.getMessage(), e);
            return CompletableFuture.completedFuture(new MailSendingStatus(false, to, subject, e.getClass().getSimpleName() + ": " + e.getMessage()));
        }
    }

    @Override
    public void saveMailToDisk(MimeMessage mimeMessage) throws IOException, MessagingException {
        File mailsDir = new File("src/main/resources/static/mails");
        if (!mailsDir.exists()) {
            mailsDir.mkdir();
        }
        String filename = String.format("%s_%d.eml",
                mimeMessage.getRecipients(Message.RecipientType.TO)[0].toString(), LocalDateTime.now().getNano());
        File file = new File(mailsDir, filename);
        System.out.println(file.getAbsolutePath());
        mimeMessage.writeTo(new FileOutputStream(file));
    }

}
