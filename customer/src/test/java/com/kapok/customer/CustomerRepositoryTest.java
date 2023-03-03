package com.kapok.customer;


import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        properties = {
                "spring.jpa.properties.javax.persistence.validation.mode=none"
        }
)
class CustomerRepositoryTest {

    @Autowired
    private CustomerRepository underTest;

    @Test
    void isShouldFindCustomerByPhoneNumber() {
        // Given
        // When
        // Then
    }
    @Test
    void isShouldSaveCustomer() {
        // Given
        UUID id = UUID.randomUUID();
        Customer customer = Customer.builder()
                .id(id)
                .firstNname("kapok")
                .lastNname("code")
                .email("kapokoffical@gmail.com")
                .phoneNumber("131xxx")
                .build();

        //When
        underTest.save(customer);

        //Then
        Optional<Customer> optionalCustomer = underTest.findById(id);
        assertThat(optionalCustomer)
                .isPresent()
                .hasValueSatisfying(c -> {
                    assertThat(c.getId()).isEqualTo(id);
                    assertThat(c.getFirstNname()).isEqualTo("kapok");
                    assertThat(c.getLastNname()).isEqualTo("code");
                    assertThat(c.getPhoneNumber()).isEqualTo("131xxx");
                });
    }
}