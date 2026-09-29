package com.kaveri.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaveri.entity.Category;
import com.kaveri.entity.FoodItem;
import com.kaveri.entity.Role;
import com.kaveri.entity.User;
import com.kaveri.enums.RoleName;
import com.kaveri.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.*;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataSeeder {

    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;

    @Bean
    public CommandLineRunner seedDatabase(
            RoleRepository roleRepository,
            UserRepository userRepository,
            CategoryRepository categoryRepository,
            FoodItemRepository foodItemRepository,
            PasswordEncoder passwordEncoder) {

        return args -> {
            // ---- 1. Seed Roles ----
            if (roleRepository.count() == 0) {
                roleRepository.saveAll(List.of(
                        Role.builder().name(RoleName.ROLE_CUSTOMER).build(),
                        Role.builder().name(RoleName.ROLE_ADMIN).build()
                ));
                log.info("Roles seeded successfully.");
            }

            Role adminRole = roleRepository.findByName(RoleName.ROLE_ADMIN).orElseThrow();
            Role customerRole = roleRepository.findByName(RoleName.ROLE_CUSTOMER).orElseThrow();

            // ---- 2. Seed Admin User ----
            if (!userRepository.existsByEmail("admin@thekaveri.com")) {
                User admin = User.builder()
                        .name("Kaveri Management")
                        .email("admin@thekaveri.com")
                        .password(passwordEncoder.encode("Admin@1234"))
                        .phone("06512330330")
                        .roles(Set.of(adminRole))
                        .enabled(true)
                        .build();
                userRepository.save(admin);
                log.info("Kaveri Admin user seeded: admin@thekaveri.com / Admin@1234");
            }
            if (!userRepository.existsByEmail("admin@foodnest.com")) {
                User legacyAdmin = User.builder()
                        .name("Kaveri Admin (Legacy)")
                        .email("admin@foodnest.com")
                        .password(passwordEncoder.encode("Admin@1234"))
                        .phone("9000000001")
                        .roles(Set.of(adminRole))
                        .enabled(true)
                        .build();
                userRepository.save(legacyAdmin);
            }

            // ---- 3. Seed Sample Customer ----
            if (!userRepository.existsByEmail("customer@thekaveri.com")) {
                User customer = User.builder()
                        .name("Rohan Verma")
                        .email("customer@thekaveri.com")
                        .password(passwordEncoder.encode("Customer@1234"))
                        .phone("8235520520")
                        .roles(Set.of(customerRole))
                        .enabled(true)
                        .build();
                userRepository.save(customer);
                log.info("Sample customer seeded: customer@thekaveri.com / Customer@1234");
            }
            if (!userRepository.existsByEmail("customer@foodnest.com")) {
                User legacyCustomer = User.builder()
                        .name("Rahul Sharma")
                        .email("customer@foodnest.com")
                        .password(passwordEncoder.encode("Customer@1234"))
                        .phone("9000000002")
                        .roles(Set.of(customerRole))
                        .enabled(true)
                        .build();
                userRepository.save(legacyCustomer);
            }

            // ---- 4. Seed Verified Kaveri Categories & Menu Items (242 items across 17 categories) ----
            Map<String, Category> categoryMap = new HashMap<>();
            
            // Map of default metadata for the 17 verified Kaveri categories
            Map<String, String[]> categoryMeta = Map.ofEntries(
                    Map.entry("Refreshers", new String[]{"Refreshing coolers, mojitos, and mocktails", "/images/menu/virgin-mojito.jpg"}),
                    Map.entry("Thick Shake", new String[]{"Rich, creamy handcrafted thick shakes", "/images/menu/mango-shake.jpg"}),
                    Map.entry("Tea, Snacks & Breakfast", new String[]{"Heritage snacks, pakodas, toasts, and hot tea", "/images/menu/paneer-pakoda.jpg"}),
                    Map.entry("Chat", new String[]{"Authentic Indian street food & tangy chaats", "/images/menu/pani-puri.jpg"}),
                    Map.entry("Soup", new String[]{"Hot and comforting chef-crafted soups", "/images/menu/tomato-soup.jpg"}),
                    Map.entry("Kebab", new String[]{"Tandoor char-grilled kebabs and paneer tikkas", "/images/menu/paneer-tikka.jpg"}),
                    Map.entry("Indian Main Course", new String[]{"Rich paneer curries, koftas, and seasonal specials", "/images/menu/paneer-butter-masala.jpg"}),
                    Map.entry("Lentils", new String[]{"Slow-simmered Dal Makhani and Yellow Dal Tadka", "/images/menu/dal-makhani.jpg"}),
                    Map.entry("Rice/Pulao", new String[]{"Aromatic dum biryanis, pulaos, and jeera rice", "/images/menu/hyderabadi-biryani.jpg"}),
                    Map.entry("Indian Breads", new String[]{"Tandoori naans, rotis, kulchas, and parathas", "/images/menu/butter-naan.jpg"}),
                    Map.entry("Condiments", new String[]{"Crisp papads, raitas, and house chutneys", "/images/menu/mixed-veg-raita.jpg"}),
                    Map.entry("Salad", new String[]{"Fresh green salads, Russian salads, and tossed greens", "/images/menu/green-salad.jpg"}),
                    Map.entry("Thali", new String[]{"Grand multi-course Kaveri Royal and Executive Thalis", "/images/vegthali.jpg"}),
                    Map.entry("South Indian", new String[]{"Crispy golden dosas, uttapams, idlis, and vadas", "/images/menu/butter-masala-dosa.jpg"}),
                    Map.entry("Chinese", new String[]{"Wok-tossed noodles, fried rice, manchurian, and paneer", "/images/menu/veg-chowmein.jpg"}),
                    Map.entry("Pizza", new String[]{"Stone-baked pizzas loaded with mozzarella and veggies", "/images/menu/cheese-pizza.jpg"}),
                    Map.entry("Desserts", new String[]{"Punjab Sweet House gulab jamuns, rasmalai, and ice creams", "/images/menu/gulab-jamun.jpg"})
            );

            // Pre-seed or fetch all categories
            for (Map.Entry<String, String[]> entry : categoryMeta.entrySet()) {
                String catName = entry.getKey();
                String desc = entry.getValue()[0];
                String img = entry.getValue()[1];

                Category cat = categoryRepository.findByNameIgnoreCase(catName)
                        .orElseGet(() -> categoryRepository.save(
                                Category.builder()
                                        .name(catName)
                                        .description(desc)
                                        .imageUrl(img)
                                        .build()
                        ));
                categoryMap.put(catName.toLowerCase(), cat);
            }

            // Ensure any existing categories in DB are also in categoryMap
            categoryRepository.findAll().forEach(c -> categoryMap.putIfAbsent(c.getName().toLowerCase(), c));

            // Load and seed items from kaveri-menu.json if DB has less than 240 items
            if (foodItemRepository.count() < 240) {
                log.info("Current DB food item count is {}. Seeding full Kaveri Menu (242 items)...", foodItemRepository.count());
                
                InputStream is = null;
                try {
                    Resource resource = resourceLoader.getResource("classpath:kaveri-menu.json");
                    if (resource.exists()) {
                        is = resource.getInputStream();
                    } else {
                        is = getClass().getResourceAsStream("/kaveri-menu.json");
                    }

                    if (is != null) {
                        List<JsonNode> rawItems = objectMapper.readValue(is, new TypeReference<List<JsonNode>>() {});
                        int seededCount = 0;

                        for (JsonNode node : rawItems) {
                            String name = node.has("name") ? node.get("name").asText().trim() : null;
                            if (name == null || name.isEmpty()) continue;

                            String catName = node.has("category") ? node.get("category").asText().trim() : "Indian Main Course";
                            Category category = categoryMap.get(catName.toLowerCase());
                            if (category == null) {
                                category = categoryRepository.findByNameIgnoreCase(catName)
                                        .orElseGet(() -> categoryRepository.save(
                                                Category.builder().name(catName).description(catName).build()
                                        ));
                                categoryMap.put(catName.toLowerCase(), category);
                            }

                            BigDecimal price = new BigDecimal("20.00");
                            if (node.hasNonNull("price")) {
                                try {
                                    price = new BigDecimal(node.get("price").asText().trim());
                                } catch (Exception ignored) {
                                    price = new BigDecimal("20.00");
                                }
                            }
                            String imageUrl = node.has("imageUrl") && !node.get("imageUrl").isNull() ? node.get("imageUrl").asText().trim() : null;
                            if (imageUrl != null && imageUrl.isEmpty()) imageUrl = null;

                            String description = node.has("description") && !node.get("description").isNull() ? node.get("description").asText().trim() : "";
                            boolean isVegetarian = !node.has("isVegetarian") || node.get("isVegetarian").asBoolean(true);
                            boolean isSpicy = name.toLowerCase().contains("chilli") || name.toLowerCase().contains("spicy") || name.toLowerCase().contains("kadai") || name.toLowerCase().contains("schezwan");

                            Optional<FoodItem> existingOpt = foodItemRepository.findByNameIgnoreCase(name);
                            if (existingOpt.isPresent()) {
                                FoodItem existing = existingOpt.get();
                                existing.setCategory(category);
                                existing.setPrice(price);
                                if (imageUrl != null && (existing.getImageUrl() == null || existing.getImageUrl().isBlank())) {
                                    existing.setImageUrl(imageUrl);
                                }
                                if (existing.getDescription() == null || existing.getDescription().isBlank()) {
                                    existing.setDescription(description);
                                }
                                foodItemRepository.save(existing);
                            } else {
                                FoodItem newItem = FoodItem.builder()
                                        .name(name)
                                        .description(description)
                                        .price(price)
                                        .category(category)
                                        .vegetarian(isVegetarian)
                                        .spicy(isSpicy)
                                        .preparationTime(15)
                                        .available(true)
                                        .imageUrl(imageUrl)
                                        .rating(BigDecimal.valueOf(4.5))
                                        .reviewCount(12)
                                        .build();
                                foodItemRepository.save(newItem);
                                seededCount++;
                            }
                        }
                        log.info("Kaveri Menu database seeding finished. Total items in DB: {}", foodItemRepository.count());
                    } else {
                        log.warn("kaveri-menu.json resource could not be found for database seeding.");
                    }
                } catch (Exception e) {
                    log.error("Error seeding kaveri-menu.json into database", e);
                } finally {
                    if (is != null) {
                        try { is.close(); } catch (Exception ignored) {}
                    }
                }
            } else {
                log.info("Kaveri Menu database already contains {} items.", foodItemRepository.count());
            }
        };
    }
}
