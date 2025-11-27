package pe.com.ikaza.backend.service;

import pe.com.ikaza.backend.entity.Pedido;
import pe.com.ikaza.backend.entity.DetallePedido;
import pe.com.ikaza.backend.entity.MensajeBuzon;
import pe.com.ikaza.backend.entity.Usuario;
import pe.com.ikaza.backend.repository.ClienteRepository;
import pe.com.ikaza.backend.entity.Cliente;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService {

        private final JavaMailSender mailSender;
        private final ClienteRepository clienteRepository;

        @Value("${spring.mail.username}")
        private String fromEmail;

        @Value("${app.name:TiendaApp}")
        private String appName;

        /**
         * Envía correo de confirmación de pedido.
         */
        public void enviarConfirmacionPedido(Pedido pedido, String emailUsuario) {
                try {
                        log.info("Enviando confirmación de pedido {} a {}",
                                        pedido.getNumeroPedido(), emailUsuario);

                        MimeMessage message = mailSender.createMimeMessage();
                        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

                        helper.setFrom(fromEmail);
                        helper.setTo(emailUsuario);
                        helper.setSubject("Confirmación de Pedido " + pedido.getNumeroPedido());

                        String htmlContent = construirHtmlConfirmacion(pedido);
                        helper.setText(htmlContent, true);

                        mailSender.send(message);

                        log.info("Email de confirmación enviado exitosamente a {}", emailUsuario);

                } catch (Exception e) {
                        log.error("Error al enviar email de confirmación", e);
                }
        }

        /**
         * Construye el HTML del correo de confirmación.
         */
        private String construirHtmlConfirmacion(Pedido pedido) {
                StringBuilder html = new StringBuilder();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

                html.append("<!DOCTYPE html>");
                html.append("<html><head><meta charset='UTF-8'></head><body>");
                html.append("<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;'>");

                // Header
                html.append(
                                "<div style='background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 30px; text-align: center;'>");
                html.append("<h1 style='margin: 0;'>¡Gracias por tu compra!</h1>");
                html.append("<p style='margin: 10px 0 0 0; font-size: 16px;'>").append(appName).append("</p>");
                html.append("</div>");

                // Información del pedido
                html.append("<div style='padding: 30px; background-color: #f8f9fa;'>");
                html.append("<h2 style='color: #333; margin-top: 0;'>Detalles del Pedido</h2>");
                html.append("<table style='width: 100%; border-collapse: collapse;'>");
                html.append(
                                "<tr><td style='padding: 10px 0; border-bottom: 1px solid #dee2e6;'><strong>Número de Pedido:</strong></td>");
                html.append("<td style='padding: 10px 0; border-bottom: 1px solid #dee2e6; text-align: right;'>")
                                .append(pedido.getNumeroPedido()).append("</td></tr>");
                html.append("<tr><td style='padding: 10px 0; border-bottom: 1px solid #dee2e6;'><strong>Fecha:</strong></td>");
                html.append("<td style='padding: 10px 0; border-bottom: 1px solid #dee2e6; text-align: right;'>")
                                .append(pedido.getFechaPedido().format(formatter)).append("</td></tr>");
                html.append("<tr><td style='padding: 10px 0; border-bottom: 1px solid #dee2e6;'><strong>Estado:</strong></td>");
                html.append(
                                "<td style='padding: 10px 0; border-bottom: 1px solid #dee2e6; text-align: right;'><span style='background-color: #28a745; color: white; padding: 5px 10px; border-radius: 5px;'>")
                                .append(pedido.getEstado()).append("</span></td></tr>");
                html.append("</table>");
                html.append("</div>");

                // Productos
                html.append("<div style='padding: 30px; background-color: white;'>");
                html.append("<h3 style='color: #333; margin-top: 0;'>Productos</h3>");
                html.append("<table style='width: 100%; border-collapse: collapse;'>");
                html.append("<thead><tr style='background-color: #f8f9fa;'>");
                html.append("<th style='padding: 10px; text-align: left; border-bottom: 2px solid #dee2e6;'>Producto</th>");
                html.append("<th style='padding: 10px; text-align: center; border-bottom: 2px solid #dee2e6;'>Cantidad</th>");
                html.append("<th style='padding: 10px; text-align: right; border-bottom: 2px solid #dee2e6;'>Precio</th>");
                html.append("<th style='padding: 10px; text-align: right; border-bottom: 2px solid #dee2e6;'>Subtotal</th>");
                html.append("</tr></thead><tbody>");

                for (DetallePedido detalle : pedido.getDetalles()) {
                        html.append("<tr>");
                        html.append("<td style='padding: 10px; border-bottom: 1px solid #dee2e6;'>")
                                        .append(detalle.getProducto().getNombreProducto());
                        if (detalle.getColorSeleccionado() != null || detalle.getTallaSeleccionada() != null) {
                                html.append("<br><small style='color: #6c757d;'>");
                                if (detalle.getColorSeleccionado() != null)
                                        html.append(detalle.getColorSeleccionado());
                                if (detalle.getTallaSeleccionada() != null)
                                        html.append(" - ").append(detalle.getTallaSeleccionada());
                                html.append("</small>");
                        }
                        html.append("</td>");
                        html.append("<td style='padding: 10px; text-align: center; border-bottom: 1px solid #dee2e6;'>")
                                        .append(detalle.getCantidad()).append("</td>");
                        html.append("<td style='padding: 10px; text-align: right; border-bottom: 1px solid #dee2e6;'>S/ ")
                                        .append(String.format("%.2f", detalle.getPrecioUnitario())).append("</td>");
                        html.append("<td style='padding: 10px; text-align: right; border-bottom: 1px solid #dee2e6;'>S/ ")
                                        .append(String.format("%.2f", detalle.getSubtotal())).append("</td>");
                        html.append("</tr>");
                }

                html.append("</tbody></table>");
                html.append("</div>");

                // Total
                html.append("<div style='padding: 30px; background-color: #f8f9fa; border-top: 3px solid #667eea;'>");
                html.append("<div style='text-align: right;'>");
                html.append("<h2 style='color: #333; margin: 0;'>Total: <span style='color: #28a745;'>S/ ")
                                .append(String.format("%.2f", pedido.getTotal())).append("</span></h2>");
                html.append("</div>");
                html.append("</div>");

                // Footer
                html.append("<div style='padding: 20px; text-align: center; background-color: #343a40; color: white;'>");
                html.append("<p style='margin: 0; font-size: 14px;'>Gracias por confiar en ").append(appName)
                                .append("</p>");
                html.append(
                                "<p style='margin: 10px 0 0 0; font-size: 12px; color: #adb5bd;'>Este es un correo automático, por favor no responder.</p>");
                html.append("</div>");

                html.append("</div></body></html>");

                return html.toString();
        }

        /**
         * Envía código de verificación por email
         */
        public void enviarCodigoVerificacion(String email, String codigo) {
                try {
                        log.info("Enviando código de verificación a: {}", email);

                        MimeMessage message = mailSender.createMimeMessage();
                        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

                        helper.setFrom(fromEmail);
                        helper.setTo(email);
                        helper.setSubject("Código de Verificación - " + appName);

                        String htmlContent = construirHtmlCodigoVerificacion(email, codigo);
                        helper.setText(htmlContent, true);

                        mailSender.send(message);

                        log.info("Código de verificación enviado exitosamente a {}", email);

                } catch (Exception e) {
                        log.error("Error al enviar código de verificación", e);
                        throw new RuntimeException("Error al enviar el código de verificación: " + e.getMessage());
                }
        }

        /**
         * Construye el HTML del correo de verificación
         */
        private String construirHtmlCodigoVerificacion(String email, String codigo) {
                StringBuilder html = new StringBuilder();

                html.append("<!DOCTYPE html>");
                html.append("<html><head><meta charset='UTF-8'></head><body>");
                html.append("<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;'>");

                // Header
                html.append("<div style='background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 30px; text-align: center;'>");
                html.append("<h1 style='margin: 0;'>🔐 Verificación de Cuenta</h1>");
                html.append("<p style='margin: 10px 0 0 0; font-size: 16px;'>").append(appName).append("</p>");
                html.append("</div>");

                // Código
                html.append("<div style='padding: 40px; background-color: white; text-align: center;'>");
                html.append("<p style='color: #333; font-size: 16px; margin-bottom: 30px;'>Hola,</p>");
                html.append("<p style='color: #333; font-size: 16px; margin-bottom: 30px;'>Tu código de verificación es:</p>");
                html.append("<div style='background-color: #f8f9fa; padding: 20px; border-radius: 10px; margin: 20px 0;'>");
                html.append("<h1 style='color: #667eea; font-size: 48px; letter-spacing: 10px; margin: 0;'>")
                                .append(codigo).append("</h1>");
                html.append("</div>");
                html.append("<p style='color: #6c757d; font-size: 14px; margin-top: 30px;'>Este código es válido por <strong>10 minutos</strong>.</p>");
                html.append("<p style='color: #6c757d; font-size: 14px;'>Si no solicitaste este código, puedes ignorar este mensaje.</p>");
                html.append("</div>");

                // Footer
                html.append("<div style='padding: 20px; text-align: center; background-color: #343a40; color: white;'>");
                html.append("<p style='margin: 0; font-size: 14px;'>").append(appName)
                                .append(" - Sistema de Verificación</p>");
                html.append("<p style='margin: 10px 0 0 0; font-size: 12px; color: #adb5bd;'>Este es un correo automático, por favor no responder.</p>");
                html.append("</div>");

                html.append("</div></body></html>");

                return html.toString();
        }

        /**
         * Envía correo de notificación para mensajes del buzón virtual
         */
        public void enviarMensajeBuzon(MensajeBuzon mensaje, Usuario usuario) {
                try {
                        log.info("Enviando notificación de buzón - Tipo: {}, Usuario: {}",
                                        mensaje.getTipoMensaje(), usuario.getEmail());

                        MimeMessage message = mailSender.createMimeMessage();
                        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

                        helper.setFrom(fromEmail);
                        helper.setTo(fromEmail);
                        helper.setReplyTo(usuario.getEmail());

                        // Asunto personalizado según tipo
                        String asunto = mensaje.getTipoMensaje() == MensajeBuzon.TipoMensaje.RECLAMO
                                        ? "🚨 NUEVO RECLAMO - " + mensaje.getAsunto()
                                        : "💡 NUEVA RECOMENDACIÓN - " + mensaje.getAsunto();

                        helper.setSubject(asunto + " - " + appName);

                        String htmlContent = construirHtmlBuzon(mensaje, usuario);
                        helper.setText(htmlContent, true);

                        mailSender.send(message);

                        log.info("Email de buzón enviado exitosamente");

                } catch (Exception e) {
                        log.error("❌ Error al enviar email de buzón", e);
                        throw new RuntimeException("Error al enviar el email de notificación: " + e.getMessage());
                }
        }

        /**
         * Construye el HTML del correo de buzón con logo y diseño diferenciado
         */
        private String construirHtmlBuzon(MensajeBuzon mensaje, Usuario usuario) {
                StringBuilder html = new StringBuilder();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

                // Buscar usuario por id de usuario
                Cliente cliente = clienteRepository.findByUsuarioIdUsuario(usuario.getIdUsuario())
                                .orElseThrow(() -> new RuntimeException("Cliente no encontrados."));
                // Determinar colores según tipo
                boolean esReclamo = mensaje.getTipoMensaje() == MensajeBuzon.TipoMensaje.RECLAMO;
                String colorPrincipal = esReclamo ? "#dc3545" : "#0d6efd";
                String colorFondo = esReclamo ? "#fff5f5" : "#f0f7ff";
                String icono = esReclamo ? "🚨" : "💡";
                String titulo = esReclamo ? "NUEVO RECLAMO" : "NUEVA RECOMENDACIÓN";

                html.append("<!DOCTYPE html>");
                html.append("<html><head><meta charset='UTF-8'>");
                html.append("<meta name='viewport' content='width=device-width, initial-scale=1.0'>");
                html.append("</head><body style='margin: 0; padding: 0; font-family: Arial, sans-serif; background-color: #f4f4f4;'>");
                html.append("<div style='max-width: 600px; margin: 0 auto; background-color: white;'>");

                // ========== HEADER CON LOGO ==========
                html.append("<div style='background: linear-gradient(135deg, ").append(colorPrincipal)
                                .append(" 0%, ").append(colorPrincipal)
                                .append("dd 100%); padding: 40px 30px; text-align: center;'>");

                // Logo de la empresa
                html.append("<img src='https://i.imgur.com/your-logo.png' alt='").append(appName)
                                .append("' style='max-width: 180px; height: auto; margin-bottom: 20px;' />");

                html.append("<h1 style='margin: 0; color: white; font-size: 28px;'>")
                                .append(icono).append(" ").append(titulo).append("</h1>");
                html.append("<p style='margin: 10px 0 0 0; color: rgba(255,255,255,0.9); font-size: 16px;'>")
                                .append("Buzón Virtual - ").append(appName).append("</p>");
                html.append("</div>");

                // ========== INFORMACIÓN DEL REMITENTE ==========
                html.append("<div style='padding: 30px; background-color: ").append(colorFondo).append(";'>");
                html.append("<h2 style='color: #333; margin-top: 0; font-size: 20px;'>")
                                .append("📋 Información del Usuario</h2>");
                html.append("<table style='width: 100%; border-collapse: collapse;'>");

                html.append("<tr><td style='padding: 10px 0; border-bottom: 1px solid #dee2e6;'><strong>Usuario:</strong></td>");
                html.append("<td style='padding: 10px 0; border-bottom: 1px solid #dee2e6; text-align: right;'>")
                                .append(cliente.getNombresCliente()).append(" ").append(cliente.getApellidosCliente())
                                .append("</td></tr>");

                html.append("<tr><td style='padding: 10px 0; border-bottom: 1px solid #dee2e6;'><strong>Email:</strong></td>");
                html.append("<td style='padding: 10px 0; border-bottom: 1px solid #dee2e6; text-align: right;'>")
                                .append(usuario.getEmail()).append("</td></tr>");

                html.append("<tr><td style='padding: 10px 0; border-bottom: 1px solid #dee2e6;'><strong>Fecha:</strong></td>");
                html.append("<td style='padding: 10px 0; border-bottom: 1px solid #dee2e6; text-align: right;'>")
                                .append(mensaje.getFechaCreacion().format(formatter)).append("</td></tr>");

                html.append("<tr><td style='padding: 10px 0; border-bottom: 1px solid #dee2e6;'><strong>Tipo:</strong></td>");
                html.append("<td style='padding: 10px 0; border-bottom: 1px solid #dee2e6; text-align: right;'>")
                                .append("<span style='background-color: ").append(colorPrincipal)
                                .append("; color: white; padding: 5px 12px; border-radius: 5px; font-size: 14px;'>")
                                .append(mensaje.getTipoMensaje().name()).append("</span></td></tr>");

                html.append("</table>");
                html.append("</div>");

                // ========== DETALLES DEL MENSAJE ==========
                html.append("<div style='padding: 30px; background-color: white;'>");
                html.append("<h3 style='color: #333; margin-top: 0; font-size: 18px; border-bottom: 2px solid ")
                                .append(colorPrincipal).append("; padding-bottom: 10px;'>")
                                .append(mensaje.getAsunto()).append("</h3>");

                // Si es reclamo, mostrar información adicional
                if (esReclamo) {
                        html.append("<div style='background-color: #fff8e1; border-left: 4px solid #ffc107; padding: 15px; margin-bottom: 20px; border-radius: 4px;'>");

                        if (mensaje.getCategoriaReclamo() != null) {
                                html.append("<p style='margin: 0 0 10px 0;'><strong>📂 Categoría:</strong> ")
                                                .append(obtenerNombreCategoria(mensaje.getCategoriaReclamo()))
                                                .append("</p>");

                                if (mensaje.getCategoriaReclamo() == MensajeBuzon.CategoriaReclamo.OTRO
                                                && mensaje.getReclamoOtro() != null) {
                                        html.append("<p style='margin: 0 0 10px 0;'><strong>Especificación:</strong> ")
                                                        .append(mensaje.getReclamoOtro()).append("</p>");
                                }
                        }

                        if (mensaje.getUrgenciaReclamo() != null) {
                                String urgenciaColor = mensaje.getUrgenciaReclamo() == MensajeBuzon.UrgenciaReclamo.ALTA
                                                ? "#dc3545"
                                                : "#28a745";
                                html.append("<p style='margin: 0;'><strong>⚡ Urgencia:</strong> ")
                                                .append("<span style='color: ").append(urgenciaColor)
                                                .append("; font-weight: bold;'>")
                                                .append(mensaje.getUrgenciaReclamo().name()).append("</span></p>");
                        }

                        html.append("</div>");
                }

                // Descripción del mensaje
                html.append("<div style='background-color: #f8f9fa; padding: 20px; border-left: 4px solid ")
                                .append(colorPrincipal).append("; border-radius: 5px;'>");
                html.append("<h4 style='margin: 0 0 10px 0; color: #333; font-size: 16px;'>Mensaje:</h4>");
                html.append("<p style='margin: 0; color: #333; line-height: 1.6; white-space: pre-wrap;'>")
                                .append(mensaje.getDescripcion()).append("</p>");
                html.append("</div>");

                // Archivos adjuntos
                if (mensaje.getArchivoAdjunto() != null || mensaje.getArchivoEvidencia() != null) {
                        html.append("<div style='margin-top: 20px; padding: 15px; background-color: #e7f3ff; border-radius: 5px;'>");
                        html.append("<p style='margin: 0; color: #0d6efd;'><strong>📎 Archivos adjuntos:</strong></p>");
                        html.append("<ul style='margin: 10px 0 0 0; padding-left: 20px;'>");

                        if (mensaje.getArchivoAdjunto() != null) {
                                html.append("<li style='color: #666;'>Archivo adjunto: <code>")
                                                .append(mensaje.getArchivoAdjunto()).append("</code></li>");
                        }
                        if (mensaje.getArchivoEvidencia() != null) {
                                html.append("<li style='color: #666;'>Evidencia: <code>")
                                                .append(mensaje.getArchivoEvidencia()).append("</code></li>");
                        }

                        html.append("</ul>");
                        html.append("</div>");
                }

                html.append("</div>");

                // ========== CALL TO ACTION ==========
                html.append("<div style='padding: 30px; background-color: ").append(colorFondo)
                                .append("; text-align: center; border-top: 3px solid ").append(colorPrincipal)
                                .append(";'>");
                html.append("<p style='margin: 0 0 20px 0; color: #333; font-size: 16px;'>")
                                .append("Para responder, haz clic en 'Responder' a este correo</p>");
                html.append("<a href='mailto:").append(usuario.getEmail())
                                .append("' style='display: inline-block; background-color: ").append(colorPrincipal)
                                .append("; color: white; padding: 12px 30px; text-decoration: none; border-radius: 5px; font-weight: bold;'>")
                                .append("Responder al Usuario</a>");
                html.append("</div>");

                // ========== FOOTER ==========
                html.append("<div style='padding: 20px; text-align: center; background-color: #343a40; color: white;'>");
                html.append("<p style='margin: 0; font-size: 14px;'>").append(appName)
                                .append(" - Sistema de Buzón Virtual</p>");
                html.append("<p style='margin: 10px 0 0 0; font-size: 12px; color: #adb5bd;'>")
                                .append("Este correo fue generado automáticamente desde el buzón virtual.</p>");
                html.append("<p style='margin: 10px 0 0 0; font-size: 12px;'>");
                html.append("<a href='https://tu-sitio.com' style='color: #17a2b8; text-decoration: none;'>Visitar sitio web</a>");
                html.append("</p>");
                html.append("</div>");

                html.append("</div></body></html>");

                return html.toString();
        }

        /**
         * Obtiene el nombre descriptivo de la categoría
         */
        private String obtenerNombreCategoria(MensajeBuzon.CategoriaReclamo categoria) {
                return switch (categoria) {
                        case PRODUCTO -> "Producto defectuoso";
                        case ENTREGA -> "Retraso o problema en entrega";
                        case ATENCION -> "Atención al cliente";
                        case FACTURACION -> "Facturación / cobro";
                        case OTRO -> "Otro";
                };
        }
}