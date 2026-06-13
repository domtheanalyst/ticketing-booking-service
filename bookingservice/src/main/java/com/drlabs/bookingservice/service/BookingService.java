package com.drlabs.bookingservice.service;

import com.drlabs.bookingservice.client.InventoryServiceClient;
import com.drlabs.bookingservice.entity.Customer;
import com.drlabs.bookingservice.repository.CustomerRepository;
import com.drlabs.bookingservice.request.BookingRequest;
import com.drlabs.bookingservice.response.BookingResponse;
import com.drlabs.bookingservice.response.InventoryResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BookingService {

    private final CustomerRepository customerRepository;
    private final InventoryServiceClient inventoryServiceClient;


    @Autowired
    public BookingService(CustomerRepository customerRepository, InventoryServiceClient inventoryServiceClient) {
        this.customerRepository = customerRepository;
        this.inventoryServiceClient = inventoryServiceClient;
    }

    public BookingResponse createBooking(final BookingRequest request) {
        // check if the user exists
        final Customer customer = customerRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        //check if there is enough inventory/ticket to be sold to the user
        final InventoryResponse inventoryResponse = inventoryServiceClient.getInventory(request.getEventId());
        //debug
        System.out.println("Inventory Service Response: " + inventoryResponse);
        if (inventoryResponse.getCapacity() < request.getTicketCount()) {
            throw new RuntimeException("Not enough tickets available");
        }

        //create booking

        // send booking to Order Service on a Kafka topic
        return BookingResponse.builder()
                .build();
    }
}
