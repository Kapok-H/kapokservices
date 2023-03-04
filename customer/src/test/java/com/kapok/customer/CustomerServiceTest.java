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
    void itShouldSaveNewCustomer() {
        // given
        // ... a request
        CustomerRegistrationRequest request = new CustomerRegistrationRequest(
                UUID.randomUUID(),
                "kapok",
                "code",
                131,
                "kapoktest@gmail.com"
        );

        // ... No customer with phone number passed
        given(customerRepository.findCustomerByPhoneNumber(request.phoneNumber()))
                .willReturn(Optional.empty());

        // ... Customer is not fraud
        given(fraudClient.isFraudster(request.id()))
                .willReturn(new FraudCheckResponse(false));
        // ... The customer successfully registers the notification request
        NotificationRequest notificationRequest = new NotificationRequest(
                request.id(),
                request.email(),
                String.format("Hi %s, welcome to kapok ...",
                        request.email())
        );

        // when
        underTest.registerCustomer(request);

        // then
        then(customerRepository).should().save(customerArgumentCaptor.capture());
        then(fraudClient).should().isFraudster(request.id());

        then(rabbitMQMessageProducer).should().publish(
                notificationRequest,
                "internal.exchange",
                "internal.notification.routing-key"
        );
        Customer customerArgumentCaptorValue = customerArgumentCaptor.getValue();
        assertThat(customerArgumentCaptorValue).usingRecursiveComparison().ignoringFields("id").isEqualTo(request);
    }

    @Test
    void itShouldNotSaveNewCustomerWhenCustomerExists() {
        // given
        CustomerRegistrationRequest request = new CustomerRegistrationRequest(
                UUID.randomUUID(),
                "kapok",
                "code",
                131,
                "kapoktest@gmail.com"
        );
        Customer customer = Customer.builder()
                .id(request.id())
                .firstName(request.firstName())
                .lastName(request.lastName())
                .email(request.email())
                .phoneNumber(request.phoneNumber())
                .build();

        // ... Customers who have already registered
        given(customerRepository.findCustomerByPhoneNumber(request.phoneNumber()))
                .willReturn(Optional.of(customer));

        // ... customer is not fraud
        given(fraudClient.isFraudster(customer.getId()))
                .willReturn(new FraudCheckResponse(false));

        // When
        underTest.registerCustomer(request);

        // Then
        then(customerRepository).should(never()).save(any());
    }

    @Test
    void itShouldNotSaveCustomerWhenCustomerExists() {
        CustomerRegistrationRequest request = new CustomerRegistrationRequest(
                UUID.randomUUID(),
                "kapok",
                "code",
                131,
                "kapoktest@gmail.com"
        );

        Customer customerTwo = Customer.builder()
                .id(request.id())
                .firstName(request.firstName())
                .lastName(request.lastName())
                .email("johnmath@gmail.com")
                .phoneNumber(request.phoneNumber())
                .build();

        // ... Customers whose email address has already been registered
        given(customerRepository.findCustomerByPhoneNumber(request.phoneNumber()))
                .willReturn(Optional.of(customerTwo));

        // When
        // Then
        assertThatThrownBy(() ->  underTest.registerCustomer(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(String.format("phone number [%s] is taken", request.phoneNumber()));

        // Finally
        then(customerRepository).should(never()).save(any(Customer.class));
    }

    @Test
    void itShouldNotSaveCustomerWhenCustomerIsFrauder(){
        // given
        CustomerRegistrationRequest request = new CustomerRegistrationRequest(
                UUID.randomUUID(),
                "kapok",
                "code",
                131,
                "kapoktest@gmail.com"
        );

        // ... No customer with phone number passed
        given(customerRepository.findCustomerByPhoneNumber(request.phoneNumber()))
                .willReturn(Optional.empty());

        // ... Customer is not fraud
        given(fraudClient.isFraudster(request.id()))
                .willReturn(new FraudCheckResponse(true));

        // When
        // Then
        assertThatThrownBy(() -> underTest.registerCustomer(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("fraudster exception");

        // Finally
        then(customerRepository).should(never()).save(any(Customer.class));
    }

    @Test
    void itShouldSaveCustomerWhenIdIsNull(){
        CustomerRegistrationRequest request = new CustomerRegistrationRequest(
                null,
                "kapok",
                "code",
                131,
                "kapoktest@gmail.com"
        );

        // ... No customer with phone number passed
        given(customerRepository.findCustomerByPhoneNumber(request.phoneNumber()))
                .willReturn(Optional.empty());

        // when
        underTest.registerCustomer(request);

        // then
        then(customerRepository).should().save(customerArgumentCaptor.capture());
        Customer customerArgumentCaptorValue = customerArgumentCaptor.getValue();

        assertThat(customerArgumentCaptorValue).usingRecursiveComparison().ignoringFields("id").isEqualTo(request);
        assertThat(customerArgumentCaptorValue.getId()).isNotNull();

    }
}