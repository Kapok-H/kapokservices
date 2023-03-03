package com.kapok.customer;

import org.springframework.stereotype.Component;

import java.util.function.Function;

@Component
public class CustomerDTOMapper implements Function<Customer, CustomerDTO> {

    @Override
    public CustomerDTO apply(Customer customer) {
        return new CustomerDTO(
                customer.getId(),
                customer.getFirstNname(),
                customer.getLastNname(),
                customer.getPhoneNumber(),
                customer.getEmail()
        );
    }
}
