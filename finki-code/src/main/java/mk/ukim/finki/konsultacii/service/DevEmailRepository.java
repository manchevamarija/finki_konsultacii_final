package mk.ukim.finki.konsultacii.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import java.io.IOException;

public interface DevEmailRepository {
    void saveMailToDisk(MimeMessage mimeMessage) throws IOException, MessagingException;
}