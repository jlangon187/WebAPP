package com.gpbmods.backend.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${frontend.url:http://localhost:4200}")
    private String frontendUrl;

    public void sendPasswordResetEmail(String toEmail, String token) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("soporte@gpbikes-mods.com", "Soporte GPBikes-Mods");
            helper.setTo(toEmail);
            helper.setSubject("GPBikes Mods - Recuperación de Contraseña");

            String resetLink = frontendUrl + "/reset-password?token=" + token;

            String htmlMessage = "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; background-color: #0f1623; color: #ffffff; border-radius: 10px;'>" +
                    "<h2 style='color: #e60000; text-align: center;'>GPBikes Mods</h2>" +
                    "<p style='color: #e2e8f0;'>Hola,</p>" +
                    "<p style='color: #e2e8f0;'>Has solicitado restablecer tu contraseña. Haz clic en el botón de abajo para asignar una nueva. Este enlace será válido durante 15 minutos.</p>" +
                    "<div style='text-align: center; margin: 30px 0;'>" +
                    "<a href='" + resetLink + "' style='background-color: #003399; color: #ffffff; padding: 12px 24px; text-decoration: none; border-radius: 5px; font-weight: bold;'>Restablecer Contraseña</a>" +
                    "</div>" +
                    "<p style='color: #e2e8f0;'>Si el botón no funciona, copia y pega este enlace en tu navegador:</p>" +
                    "<p style='word-break: break-all; color: #94a3b8; font-size: 12px;'>" + resetLink + "</p>" +
                    "<p style='color: #e2e8f0; margin-top: 30px;'>Si no solicitaste este cambio, puedes ignorar este correo.</p>" +
                    "</div>";

            helper.setText(htmlMessage, true);

            mailSender.send(message);

        } catch (MessagingException | java.io.UnsupportedEncodingException e) {
            throw new RuntimeException("Error al enviar el correo de recuperación", e);
        }
    }

    public void sendTicketResponseEmail(String toEmail, Long ticketId, String respuesta, String estado) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("soporte@gpbikes-mods.com", "Soporte GPBikes-Mods");
            helper.setTo(toEmail);
            helper.setSubject("GPBikes Mods - Respuesta a Ticket #" + ticketId);

            String htmlMessage = "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; background-color: #0f1623; color: #ffffff; border-radius: 10px;'>" +
                    "<h2 style='color: #e60000; text-align: center;'>Respuesta de Soporte</h2>" +
                    "<p style='color: #e2e8f0;'>Tu ticket <strong>#" + ticketId + "</strong> ha sido actualizado.</p>" +
                    "<p style='color: #e2e8f0;'><strong>Estado:</strong> " + estado + "</p>" +
                    "<div style='margin: 20px 0; padding: 12px; border-radius: 6px; background-color: #1e293b; color: #e2e8f0; white-space: pre-wrap;'>" + respuesta + "</div>" +
                    "<p style='color: #94a3b8;'>Puedes revisar el ticket iniciando sesión en la plataforma.</p>" +
                    "<p style='color: #94a3b8;'>Nota: si el ticket se cierra, no podrá reabrirse automáticamente.</p>" +
                    "</div>";

            helper.setText(htmlMessage, true);
            mailSender.send(message);
        } catch (MessagingException | java.io.UnsupportedEncodingException e) {
            throw new RuntimeException("Error al enviar correo de respuesta de ticket", e);
        }
    }
}
