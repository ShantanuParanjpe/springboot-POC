package com.example;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

 class MainTest {

    @Test
    void testHello() {
        Main main = new Main();
        assertEquals("Hello World", main.hello());
    }
}