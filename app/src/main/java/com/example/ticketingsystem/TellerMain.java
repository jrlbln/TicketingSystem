package com.example.ticketingsystem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FieldValue;

import java.util.ArrayList;
import java.util.List;

public class TellerMain extends AppCompatActivity {

    private FirebaseFirestore db;
    private TextView textViewServiceName;
    private TextView textViewTicketNumber;
    private List<String> ticketNumbers;
    private ArrayAdapter<String> ticketAdapter;
    private String assignedService, tellerName ;
    private String currentTicketNumber = "0";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teller_main);

        db = FirebaseFirestore.getInstance();
        textViewServiceName = findViewById(R.id.textViewServiceName);
        textViewTicketNumber = findViewById(R.id.textViewTicketNumber);
        ListView ticketsList = findViewById(R.id.ticketsList);

        Button buttonCallNext = findViewById(R.id.buttonCallNext);
        Button buttonCallAgain = findViewById(R.id.buttonCallAgain);
        Button buttonCallPrevious = findViewById(R.id.buttonCallPrevious);

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigation);
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_call:
                        return true;
                    case R.id.action_transfer:
                        startActivity(new Intent(TellerMain.this, TellerTransfer.class));
                        finish();
                        return true;
                    default:
                        return false;
                }
            }
        });

        bottomNavigationView.setSelectedItemId(R.id.action_call);

        ticketNumbers = new ArrayList<>();
        ticketAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, ticketNumbers);
        ticketsList.setAdapter(ticketAdapter);

        String tellerUID = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Retrieve the service assigned to the teller from Firestore
        db.collection("users")
                .document(tellerUID)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        DocumentSnapshot documentSnapshot = task.getResult();
                        assignedService = documentSnapshot.getString("service");
                        tellerName = documentSnapshot.getString("name");
                        if (assignedService != null) {
                            textViewServiceName.setText(assignedService);
                            setupTicketsRealTimeListener(assignedService.toLowerCase());

                            // Add the teller's information to the "tellers" subcollection
                            db.collection("services")
                                    .document(assignedService.toLowerCase())
                                    .collection("tellers")
                                    .document(tellerName)
                                    .set(new Teller(tellerName, "N/A"))
                                    .addOnSuccessListener(aVoid -> {
                                        // Teller information added successfully
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(TellerMain.this, "Error adding teller information: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    });
                        } else {
                            textViewServiceName.setText("No Service Assigned");
                        }
                    }
                });



        // Set up real-time listener for the teller document in Firestore
        db.collection("users")
                .document(tellerUID)
                .addSnapshotListener(new EventListener<DocumentSnapshot>() {
                    @Override
                    public void onEvent(@javax.annotation.Nullable DocumentSnapshot documentSnapshot, @javax.annotation.Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            // Handle any errors that occurred during the listening process
                            textViewServiceName.setText("Error fetching assigned service");
                            return;
                        }

                        if (documentSnapshot != null && documentSnapshot.exists()) {
                            String newAssignedService = documentSnapshot.getString("service");
                            if (newAssignedService != null && !newAssignedService.equals(assignedService)) {
                                assignedService = newAssignedService;
                                textViewServiceName.setText(assignedService);
                                setupTicketsRealTimeListener(assignedService.toLowerCase());
                            } else if (newAssignedService == null) {
                                // Service was unassigned, clear the ticket list
                                assignedService = null;
                                textViewServiceName.setText("No Service Assigned");
                                ticketNumbers.clear();
                                ticketAdapter.notifyDataSetChanged();
                            }
                        }
                    }
                });

        buttonCallNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                callNextTicket();
            }
        });

        buttonCallAgain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                callAgainTicket();
            }
        });

        buttonCallPrevious.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                callPreviousTicket();
            }
        });
    }

    private void setupTicketsRealTimeListener(String assignedService) {
        db.collection("services")
                .document(assignedService.toLowerCase())
                .collection("tickets")
                .orderBy("ticketNumber")
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        // Handle any errors that occurred during the listening process
                        Toast.makeText(TellerMain.this, "Error fetching tickets: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (snapshot != null && !snapshot.isEmpty()) {
                        // Fetch the updated ticket numbers from the snapshot
                        ticketNumbers.clear();
                        for (DocumentSnapshot documentSnapshot : snapshot.getDocuments()) {
                            String ticketNumber = documentSnapshot.getString("ticketNumber");
                            ticketNumbers.add(ticketNumber);
                        }
                    } else {
                        // If there are no tickets, clear the list
                        ticketNumbers.clear();
                    }
                    // Update the list view with the new ticket numbers or empty list
                    ticketAdapter.notifyDataSetChanged();
                });
    }

    private void callNextTicket() {
        if (ticketNumbers.isEmpty()) {
            textViewTicketNumber.setText("No Tickets");
            return;
        }

        String nextTicketNumber = ticketNumbers.get(0);

        // Reference to the "tickets" collection for the selected service
        CollectionReference ticketsCollection = db.collection("services")
                .document(assignedService.toLowerCase())
                .collection("tickets");

        // Use a Firestore transaction to ensure atomicity
        db.runTransaction(transaction -> {
            // Get the ticket document in the transaction
            DocumentSnapshot ticketSnapshot = transaction.get(ticketsCollection.document(nextTicketNumber));

            if (ticketSnapshot.exists()) {
                // Get the ticket timestamp from Firestore
                Object timestamp = ticketSnapshot.get("timestamp");

                // Create a new Ticket object with the ticket number, timestamp, and teller name
                Ticket ticket = new Ticket(nextTicketNumber, timestamp, tellerName);

                // Save the ticket to the "calledTickets" collection under the service
                transaction.set(db.collection("services")
                                .document(assignedService.toLowerCase())
                                .collection("calledTickets")
                                .document(nextTicketNumber),
                        ticket);

                // Remove the ticket from the "tickets" collection
                transaction.delete(ticketsCollection.document(nextTicketNumber));

                // Update the currentTicket field to the new ticket number
                transaction.update(db.collection("services")
                        .document(assignedService.toLowerCase()), "currentTicket", nextTicketNumber);

                // Update the teller's current ticket number in the "tellers" subcollection
                transaction.update(db.collection("services")
                        .document(assignedService.toLowerCase())
                        .collection("tellers")
                        .document(tellerName), "currentTicket", nextTicketNumber);

                // Update the previousTicket field to the previous current ticket number
                String currentTicketNumber = textViewTicketNumber.getText().toString();
                if (!currentTicketNumber.isEmpty()) {
                    transaction.update(db.collection("services")
                            .document(assignedService.toLowerCase()), "previousTicket", currentTicketNumber);
                }

                return null;
            } else {
                // If the ticket does not exist, throw an exception to roll back the transaction
                throw new FirebaseFirestoreException("Ticket not found", FirebaseFirestoreException.Code.ABORTED);
            }
        }).addOnSuccessListener(aVoid -> {
            // Transaction completed successfully
            textViewTicketNumber.setText(nextTicketNumber);
            // Remove the ticket number from the list as it is called
            ticketNumbers.remove(0);
            ticketAdapter.notifyDataSetChanged();
        }).addOnFailureListener(e -> {
            // Transaction failed, handle the error
            if (e instanceof FirebaseFirestoreException && ((FirebaseFirestoreException) e).getCode() == FirebaseFirestoreException.Code.ABORTED) {
                // Ticket not found, inform the user
                Toast.makeText(TellerMain.this, "Ticket not found.", Toast.LENGTH_SHORT).show();
            } else {
                // Other errors
                Toast.makeText(TellerMain.this, "Error calling next ticket: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void callAgainTicket() {
        String currentTicket = textViewTicketNumber.getText().toString();
        if (!currentTicket.isEmpty()) {
            // You can optionally update the UI to indicate that the ticket is being called again
            Toast.makeText(TellerMain.this, "Calling Again: " + currentTicket, Toast.LENGTH_SHORT).show();
        }
    }


    private void callPreviousTicket() {
        // Retrieve the previousTicket from Firestore
        db.collection("services")
                .document(assignedService.toLowerCase())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        String previousTicketNumber = task.getResult().getString("previousTicket");

                        if (previousTicketNumber != null && !previousTicketNumber.isEmpty()) {
                            // Set the textViewTicketNumber to the previous ticket number
                            textViewTicketNumber.setText(previousTicketNumber);
                        } else {
                            Toast.makeText(TellerMain.this, "No previous ticket available.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(TellerMain.this, "Error fetching previous ticket: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

}
