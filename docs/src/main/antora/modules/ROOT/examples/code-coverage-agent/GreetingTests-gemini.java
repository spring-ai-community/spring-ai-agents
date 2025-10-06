package com.example.restservice;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class GreetingTests {

    @Test
    void greetingShouldHaveCorrectIdAndContent() {
        Greeting greeting = new Greeting(1L, "Hello, World!");
        assertThat(greeting.id()).isEqualTo(1L);
        assertThat(greeting.content()).isEqualTo("Hello, World!");
    }

    @Test
    void greetingEqualityShouldBeBasedOnIdAndContent() {
        Greeting greeting1 = new Greeting(1L, "Hello, World!");
        Greeting greeting2 = new Greeting(1L, "Hello, World!");
        Greeting greeting3 = new Greeting(2L, "Hello, World!");
        Greeting greeting4 = new Greeting(1L, "Hello, JUnit!");

        assertThat(greeting1).isEqualTo(greeting2);
        assertThat(greeting1).isNotEqualTo(greeting3);
        assertThat(greeting1).isNotEqualTo(greeting4);
    }

    @Test
    void greetingHashCodeShouldBeConsistent() {
        Greeting greeting1 = new Greeting(1L, "Hello, World!");
        Greeting greeting2 = new Greeting(1L, "Hello, World!");
        assertThat(greeting1.hashCode()).isEqualTo(greeting2.hashCode());
    }

    @Test
    void greetingToStringShouldContainIdAndContent() {
        Greeting greeting = new Greeting(1L, "Hello, World!");
        assertThat(greeting.toString()).contains("id=1", "content=Hello, World!");
    }
}
