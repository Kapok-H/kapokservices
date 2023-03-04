package com.kapok.customer;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import javax.persistence.*;
import java.util.UUID;


@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@JsonIgnoreProperties(value = {"id"}, allowGetters = true)
public class Customer {
    @Id
    private UUID id;
    @Column(nullable = false)
    private String firstNname;
    @Column(nullable = false)
    private String lastNname;
    @Column(nullable = false, unique = true)
    private Integer phoneNumber;
    @Column(nullable = false)
    private String email;
}
