package com.kapok.customer;

import com.kapok.amqp.RabbitMQMessageProducer;
import com.kapok.clients.fraud.FraudCheckResponse;
import com.kapok.clients.fraud.FraudClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;


import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.verify;

class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private FraudClient fraudClient;
    @Mock
    private RabbitMQMessageProducer rabbitMQMessageProducer;
    @Mock
    private CustomerDTOMapper customerDTOMapper;

    @Captor
    private ArgumentCaptor<Customer> customerArgumentCaptor;

    private CustomerService underTest;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        underTest = new CustomerService(
                customerRepository,
                fraudClient,
                rabbitMQMessageProducer,
                customerDTOMapper);
    }

    @Test
    void itShouldGetAllCustomers(){
        // when
        underTest.getAllCustomers();
        // then
        verify(customerRepository).findAll();
    }

    @Test
    void itShouldSaveNewCustomer(){
        // given
        UUID id = UUID.randomUUID();
        Customer customer = Customer.builder()
                .id(id)
                .firstNname("kapok")
                .lastNname("code")
                .email("kapokoffical@gmail.com")
                .phoneNumber("131xxx")
                .build();
        // ... a request
        CustomerRegistrationRequest registrationRequest = new CustomerRegistrationRequest(customer);

        // ... No customer with phone number passed
        given(customerRepository.findCustomerByPhoneNumber(customer.getPhoneNumber()))
                .willReturn(Optional.empty());

        // ... customer is not fraud
        given(fraudClient.isFraudster(customer.getId()))
                .willReturn(new FraudCheckResponse(false));

        // when
        underTest.registerCustomer(registrationRequest);

        // then
        then(customerRepository).should().save(customerArgumentCaptor.capture());
        Customer customerArgumentCaptorValue = customerArgumentCaptor.getValue();
        assertThat(customerArgumentCaptorValue).isEqualTo(customer);
    }
}