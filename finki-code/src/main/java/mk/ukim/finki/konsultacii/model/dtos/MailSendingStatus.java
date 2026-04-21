package mk.ukim.finki.konsultacii.model.dtos;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
public class MailSendingStatus {

    public boolean sent;

    public String[] to;

    public String subject;

    public String reason;
}
