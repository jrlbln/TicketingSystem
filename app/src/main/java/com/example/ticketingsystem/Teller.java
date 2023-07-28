package com.example.ticketingsystem;

public class Teller {
    private String name;
    private String currentTicket;

    public Teller() {
        // Empty constructor required for Firestore
    }

    public Teller(String name, String currentTicket) {
        this.name = name;
        this.currentTicket = currentTicket;
    }

    public String getName() {
        return name;
    }

    public String getCurrentTicket() {
        return currentTicket;
    }
}

