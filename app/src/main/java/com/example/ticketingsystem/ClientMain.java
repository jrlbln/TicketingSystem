package com.example.ticketingsystem;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ClientMain extends AppCompatActivity {

    private TextView textViewClientName, textViewTicketNumber, textViewService, textViewLatestNumberEdit, textViewCurrentNumberEdit;
    private Spinner spinnerServices;
    private Button buttonGenerateTicket;
    private List<String> servicesList;
    private List<String> servicesListForInitialLoading;
    private CustomSpinnerAdapter spinnerAdapter;
    private ListenerRegistration servicesListenerRegistration;
    private boolean isInitialLoadingComplete = false;
    private int latestTicketNumber = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client_main);

        textViewClientName = findViewById(R.id.textViewClientName);
        textViewTicketNumber = findViewById(R.id.textViewTicketNumber);
        textViewService = findViewById(R.id.textViewService);
        spinnerServices = findViewById(R.id.spinnerServices);
        buttonGenerateTicket = findViewById(R.id.buttonGenerateTicket);
        textViewLatestNumberEdit = findViewById(R.id.textViewLatestNumberEdit);
        textViewCurrentNumberEdit = findViewById(R.id.textViewCurrentNumberEdit);

        servicesListForInitialLoading = new ArrayList<>();
        servicesList = new ArrayList<>();

        spinnerAdapter = new CustomSpinnerAdapter(this, servicesList);
        spinnerServices.setAdapter(spinnerAdapter);

        spinnerServices.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedService = (String) parent.getItemAtPosition(position);
                if (isInitialLoadingComplete) {
                    textViewService.setText(selectedService);
                    listenForTicketChanges(selectedService.toLowerCase());
                } else {
                    textViewService.setText("Please Select a Service: ");
                }
                isInitialLoadingComplete = true;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        loadServicesFromFirestore();

        setupRealTimeListener();

        buttonGenerateTicket.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                generateTicket();
            }
        });
    }

    private void loadServicesFromFirestore() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference servicesCollection = db.collection("services");
        servicesCollection.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    servicesListForInitialLoading.clear();
                    for (DocumentChange documentChange : task.getResult().getDocumentChanges()) {
                        if (documentChange.getType() == DocumentChange.Type.ADDED) {
                            String serviceName = documentChange.getDocument().getString("serviceName");
                            servicesListForInitialLoading.add(serviceName);
                        }
                    }
                    // Update the adapter with initial loading data
                    servicesList.clear();
                    servicesList.addAll(servicesListForInitialLoading);
                    spinnerAdapter.notifyDataSetChanged();
                } else {
                    // Handle error if loading fails
                    textViewService.setText("Failed to load services");
                }
            }
        });
    }

    private void setupRealTimeListener() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference servicesCollection = db.collection("services");
        servicesListenerRegistration = servicesCollection.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@NonNull QuerySnapshot snapshot, @NonNull FirebaseFirestoreException e) {
                if (e != null) {
                    // Handle any errors that occurred during the listening process
                    Toast.makeText(ClientMain.this, "Error fetching services: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    return;
                }

                Set<String> updatedServices = new HashSet<>();

                // Loop through the documents in the snapshot and update the list of services
                for (DocumentChange documentChange : snapshot.getDocumentChanges()) {
                    String serviceName = documentChange.getDocument().getString("serviceName");
                    if (documentChange.getType() == DocumentChange.Type.ADDED) {
                        // Add the service to the updated set
                        updatedServices.add(serviceName);
                    } else if (documentChange.getType() == DocumentChange.Type.REMOVED) {
                        // Remove the service from the list and adapter
                        servicesList.remove(serviceName);
                        spinnerAdapter.notifyDataSetChanged();
                    }
                }

                // Add new services to the list
                servicesList.addAll(updatedServices);

                // Update the adapter with real-time update data
                spinnerAdapter.notifyDataSetChanged();
            }
        });
    }

    private void listenForTicketChanges(String selectedService) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference servicesCollection = db.collection("services");
        CollectionReference ticketsCollection = servicesCollection.document(selectedService).collection("tickets");
        ticketsCollection.orderBy("ticketNumber", Query.Direction.DESCENDING).limit(1).addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@NonNull QuerySnapshot snapshot, @NonNull FirebaseFirestoreException e) {
                if (e != null) {
                    // Handle any errors that occurred during the listening process
                    Toast.makeText(ClientMain.this, "Error fetching tickets: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    return;
                }

                // Check if there are any tickets in the collection
                if (!snapshot.isEmpty()) {
                    String lastTicketNumber = snapshot.getDocuments().get(0).getString("ticketNumber");
                    try {
                        latestTicketNumber = Integer.parseInt(lastTicketNumber);
                        textViewLatestNumberEdit.setText(String.format("%03d", latestTicketNumber));

                        // Real-time listener for the currentTicket field
                        servicesCollection.document(selectedService).addSnapshotListener(new EventListener<DocumentSnapshot>() {
                            @Override
                            public void onEvent(@javax.annotation.Nullable DocumentSnapshot documentSnapshot, @javax.annotation.Nullable FirebaseFirestoreException e) {
                                if (e != null) {
                                    // Handle any errors that occurred during the listening process
                                    Toast.makeText(ClientMain.this, "Error fetching current ticket: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                if (documentSnapshot != null && documentSnapshot.exists()) {
                                    String currentTicketNumber = documentSnapshot.getString("currentTicket");
                                    if (currentTicketNumber != null) {
                                        textViewCurrentNumberEdit.setText(currentTicketNumber);
                                    } else {
                                        textViewCurrentNumberEdit.setText("No Current Ticket");
                                    }
                                }
                            }
                        });
                    } catch (NumberFormatException ex) {
                        // Handle error if the ticket number is not a valid integer
                        ex.printStackTrace();
                    }
                } else {
                    // If no tickets found, set the latestTicketNumber to 0 and update textViewLatestNumberEdit
                    latestTicketNumber = 0;
                    textViewLatestNumberEdit.setText(String.format("%03d", latestTicketNumber));
                }
            }
        });

        setupRealTimeListenerForCurrentTicket(selectedService);
    }

    private void setupRealTimeListenerForCurrentTicket(String selectedService) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference serviceDocument = db.collection("services").document(selectedService);

        serviceDocument.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@javax.annotation.Nullable DocumentSnapshot snapshot, @javax.annotation.Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    // Handle any errors that occurred during the listening process
                    Toast.makeText(ClientMain.this, "Error fetching service data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    return;
                }

                if (snapshot != null && snapshot.exists()) {
                    String currentTicketNumber = snapshot.getString("currentTicket");
                    if (currentTicketNumber != null && !currentTicketNumber.isEmpty()) {
                        textViewCurrentNumberEdit.setText(currentTicketNumber);
                        checkForTurn(currentTicketNumber);
                    } else {
                        textViewCurrentNumberEdit.setText("N/A");
                    }
                }
            }
        });
    }

    private void checkForTurn(String currentTicketNumber) {
        String generatedTicketNumber = textViewTicketNumber.getText().toString();
        if (generatedTicketNumber.equals(currentTicketNumber)) {
            Toast.makeText(ClientMain.this, "It's your turn! Your ticket number is " + currentTicketNumber, Toast.LENGTH_SHORT).show();
        }
    }

    private void generateTicket() {
        // Get the selected service from the spinner
        String selectedService = (String) spinnerServices.getSelectedItem();

        if (selectedService == null || selectedService.isEmpty()) {
            Toast.makeText(this, "Please select a service first.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Convert the selected service to lowercase
        selectedService = selectedService.toLowerCase();

        // Create a new ticket document under the "tickets" collection for the selected service
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference servicesCollection = db.collection("services");
        CollectionReference ticketsCollection = servicesCollection.document(selectedService).collection("tickets");

        // Query the tickets collection to find the last ticket number
        ticketsCollection.orderBy("timestamp", Query.Direction.DESCENDING).limit(1).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    int ticketNumber = 1; // Default ticket number if no tickets found

                    // Check if any ticket exists in the collection
                    if (!task.getResult().isEmpty()) {
                        // Get the last ticket number and increment it
                        String lastTicketNumber = task.getResult().getDocuments().get(0).getString("ticketNumber");
                        try {
                            ticketNumber = Integer.parseInt(lastTicketNumber) + 1;
                        } catch (NumberFormatException e) {
                            // Handle error if the ticket number is not a valid integer
                            e.printStackTrace();
                        }
                    }

                    // Format the ticket number as a 3-digit string (e.g., 001)
                    String ticketNumberString = String.format("%03d", ticketNumber);

                    // Create a new Ticket object with the server's timestamp
                    Ticket ticket = new Ticket(ticketNumberString, FieldValue.serverTimestamp());

                    // Use the ticket number as the document name when adding the ticket to the "tickets" collection
                    int finalTicketNumber = ticketNumber;
                    ticketsCollection.document(ticketNumberString)
                            .set(ticket)
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()) {
                                        // Update the UI with the generated ticket information
                                        textViewTicketNumber.setText(ticketNumberString);

                                        // Update the latest ticket number displayed
                                        latestTicketNumber = finalTicketNumber;
                                        textViewLatestNumberEdit.setText(String.format("%03d", latestTicketNumber));

                                        // Check if the generated ticket number matches the current ticket number
                                        String currentTicketNumber = textViewCurrentNumberEdit.getText().toString();
                                        if (ticketNumberString.equals(currentTicketNumber)) {
                                            Toast.makeText(ClientMain.this, "It's your turn! Your ticket number is " + currentTicketNumber, Toast.LENGTH_SHORT).show();
                                        } else {
                                            Toast.makeText(ClientMain.this, "Ticket generated successfully. Your ticket number is " + ticketNumberString, Toast.LENGTH_SHORT).show();
                                        }
                                    } else {
                                        // Handle error if adding the ticket fails
                                        Toast.makeText(ClientMain.this, "Failed to generate ticket.", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                } else {
                    // Handle error if fetching last ticket fails
                    Toast.makeText(ClientMain.this, "Failed to generate ticket.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
