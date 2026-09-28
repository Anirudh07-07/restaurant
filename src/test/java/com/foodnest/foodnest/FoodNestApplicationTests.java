package com.foodnest.foodnest;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class FoodNestApplicationTests {

    @Test
    void contextLoads() {
        // Verifies the Spring application context loads without errors
    }
}
