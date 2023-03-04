package com.kapok.customer;

import com.kapok.amqp.RabbitMQMessageProducer;
import com.kapok.clients.fraud.FraudCheckResponse;
import com.kapok.clients.fraud.FraudClient;
import com.kapok.clients.notification.NotificationRequest;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final FraudClient fraudClient;
    private final RabbitMQMessageProducer rabbitMQMessageProducer;
    private final CustomerDTOMapper CustomerDTOMapper;

    public List<CustomerDTO> getAllCustomers() {
        return customerRepository.findAll().stream()
                .map(CustomerDTOMapper).collect(Collectors.toList());
    }

    public void registerCustomer(CustomerRegistrationRequest request) {

        Customer customer = request.getCustomer();

        Integer phoneNumber = customer.getPhoneNumber();
        Optional<Customer> customerOptional = customerRepository.findCustomerByPhoneNumber(phoneNumber);
        if (customerOptional.isPresent()) {
            // make sure that's the exact same customer
            if (!customerOptional.get().getEmail().equals(customer.getEmail())) {
                throw new IllegalStateException(String.format("phone number [%s] is taken", phoneNumber));
            }
            // If duplicate commits occur, return directly
            return;
        }


        FraudCheckResponse fraudCheckResponse = fraudClient.isFraudster(customer.getId());

        if (fraudCheckResponse != null && fraudCheckResponse.isFraudster()) { // fraudCheckResponse != null is fix when unit test customer entity id is null situation
            throw new IllegalStateException("fraudster exception");
        }

        if(request.getCustomer().getId() == null)
            request.getCustomer().setId(UUID.randomUUID());

        customerRepository.save(customer);

        NotificationRequest notificationRequest = new NotificationRequest(
                customer.getId(),
                customer.getEmail(),
                String.format("Hi %s, welcome to kapok ...",
                        customer.getEmail())
        );

        rabbitMQMessageProducer.publish(
                notificationRequest,
                "internal.exchange",
                "internal.notification.routing-key"
        );
    }
}
