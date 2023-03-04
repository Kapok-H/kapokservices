package com.kapok.customer;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
public class CustomerDTO {
    private UUID id;
    private String firstName;
    private String lastName;
    private Integer phoneNumber;
    private String email;
}
