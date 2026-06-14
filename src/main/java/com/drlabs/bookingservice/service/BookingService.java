package com.drlabs.bookingservice.service;

import com.drlabs.bookingservice.client.InventoryServiceClient;
import com.drlabs.bookingservice.entity.Customer;
import com.drlabs.bookingservice.event.BookingEvent;
import com.drlabs.bookingservice.repository.CustomerRepository;
import com.drlabs.bookingservice.request.BookingRequest;
import com.drlabs.bookingservice.response.BookingResponse;
import com.drlabs.bookingservice.response.InventoryResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@Slf4j //for logging
public class BookingService {

    private final CustomerRepository customerRepository;
    private final InventoryServiceClient inventoryServiceClient;
    private final KafkaTemplate<String, BookingEvent> kafkaTemplate;


    @Autowired
    public BookingService(final CustomerRepository customerRepository,
                          final InventoryServiceClient inventoryServiceClient,
                          final KafkaTemplate<String, BookingEvent> kafkaTemplate) {

        this.customerRepository = customerRepository;
        this.inventoryServiceClient = inventoryServiceClient;
        this.kafkaTemplate = kafkaTemplate;

    } // constructor


    public BookingResponse createBooking(final BookingRequest request) {

        // check if the user exists
        final Customer customer = customerRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        //check if there is enough inventory/ticket to be sold to the user
        final InventoryResponse inventoryResponse = inventoryServiceClient.getInventory(request.getEventId());

        //debug
        // System.out.println("Inventory Service Response: " + inventoryResponse);
        log.info("Inventory Service Response: {}", inventoryResponse);

        //check if there is enough inventory
        if (inventoryResponse.getCapacity() < request.getTicketCount()) {
            throw new RuntimeException("Not enough tickets available");
        }

        //create booking event
        final BookingEvent bookingEvent = createBookingEvent(request, customer, inventoryResponse);

        // send booking to Order Service on a Kafka topic
        kafkaTemplate.send("booking-topic", bookingEvent);
        log.info("Booking sent to Kafka: {}", bookingEvent);
        return BookingResponse.builder()
                .userId(bookingEvent.getUserId())
                .eventId(bookingEvent.getEventId())
                .ticketCount(bookingEvent.getTicketCount())
                .totalPrice(bookingEvent.getTotalPrice())
                .build();

    } // createBooking


    private BookingEvent createBookingEvent(final BookingRequest request,
                                            final Customer customer,final InventoryResponse inventoryResponse) {

            return BookingEvent.builder()
                    .userId(customer.getId())
                    .eventId(request.getEventId())
                    .ticketCount(request.getTicketCount())
                    .totalPrice(inventoryResponse.getTicketPrice().multiply(BigDecimal.valueOf(request.getTicketCount())))
                    .build();

    }// createBookingEvent()



}// BookingService
