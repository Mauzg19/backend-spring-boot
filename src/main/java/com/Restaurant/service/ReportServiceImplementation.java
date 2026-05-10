package com.Restaurant.service;

import com.Restaurant.model.Food;
import com.Restaurant.model.Order;
import com.Restaurant.model.User;
import com.Restaurant.repository.OrderRepository;
import com.Restaurant.repository.UserRepository;
import com.Restaurant.repository.foodRepository;
import com.opencsv.CSVWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class ReportServiceImplementation implements ReportService {

    private static final Logger LOG = LoggerFactory.getLogger(ReportServiceImplementation.class);

	private final Path reportsDir;
	private final OrderRepository orderRepository;
	private final foodRepository foodRepository;
	private final UserRepository userRepository;

	public ReportServiceImplementation(@Value("${app.reports.path:./reports}") String reportsPath,
									   OrderRepository orderRepository,
									   foodRepository foodRepository,
									   UserRepository userRepository) throws IOException {
		this.reportsDir = Paths.get(reportsPath).toAbsolutePath().normalize();
		this.orderRepository = orderRepository;
		this.foodRepository = foodRepository;
		this.userRepository = userRepository;
		Files.createDirectories(reportsDir);
	}

    @Override
    public String exportSalesCsv(LocalDate from, LocalDate to, Long restaurantId) throws IOException {
        Date fromDate = Date.from(from.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date toDate = Date.from(to.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
        List<Order> orders = orderRepository.findByCreatedAtBetween(fromDate, toDate);

        String fileName = "sales_" + restaurantId + "_" + System.currentTimeMillis() + ".csv";
        Path file = reportsDir.resolve(fileName);
        try (CSVWriter csv = new CSVWriter(Files.newBufferedWriter(file))) {
            csv.writeNext(new String[]{"id", "customer", "total", "status", "date", "items"});
            orders.stream()
                    .filter(o -> o.getRestaurant() != null
                            && o.getRestaurant().getId().equals(restaurantId))
                    .forEach(o -> csv.writeNext(new String[]{
                            String.valueOf(o.getId()),
                            o.getCustomer() != null ? o.getCustomer().getEmail() : "N/A",
                            String.valueOf(o.getTotalAmount()),
                            String.valueOf(o.getOrderStatus()),
                            String.valueOf(o.getCreatedAt()),
                            String.valueOf(o.getTotalItem())
                    }));
        }
        LOG.info("Generated report: {}", fileName);
        return fileName;
    }

    @Override
    public String exportSalesTxt(LocalDate from, LocalDate to, Long restaurantId) throws IOException {
        Date fromDate = Date.from(from.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date toDate = Date.from(to.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
        List<Order> orders = orderRepository.findByCreatedAtBetween(fromDate, toDate);

        String fileName = "sales_" + restaurantId + "_" + System.currentTimeMillis() + ".txt";
        Path file = reportsDir.resolve(fileName);
        try (BufferedWriter w = Files.newBufferedWriter(file)) {
            w.write("=== REPORTE DE VENTAS ===\n");
            w.write(String.format("Desde: %s  Hasta: %s\n", from, to));
            w.write("========================================\n");
            orders.stream()
                    .filter(o -> o.getRestaurant() != null
                            && o.getRestaurant().getId().equals(restaurantId))
                    .forEach(o -> {
                        try {
                            w.write(String.format("Order #%d | %s | $%d | %s | %s\n",
                                    o.getId(),
                                    o.getCustomer() != null ? o.getCustomer().getEmail() : "N/A",
                                    o.getTotalAmount(),
                                    o.getOrderStatus(),
                                    o.getCreatedAt()));
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    });
        }
        LOG.info("Generated report: {}", fileName);
        return fileName;
    }

    @Override
    public String exportMenuCsv(Long restaurantId) throws IOException {
        List<Food> foods = foodRepository.findByRestaurantId(restaurantId);
        String fileName = "menu_" + restaurantId + "_" + System.currentTimeMillis() + ".csv";
        Path file = reportsDir.resolve(fileName);
        try (CSVWriter csv = new CSVWriter(Files.newBufferedWriter(file))) {
            csv.writeNext(new String[]{"id", "name", "price", "available", "category", "vegetarian", "seasonal"});
            foods.forEach(f -> csv.writeNext(new String[]{
                    String.valueOf(f.getId()),
                    f.getName(),
                    String.valueOf(f.getPrice()),
                    String.valueOf(f.isAvailable()),
                    f.getFoodCategory() != null ? f.getFoodCategory().getName() : "",
                    String.valueOf(f.isVegetarian()),
                    String.valueOf(f.isSeasonal())
            }));
        }
        LOG.info("Generated report: {}", fileName);
        return fileName;
    }

    @Override
    public String exportMenuTxt(Long restaurantId) throws IOException {
        List<Food> foods = foodRepository.findByRestaurantId(restaurantId);
        String fileName = "menu_" + restaurantId + "_" + System.currentTimeMillis() + ".txt";
        Path file = reportsDir.resolve(fileName);
        try (BufferedWriter w = Files.newBufferedWriter(file)) {
            w.write("=== REPORTE DE MENÚ ===\n");
            w.write(String.format("Restaurante #%d\n", restaurantId));
            w.write("========================================\n");
            for (Food f : foods) {
                w.write(String.format("%s | $%d | %s | %s\n",
                        f.getName(), f.getPrice(),
                        f.isAvailable() ? "Disponible" : "No disponible",
                        f.getFoodCategory() != null ? f.getFoodCategory().getName() : ""));
            }
        }
        LOG.info("Generated report: {}", fileName);
        return fileName;
    }

    @Override
    public List<String> listAvailableReports() {
        try (var files = Files.list(reportsDir)) {
            return files.filter(Files::isRegularFile)
                    .map(p -> p.getFileName().toString())
                    .toList();
        } catch (IOException e) {
            LOG.warn("Error listing reports: {}", e.getMessage());
            return List.of();
        }
    }

    @Override
    public String exportUsersCsv() throws IOException {
        List<User> users = userRepository.findAll();
        String fileName = "users_" + System.currentTimeMillis() + ".csv";
        Path file = reportsDir.resolve(fileName);
        try (CSVWriter csv = new CSVWriter(Files.newBufferedWriter(file))) {
            csv.writeNext(new String[]{"id", "fullName", "email", "role", "status"});
            for (User u : users) {
                csv.writeNext(new String[]{
                        String.valueOf(u.getId()),
                        u.getFullName(),
                        u.getEmail(),
                        u.getRole() != null ? u.getRole().name() : "",
                        u.getStatus() != null ? u.getStatus() : ""
                });
            }
        }
        LOG.info("Generated report: {}", fileName);
        return fileName;
    }

    @Override
    public String exportUsersTxt() throws IOException {
        List<User> users = userRepository.findAll();
        String fileName = "users_" + System.currentTimeMillis() + ".txt";
        Path file = reportsDir.resolve(fileName);
        try (BufferedWriter w = Files.newBufferedWriter(file)) {
            w.write("=== REPORTE DE USUARIOS ===\n");
            w.write("========================================\n");
            for (User u : users) {
                w.write(String.format("#%d | %s | %s | %s | %s\n",
                        u.getId(), u.getFullName(), u.getEmail(),
                        u.getRole() != null ? u.getRole().name() : "",
                        u.getStatus() != null ? u.getStatus() : ""));
            }
        }
        LOG.info("Generated report: {}", fileName);
        return fileName;
    }
}
