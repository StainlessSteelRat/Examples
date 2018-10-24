package io.echoplex.web.utils;

import javax.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Component
public class Email {

	Logger log = LoggerFactory.getLogger(Email.class);
	
	@Autowired
    private JavaMailSender javaMailSender;
	
	@Autowired
    private TemplateEngine templateEngine;
 
    
    public EmailStatus sendPlainText(String to, String subject, String text) {
        return sendM(to, subject, text, false);
    }
 
    public EmailStatus sendHtml(String to, String subject, String htmlBody) {
        return sendM(to, subject, htmlBody, true);
    }
    
    public EmailStatus sendHtmlWithTemplate(String to, String subject, String templateName, Context context) {
        String body = templateEngine.process(templateName, context);
        return sendHtml(to, subject, body);
    }
 
    private EmailStatus sendM(String to, String subject, String text, Boolean isHtml) {
        try {
            MimeMessage mail = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mail, true);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text, isHtml);
            javaMailSender.send(mail);
            log.info("Send email '{}' to: {}", subject, to);
            return new EmailStatus(to, subject, text).success();
        } catch (Exception e) {
            log.error(String.format("Problem with sending email to: {}, error message: {}", to, e.getMessage()));
            return new EmailStatus(to, subject, text).error(e.getMessage());
        }
    }
}
