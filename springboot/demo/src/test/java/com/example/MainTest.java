package com.example;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class HelloControllerTest {

    @Test
    void testHello() {
        HelloController controller = new HelloController();
        assertEquals("Hello World", controller.hello());
    }
}