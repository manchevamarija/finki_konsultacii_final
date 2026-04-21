package mk.ukim.finki.konsultacii.service.implementation;

import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import mk.ukim.finki.konsultacii.service.DevEmailRepository;
import org.springframework.stereotype.Repository;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;


@Repository
public class DevEmailRepositoryImpl implements DevEmailRepository {

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
