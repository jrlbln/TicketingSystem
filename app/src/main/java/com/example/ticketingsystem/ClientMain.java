package com.example.ticketingsystem;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ClientMain extends AppCompatActivity {

    private TextView textViewClientName;
    private TextView textViewTicketNumber;
    private TextView textViewService;
    private Spinner spinnerServices;
    private Button buttonGenerateTicket;
    private List<String> servicesList;
    private List<String> servicesListForInitialLoading;
    private CustomSpinnerAdapter spinnerAdapter;
    private ListenerRegistration servicesListenerRegistration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client_main);

        textViewClientName = findViewById(R.id.textViewClientName);
        textViewTicketNumber = findViewById(R.id.textViewTicketNumber);
        textViewService = findViewById(R.id.textViewService);
        spinnerServices = findViewById(R.id.spinnerServices);
        buttonGenerateTicket = findViewById(R.id.buttonGenerateTicket);

        servicesListForInitialLoading = new ArrayList<>();
        servicesList = new ArrayList<>();

        spinnerAdapter = new CustomSpinnerAdapter(this, servicesList);
        spinnerServices.setAdapter(spinnerAdapter);

        spinnerServices.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Update the selected service in the textViewService
                String selectedService = (String) parent.getItemAtPosition(position);
                textViewService.setText(selectedService);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });

        loadServicesFromFirestore();

        setupRealTimeListener();
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
}
