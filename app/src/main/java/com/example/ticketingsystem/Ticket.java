package com.example.ticketingsystem;

public class Ticket {
    private String ticketNumber;
    private Object timestamp;
    private String tellerName;

    public Ticket() {
        // Required empty constructor for Firestore
    }

    public Ticket(String ticketNumber, Object timestamp) {
        this.ticketNumber = ticketNumber;
        this.timestamp = timestamp;
    }

    public Ticket(String ticketNumber, Object timestamp, String tellerName) {
        this.ticketNumber = ticketNumber;
        this.timestamp = timestamp;
        this.tellerName = tellerName;
    }

    public String getTicketNumber() {
        return ticketNumber;
    }

    public Object getTimestamp() {
        return timestamp;
    }

    public String getTellerName() {
        return tellerName;
    }
}