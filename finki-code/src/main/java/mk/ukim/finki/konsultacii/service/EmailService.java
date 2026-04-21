package mk.ukim.finki.konsultacii.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import mk.ukim.finki.konsultacii.model.dtos.MailSendingStatus;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionService;


public interface EmailService {

    CompletableFuture<MailSendingStatus> sendMail(String[] to, String subject, String template,
                                                 List<String> cc, Map<String, Object> model, File attachment);

    void saveMailToDisk(MimeMessage mimeMessage) throws IOException, MessagingException;
}