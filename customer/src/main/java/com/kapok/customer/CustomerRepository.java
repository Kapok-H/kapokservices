package com.kapok.customer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    @Query("select c from Customer c where c.phoneNumber = ?1")
    Optional<Customer> findCustomerByPhoneNumber(Integer phoneNumber);

}
