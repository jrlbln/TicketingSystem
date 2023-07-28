package com.example.ticketingsystem;

public class Ticket {
    private String ticketNumber;
    private long timestamp;

    public Ticket() {
    }

    public Ticket(String ticketNumber, long timestamp) {
        this.ticketNumber = ticketNumber;
        this.timestamp = timestamp;
    }

    public String getTicketNumber() {
        return ticketNumber;
    }

    public void setTicketNumber(String ticketNumber) {
        this.ticketNumber = ticketNumber;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}