package com.kaveri.config;

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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataSeeder {

    @Bean
    public CommandLineRunner seedDatabase(
            RoleRepository roleRepository,
            UserRepository userRepository,
            CategoryRepository categoryRepository,
            FoodItemRepository foodItemRepository,
            PasswordEncoder passwordEncoder) {

        return args -> {
            // ---- Seed Roles ----
            if (roleRepository.count() == 0) {
                roleRepository.saveAll(List.of(
                        Role.builder().name(RoleName.ROLE_CUSTOMER).build(),
                        Role.builder().name(RoleName.ROLE_ADMIN).build()
                ));
                log.info("Roles seeded.");
            }

            Role adminRole = roleRepository.findByName(RoleName.ROLE_ADMIN).orElseThrow();
            Role customerRole = roleRepository.findByName(RoleName.ROLE_CUSTOMER).orElseThrow();

            // ---- Seed Admin User ----
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

            // ---- Seed Sample Customer ----
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

            // ---- Seed Categories ----
            if (categoryRepository.count() == 0) {
                List<Category> categories = categoryRepository.saveAll(List.of(
                        Category.builder().name("Kaveri Specials & Thalis").description("Signature multi-course royal thalis and chef specials").imageUrl("/images/vegthali.jpg").build(),
                        Category.builder().name("South Indian").description("Crispy authentic dosas, uttapams and idlis").imageUrl("/images/fastfood.jpg").build(),
                        Category.builder().name("North Indian Curries").description("Rich rich paneer gravies and slow-simmered dals").imageUrl("/img/home1-main-image-1.jpg").build(),
                        Category.builder().name("Tandoor & Starters").description("Charcoal roasted kebabs, paneer tikka and hot starters").imageUrl("/img/home1-main-image-4.jpg").build(),
                        Category.builder().name("Punjab Sweet House").description("Heritage Indian mithai and confections since 1947").imageUrl("/images/punjaabsweethouse.jpg").build(),
                        Category.builder().name("Breads & Biryani").description("Tandoori naans, rotis and fragrant dum biryani").imageUrl("/img/home1-main-image-5.jpg").build(),
                        Category.builder().name("Chinese Delicacies").description("Wok-tossed noodles, chilli paneer and fried rice").imageUrl("/img/main-home-slide3.jpg").build(),
                        Category.builder().name("Beverages & Shakes").description("Kaveri special thick lassi, shakes and coolers").imageUrl("/images/ice4.jpg").build()
                ));
                log.info("Kaveri Categories seeded.");

                // ---- Seed Food Items ----
                if (foodItemRepository.count() == 0) {
                    Category thalis = categories.get(0);
                    Category southIndian = categories.get(1);
                    Category northIndian = categories.get(2);
                    Category tandoor = categories.get(3);
                    Category sweets = categories.get(4);
                    Category breadsRice = categories.get(5);
                    Category chinese = categories.get(6);
                    Category beverages = categories.get(7);

                    foodItemRepository.saveAll(List.of(
                        // Thalis & Specials
                        FoodItem.builder().name("Kaveri Royal Executive Thali").description("Special Paneer Butter Masala, Dal Makhani, Seasonal Veg, Jeera Rice, 2 Butter Naan, Sweet, Raita, Salad & Papad")
                            .price(new BigDecimal("349")).category(thalis).vegetarian(true).spicy(false).preparationTime(20).available(true).build(),
                        FoodItem.builder().name("Kaveri Special Veg Thali").description("Paneer Gravy, Yellow Dal Tadka, Mixed Vegetable, Steamed Basmati Rice, 3 Phulkas, Pickle & Gulab Jamun")
                            .price(new BigDecimal("269")).category(thalis).vegetarian(true).spicy(false).preparationTime(15).available(true).build(),

                        // South Indian
                        FoodItem.builder().name("Kaveri Special Butter Masala Dosa").description("Crispy golden crepe roasted in pure butter, stuffed with spiced potato masala, served with 3 chutneys & sambar")
                            .price(new BigDecimal("179")).category(southIndian).vegetarian(true).spicy(false).preparationTime(12).available(true).build(),
                        FoodItem.builder().name("Mysore Cheese Masala Dosa").description("Crispy dosa smeared with red garlic chutney, loaded with processed cheese and aloo masala")
                            .price(new BigDecimal("219")).category(southIndian).vegetarian(true).spicy(true).preparationTime(15).available(true).build(),
                        FoodItem.builder().name("Steamed Button Idli Sambar").description("Feather-soft steamed rice cakes submerged in piping hot piping sambar with coconut chutney")
                            .price(new BigDecimal("119")).category(southIndian).vegetarian(true).spicy(false).preparationTime(8).available(true).build(),

                        // North Indian Curries
                        FoodItem.builder().name("Paneer Butter Masala").description("Fresh cottage cheese cubes simmered in velvety butter-tomato gravy with rich cashew paste and kasuri methi")
                            .price(new BigDecimal("299")).category(northIndian).vegetarian(true).spicy(false).preparationTime(18).available(true).build(),
                        FoodItem.builder().name("Dal Makhani (Kaveri Signature)").description("Black lentils and kidney beans slow-cooked overnight with churned butter and fresh cream")
                            .price(new BigDecimal("249")).category(northIndian).vegetarian(true).spicy(false).preparationTime(15).available(true).build(),
                        FoodItem.builder().name("Kadhai Paneer").description("Paneer tossed with crunchy bell peppers, whole coriander seeds, and crushed Kashmiri red chillies")
                            .price(new BigDecimal("289")).category(northIndian).vegetarian(true).spicy(true).preparationTime(18).available(true).build(),
                        FoodItem.builder().name("Malai Kofta").description("Melt-in-mouth cottage cheese and khoya dumplings served in a rich royal white cashew gravy")
                            .price(new BigDecimal("319")).category(northIndian).vegetarian(true).spicy(false).preparationTime(20).available(true).build(),

                        // Tandoor & Starters
                        FoodItem.builder().name("Tandoori Paneer Tikka").description("Marinated cottage cheese char-grilled in clay tandoor with onions and capsicum, served with mint chutney")
                            .price(new BigDecimal("279")).category(tandoor).vegetarian(true).spicy(true).preparationTime(20).available(true).build(),
                        FoodItem.builder().name("Hara Bhara Kebab").description("Pan-fried spinach, green pea and potato patties spiced with cardamom and royal cumin")
                            .price(new BigDecimal("219")).category(tandoor).vegetarian(true).spicy(false).preparationTime(15).available(true).build(),

                        // Punjab Sweet House
                        FoodItem.builder().name("Punjab Sweet House Gulab Jamun (2 Pcs)").description("Iconic heritage recipe: melt-in-mouth golden khoya dumplings soaked in saffron-cardamom syrup")
                            .price(new BigDecimal("99")).category(sweets).vegetarian(true).spicy(false).preparationTime(5).available(true).build(),
                        FoodItem.builder().name("Kesar Rasmalai (2 Pcs)").description("Spongy cottage cheese patties immersed in chilled condensed milk infused with Kashmiri saffron and pistachios")
                            .price(new BigDecimal("129")).category(sweets).vegetarian(true).spicy(false).preparationTime(5).available(true).build(),
                        FoodItem.builder().name("Special Motichoor Ladoo (250g Box)").description("Pure desi ghee tiny pearl boondi laddus made fresh daily")
                            .price(new BigDecimal("169")).category(sweets).vegetarian(true).spicy(false).preparationTime(5).available(true).build(),

                        // Breads & Biryani
                        FoodItem.builder().name("Kaveri Special Veg Dum Biryani").description("Slow-cooked aromatic basmati rice layered with garden vegetables, saffron and royal spices, served with raita")
                            .price(new BigDecimal("289")).category(breadsRice).vegetarian(true).spicy(true).preparationTime(25).available(true).build(),
                        FoodItem.builder().name("Butter Naan").description("Clay oven baked refined flour bread brushed with generous dollop of butter")
                            .price(new BigDecimal("59")).category(breadsRice).vegetarian(true).spicy(false).preparationTime(8).available(true).build(),

                        // Chinese
                        FoodItem.builder().name("Chilli Paneer Dry").description("Crispy paneer cubes wok-tossed with green chillies, garlic, capsicum and soya glaze")
                            .price(new BigDecimal("259")).category(chinese).vegetarian(true).spicy(true).preparationTime(15).available(true).build(),
                        FoodItem.builder().name("Veg Hakka Noodles").description("Thin wok-tossed noodles with julienned vegetables and mild Chinese spices")
                            .price(new BigDecimal("189")).category(chinese).vegetarian(true).spicy(false).preparationTime(12).available(true).build(),

                        // Beverages
                        FoodItem.builder().name("Kaveri Royal Sweet Lassi").description("Traditional thick churned Punjabi yogurt lassi topped with malai, saffron, and crushed almonds")
                            .price(new BigDecimal("99")).category(beverages).vegetarian(true).spicy(false).preparationTime(5).available(true).build(),
                        FoodItem.builder().name("Fresh Mint Lime Soda").description("Refreshing sparkling cooler with fresh lime juice, crushed mint sprigs, and roasted cumin salt")
                            .price(new BigDecimal("79")).category(beverages).vegetarian(true).spicy(false).preparationTime(3).available(true).build()
                    ));
                    log.info("Kaveri Food items seeded (19 iconic items).");
                }
            }
        };
    }
}
