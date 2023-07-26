package com.example.ticketingsystem;

public class Service {
    private String serviceName;

    public Service() {
        // Empty constructor needed for Firestore
    }

    public Service(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }
}

