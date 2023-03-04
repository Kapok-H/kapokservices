package com.kapok.customer;

import com.kapok.amqp.RabbitMQMessageProducer;
import com.kapok.clients.fraud.FraudCheckResponse;
import com.kapok.clients.fraud.FraudClient;
import com.kapok.clients.notification.NotificationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;


import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
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
    void itShouldGetAllCustomers() {
        // when
        underTest.getAllCustomers();
        // then
        verify(customerRepository).findAll();
    }

    @Test
    void isShouldSaveNewCustomer() {
        // given
        Customer customer = Customer.builder()
                .id(UUID.randomUUID())
                .firstNname("kapok")
                .lastNname("code")
                .email("kapokoffical@gmail.com")
                .phoneNumber(131)
                .build();
        // ... a request
        CustomerRegistrationRequest registrationRequest = new CustomerRegistrationRequest(customer);

        // ... No customer with phone number passed
        given(customerRepository.findCustomerByPhoneNumber(customer.getPhoneNumber()))
                .willReturn(Optional.empty());

        // ... Customer is not fraud
        given(fraudClient.isFraudster(customer.getId()))
                .willReturn(new FraudCheckResponse(false));
        // ... The customer successfully registers the notification request
        NotificationRequest notificationRequest = new NotificationRequest(
                customer.getId(),
                customer.getEmail(),
                String.format("Hi %s, welcome to kapok ...",
                        customer.getEmail())
        );

        // when
        underTest.registerCustomer(registrationRequest);

        // then
        then(customerRepository).should().save(customerArgumentCaptor.capture());
        then(fraudClient).should().isFraudster(customer.getId());

        then(rabbitMQMessageProducer).should().publish(
                notificationRequest,
                "internal.exchange",
                "internal.notification.routing-key"
        );
        Customer customerArgumentCaptorValue = customerArgumentCaptor.getValue();
        assertThat(customerArgumentCaptorValue).isEqualTo(customer);
    }

    @Test
    void itShouldNotSaveNewCustomerWhenCustomerExists() {
        // given
        Customer customer = Customer.builder()
                .id(UUID.randomUUID())
                .firstNname("kapok")
                .lastNname("code")
                .email("kapokoffical@gmail.com")
                .phoneNumber(131)
                .build();
        // ... a request
        CustomerRegistrationRequest registrationRequest = new CustomerRegistrationRequest(customer);

        // ... Customers who have already registered
        given(customerRepository.findCustomerByPhoneNumber(customer.getPhoneNumber()))
                .willReturn(Optional.of(customer));

        // ... customer is not fraud
        given(fraudClient.isFraudster(customer.getId()))
                .willReturn(new FraudCheckResponse(false));

        // When
        underTest.registerCustomer(registrationRequest);

        // Then
        then(customerRepository).should(never()).save(any());
    }

    @Test
    void isShouldNotSaveCustomerWhenCustomerExists() {
        // given
        Customer customer = Customer.builder()
                .id(UUID.randomUUID())
                .firstNname("kapok")
                .lastNname("code")
                .email("kapokoffical@gmail.com")
                .phoneNumber(131)
                .build();

        Customer customerTwo = Customer.builder()
                .id(UUID.randomUUID())
                .firstNname("john")
                .lastNname("math")
                .email("johnmath@gmail.com")
                .phoneNumber(131)
                .build();

        // ... a request
        CustomerRegistrationRequest registrationRequest = new CustomerRegistrationRequest(customer);

        // ... Customers whose email address has already been registered
        given(customerRepository.findCustomerByPhoneNumber(customer.getPhoneNumber()))
                .willReturn(Optional.of(customerTwo));

        // When
        // Then
        assertThatThrownBy(() ->  underTest.registerCustomer(registrationRequest))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(String.format("phone number [%s] is taken", customer.getPhoneNumber()));

        // Finally
        then(customerRepository).should(never()).save(any(Customer.class));
    }

    @Test
    void isShouldNotSaveCustomerWhenCustomerIsFrauder(){
        // given
        Customer customer = Customer.builder()
                .id(UUID.randomUUID())
                .firstNname("kapok")
                .lastNname("code")
                .email("kapokoffical@gmail.com")
                .phoneNumber(131)
                .build();
        // ... a request
        CustomerRegistrationRequest registrationRequest = new CustomerRegistrationRequest(customer);

        // ... No customer with phone number passed
        given(customerRepository.findCustomerByPhoneNumber(customer.getPhoneNumber()))
                .willReturn(Optional.empty());

        // ... Customer is not fraud
        given(fraudClient.isFraudster(customer.getId()))
                .willReturn(new FraudCheckResponse(true));

        // When
        // Then
        assertThatThrownBy(() -> underTest.registerCustomer(registrationRequest))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("fraudster exception");

        // Finally
        then(customerRepository).should(never()).save(any(Customer.class));
    }

    @Test
    void isShouldSaveCustomerWhenIdIsNull(){
        // given
        Customer customer = Customer.builder()
                .id(null)
                .firstNname("kapok")
                .lastNname("code")
                .email("kapokoffical@gmail.com")
                .phoneNumber(131)
                .build();

        Customer compareCustomer = Customer.builder()
                .id(null)
                .firstNname("kapok")
                .lastNname("code")
                .email("kapokoffical@gmail.com")
                .phoneNumber(131)
                .build();
        // ... a request
        CustomerRegistrationRequest request = new CustomerRegistrationRequest(customer);

        // ... No customer with phone number passed
        given(customerRepository.findCustomerByPhoneNumber(customer.getPhoneNumber()))
                .willReturn(Optional.empty());

        // when
        underTest.registerCustomer(request);

        // then
        then(customerRepository).should().save(customerArgumentCaptor.capture());
        Customer customerArgumentCaptorValue = customerArgumentCaptor.getValue();

        assertThat(customerArgumentCaptorValue).usingRecursiveComparison().ignoringFields("id").isEqualTo(compareCustomer);
        assertThat(customerArgumentCaptorValue.getId()).isNotNull();

    }
}