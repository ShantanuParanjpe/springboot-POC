package com.example;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class MainTest {

    @Test
    void testHello() {
        Main main = new Main();
        assertEquals("Hello World", main.hello());
    }
}