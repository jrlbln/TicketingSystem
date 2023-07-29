package com.example.ticketingsystem;

import com.google.firebase.firestore.FieldValue;
public class Ticket {
    private String ticketNumber;
    private Object timestamp; // Use Object to store the timestamp as Firestore accepts different timestamp types

    public Ticket() {
        // Required empty constructor for Firestore
    }

    public Ticket(String ticketNumber, Object timestamp) {
        this.ticketNumber = ticketNumber;
        this.timestamp = timestamp;
    }

    public String getTicketNumber() {
        return ticketNumber;
    }

    public Object getTimestamp() {
        return timestamp;
    }
}
