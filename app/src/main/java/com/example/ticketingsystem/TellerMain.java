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
    private String assignedService;
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
                        if (assignedService != null) {
                            textViewServiceName.setText(assignedService);
                            setupTicketsRealTimeListener(assignedService.toLowerCase());
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
                            assignedService = documentSnapshot.getString("service");
                            if (assignedService != null) {
                                textViewServiceName.setText(assignedService);
                            } else {
                                textViewServiceName.setText("No Service Assigned");
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
                        // Clear the previous ticket list
                        ticketNumbers.clear();
                        // Fetch the updated ticket numbers from the snapshot
                        for (DocumentSnapshot documentSnapshot : snapshot.getDocuments()) {
                            String ticketNumber = documentSnapshot.getString("ticketNumber");
                            ticketNumbers.add(ticketNumber);
                        }
                        // Update the list view with the new ticket numbers
                        ticketAdapter.notifyDataSetChanged();
                    }
                });
    }

    private void callNextTicket() {
        if (ticketNumbers.isEmpty()) {
            textViewTicketNumber.setText("No Tickets");
            return;
        }

        String nextTicketNumber = ticketNumbers.get(0);
        String currentTicketNumber = textViewTicketNumber.getText().toString();

        // Remove the first ticket from the list in Firestore
        db.collection("services")
                .document(assignedService.toLowerCase())
                .collection("tickets")
                .document(nextTicketNumber)
                .delete()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Retrieve the currentTicket value from Firestore
                        db.collection("services")
                                .document(assignedService.toLowerCase())
                                .get()
                                .addOnCompleteListener(currentTicketTask -> {
                                    if (currentTicketTask.isSuccessful() && currentTicketTask.getResult() != null) {
                                        String currentTicket = currentTicketTask.getResult().getString("currentTicket");

                                        // Update the previousTicket field to the retrieved currentTicket
                                        db.collection("services")
                                                .document(assignedService.toLowerCase())
                                                .update("previousTicket", currentTicket)
                                                .addOnSuccessListener(aVoid -> {
                                                    // Set the currentTicket field to the new ticket number
                                                    db.collection("services")
                                                            .document(assignedService.toLowerCase())
                                                            .update("currentTicket", nextTicketNumber)
                                                            .addOnSuccessListener(aVoid1 -> {
                                                                textViewTicketNumber.setText(nextTicketNumber);

                                                                // Create "calledTickets" collection if it doesn't exist
                                                                db.collection("services")
                                                                        .document(assignedService.toLowerCase())
                                                                        .collection("calledTickets")
                                                                        .document(nextTicketNumber)
                                                                        .set(new Ticket(nextTicketNumber, FieldValue.serverTimestamp()))
                                                                        .addOnSuccessListener(aVoid2 -> {
                                                                            // Ticket added to "calledTickets" collection
                                                                        })
                                                                        .addOnFailureListener(e -> {
                                                                            Toast.makeText(TellerMain.this, "Error adding ticket to 'calledTickets': " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                                        });
                                                            })
                                                            .addOnFailureListener(e -> {
                                                                Toast.makeText(TellerMain.this, "Error updating current ticket: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                            });
                                                })
                                                .addOnFailureListener(e -> {
                                                    Toast.makeText(TellerMain.this, "Error updating previous ticket: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                });
                                    } else {
                                        Toast.makeText(TellerMain.this, "Error retrieving current ticket: " + currentTicketTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        Toast.makeText(TellerMain.this, "Error calling next ticket: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
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
