// SmsService.java
package pe.com.ikaza.backend.service;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SmsService {

    @Value("${twilio.account.sid}")
    private String accountSid;

    @Value("${twilio.auth.token}")
    private String authToken;

    @Value("${twilio.phone.number}")
    private String fromPhoneNumber;

    private boolean twilioInitialized = false;

    private void initializeTwilio() {
        if (!twilioInitialized) {
            Twilio.init(accountSid, authToken);
            twilioInitialized = true;
            log.info("Twilio inicializado correctamente");
        }
    }

    /**
     * Enviar SMS con código de verificación
     */
    public void enviarCodigoVerificacion(String telefono, String codigo) {
        try {
            initializeTwilio();

            String mensajeTexto = String.format(
                    "Tu código de verificación es: %s\n\nEste código es válido por 10 minutos.",
                    codigo);

            Message message = Message.creator(
                    new PhoneNumber(telefono),
                    new PhoneNumber(fromPhoneNumber),
                    mensajeTexto).create();

            log.info("SMS enviado exitosamente. SID: {}", message.getSid());

        } catch (Exception e) {
            log.error("Error enviando SMS a {}: {}", telefono, e.getMessage());
            throw new RuntimeException("Error al enviar SMS: " + e.getMessage());
        }
    }

    /**
     * Validar formato de teléfono
     */
    public boolean isValidPhoneNumber(String telefono) {
        return telefono != null && telefono.matches("^\\+[1-9]\\d{9,14}$");
    }
}
