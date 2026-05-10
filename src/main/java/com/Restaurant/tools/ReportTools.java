package com.Restaurant.tools;

import com.Restaurant.model.Restaurant;
import com.Restaurant.model.User;
import com.Restaurant.repository.UserRepository;
import com.Restaurant.service.ReportService;
import com.Restaurant.service.RestaurantService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class ReportTools {

    private static final Logger LOG = LoggerFactory.getLogger(ReportTools.class);

    private record Resolution(Long id, String error) {}

    private final ReportService reportService;
    private final RestaurantService restaurantService;
    private final UserRepository userRepository;

    public ReportTools(ReportService reportService, RestaurantService restaurantService,
                       UserRepository userRepository) {
        this.reportService = reportService;
        this.restaurantService = restaurantService;
        this.userRepository = userRepository;
    }

    @PreAuthorize("hasAnyRole('ADMIN','RESTAURANT_OWNER')")
    @Tool(description = "Genera un reporte CSV o TXT de ventas por rango de fechas. ADMIN debe especificar restaurantId; OWNER usa el suyo automáticamente.")
    public String generateSalesReport(
            @ToolParam(description = "Fecha inicio (YYYY-MM-DD)") String fromDate,
            @ToolParam(description = "Fecha fin (YYYY-MM-DD)") String toDate,
            @ToolParam(description = "Formato: csv o txt") String format,
            @ToolParam(description = "ID del restaurante (ADMIN debe especificar; OWNER opcional)") Long restaurantId) {
        LOG.info("tool=generateSalesReport from={} to={} format={} restaurantId={}",
                fromDate, toDate, format, restaurantId);
        try {
            Resolution resolution = resolveRestaurantId(restaurantId);
            if (resolution.error != null) return resolution.error;

            LocalDate from = LocalDate.parse(fromDate);
            LocalDate to = LocalDate.parse(toDate);
            String fmt = (format != null && format.equalsIgnoreCase("txt")) ? "txt" : "csv";
            String fileName;
            if ("txt".equals(fmt)) {
                fileName = reportService.exportSalesTxt(from, to, resolution.id);
            } else {
                fileName = reportService.exportSalesCsv(from, to, resolution.id);
            }
            return "http://localhost:8080/api/reports/" + fileName
                    + "\nCOPIA la URL de arriba y pégala en tu navegador para descargar el archivo.";
        } catch (AccessDeniedException e) {
            return "DENEGADO: solo ADMIN o RESTAURANT_OWNER pueden generar reportes.";
        } catch (Exception e) {
            LOG.error("generateSalesReport error", e);
            return "Error al generar reporte: " + e.getMessage();
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN','RESTAURANT_OWNER')")
    @Tool(description = "Genera un reporte CSV o TXT del menú de un restaurante. ADMIN debe especificar restaurantId; OWNER usa el suyo automáticamente.")
    public String generateMenuReport(
            @ToolParam(description = "ID del restaurante (ADMIN debe especificar; OWNER opcional)") Long restaurantId,
            @ToolParam(description = "Formato: csv o txt") String format) {
        LOG.info("tool=generateMenuReport restaurantId={} format={}", restaurantId, format);
        try {
            Resolution resolution = resolveRestaurantId(restaurantId);
            if (resolution.error != null) return resolution.error;

            String fmt = (format != null && format.equalsIgnoreCase("txt")) ? "txt" : "csv";
            String fileName;
            if ("txt".equals(fmt)) {
                fileName = reportService.exportMenuTxt(resolution.id);
            } else {
                fileName = reportService.exportMenuCsv(resolution.id);
            }
            return String.format("Reporte de menú generado: %s. Descarga: GET /api/reports/%s",
                    fileName, fileName);
        } catch (AccessDeniedException e) {
            return "DENEGADO: solo ADMIN o RESTAURANT_OWNER pueden generar reportes.";
        } catch (Exception e) {
            LOG.error("generateMenuReport error", e);
            return "Error al generar reporte: " + e.getMessage();
        }
    }

    @PreAuthorize("isAuthenticated()")
    @Tool(description = "Lista los archivos de reportes disponibles para descargar. No requiere parámetros.")
    public String listAvailableReports() {
        LOG.info("tool=listAvailableReports");
        try {
            List<String> files = reportService.listAvailableReports();
            if (files.isEmpty()) return "No hay reportes disponibles.";
            StringBuilder sb = new StringBuilder("Reportes disponibles:\n");
            for (String f : files) {
                sb.append(String.format("- %s (GET /api/reports/%s)\n", f, f));
            }
            return sb.toString();
        } catch (AccessDeniedException e) {
            return "DENEGADO: inicia sesión para ver reportes.";
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Tool(description = "Genera un reporte CSV o TXT con todos los usuarios del sistema. Solo ADMIN.")
    public String generateUsersReport(
            @ToolParam(description = "Formato: csv o txt") String format) {
        LOG.info("tool=generateUsersReport format={}", format);
        try {
            String fmt = (format != null && format.equalsIgnoreCase("txt")) ? "txt" : "csv";
            String fileName;
            if ("txt".equals(fmt)) {
                fileName = reportService.exportUsersTxt();
            } else {
                fileName = reportService.exportUsersCsv();
            }
            return String.format("Reporte de usuarios generado: %s. Descarga: GET /api/reports/%s",
                    fileName, fileName);
        } catch (AccessDeniedException e) {
            return "DENEGADO: solo ADMIN puede generar reportes de usuarios.";
        } catch (Exception e) {
            LOG.error("generateUsersReport error", e);
            return "Error al generar reporte: " + e.getMessage();
        }
    }

    private Resolution resolveRestaurantId(Long explicitId) {
        User user = getCurrentUser();
        if (user == null) return new Resolution(null, "Usuario no autenticado.");

        boolean isAdmin = hasRole("ROLE_ADMIN");

        if (explicitId != null) {
            if (isAdmin) return new Resolution(explicitId, null);
            Long ownerRestaurantId = getOwnerRestaurantId(user);
            if (ownerRestaurantId == null) {
                return new Resolution(null, "No tienes un restaurante asociado a tu cuenta.");
            }
            if (!explicitId.equals(ownerRestaurantId)) {
                return new Resolution(null, "DENEGADO: el restaurante " + explicitId
                        + " no es tuyo. Tu restaurante es el ID=" + ownerRestaurantId + ".");
            }
            return new Resolution(explicitId, null);
        }

        if (isAdmin) return new Resolution(null, "ADMIN: debes especificar restaurantId para generar el reporte.");

        Long ownerRestaurantId = getOwnerRestaurantId(user);
        if (ownerRestaurantId == null) {
            return new Resolution(null, "No tienes un restaurante asociado. Contacta al administrador.");
        }
        return new Resolution(ownerRestaurantId, null);
    }

    private Long getOwnerRestaurantId(User user) {
        try {
            Restaurant r = restaurantService.getRestaurantsByUserId(user.getId());
            return r != null ? r.getId() : null;
        } catch (Exception e) {
            LOG.warn("No restaurant found for user={}", user.getEmail());
            return null;
        }
    }

    private boolean hasRole(String role) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        for (GrantedAuthority ga : auth.getAuthorities()) {
            if (ga.getAuthority().equals(role)) return true;
        }
        return false;
    }

    private User getCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        return userRepository.findByEmail(auth.getName());
    }
}
