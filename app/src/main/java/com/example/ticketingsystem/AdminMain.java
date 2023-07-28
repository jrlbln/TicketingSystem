package com.example.ticketingsystem;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class AdminMain extends AppCompatActivity {

    private LinearLayout servicesContainer;
    private Button addButton;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_main);

        db = FirebaseFirestore.getInstance();
        servicesContainer = findViewById(R.id.servicesContainer);
        addButton = findViewById(R.id.buttonAddService);

        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addServiceToList();
            }
        });

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigation);
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_services:
                        return true;
                    case R.id.action_tellers:
                        startActivity(new Intent(AdminMain.this, AdminSetService.class));
                        finish();
                        return true;
                    default:
                        return false;
                }
            }
        });

        bottomNavigationView.setSelectedItemId(R.id.action_services);

        loadExistingServices();
        setupRealTimeListener();
    }

    private void addServiceToList() {
        EditText editTextService = findViewById(R.id.editTextService);
        String serviceText = editTextService.getText().toString().trim();

        if (!serviceText.isEmpty()) {
            // Check if the service name already exists in the list
            boolean serviceExists = checkServiceExists(serviceText);

            if (!serviceExists) {
                // Create a map to store the service details
                Map<String, Object> serviceMap = new HashMap<>();
                serviceMap.put("serviceName", serviceText);

                // Add the service to Firestore with the service name as the document ID
                db.collection("services").document(serviceText)
                        .set(serviceMap)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    // We don't need to do anything here because the real-time listener will update the UI
                                    editTextService.setText(""); // Clear the EditText after adding the service
                                } else {
                                    Toast.makeText(AdminMain.this, "Failed to add service.", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
            } else {
                Toast.makeText(this, "Service '" + serviceText + "' already exists.", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Please enter a service name.", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean checkServiceExists(String serviceText) {
        for (int i = 0; i < servicesContainer.getChildCount(); i++) {
            View serviceRow = servicesContainer.getChildAt(i);
            TextView textViewServiceName = serviceRow.findViewById(R.id.textViewServiceName);
            String existingService = textViewServiceName.getText().toString().trim();

            if (existingService.equalsIgnoreCase(serviceText)) {
                return true;
            }
        }
        return false;
    }

    private void loadExistingServices() {
        CollectionReference servicesCollection = db.collection("services");
        servicesCollection.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    for (DocumentChange documentChange : task.getResult().getDocumentChanges()) {
                        if (documentChange.getType() == DocumentChange.Type.ADDED) {
                            String serviceName = documentChange.getDocument().getString("serviceName");
                            addServiceRowWithText(serviceName);
                        }
                    }
                } else {
                    Toast.makeText(AdminMain.this, "Failed to load services.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void addServiceRowWithText(String serviceName) {
        // Check if the service already exists in the UI before adding it
        if (!checkServiceExists(serviceName)) {
            View serviceRow = LayoutInflater.from(this).inflate(R.layout.service_row_layout, null);
            TextView textViewServiceName = serviceRow.findViewById(R.id.textViewServiceName);
            Button buttonDeleteService = serviceRow.findViewById(R.id.buttonDeleteService);
            textViewServiceName.setText(serviceName);

            buttonDeleteService.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    deleteService(serviceRow, serviceName);
                }
            });

            servicesContainer.addView(serviceRow);
        }
    }

    private void deleteService(View serviceRow, String serviceName) {
        servicesContainer.removeView(serviceRow);
        if (!serviceName.isEmpty()) {
            db.collection("services").document(serviceName.toLowerCase()).delete()
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                Toast.makeText(AdminMain.this, "Service deleted successfully", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(AdminMain.this, "Failed to delete service", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }
    }

    private void setupRealTimeListener() {
        CollectionReference servicesCollection = db.collection("services");
        servicesCollection.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot snapshot, @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    // Handle any errors that occurred during the listening process
                    Toast.makeText(AdminMain.this, "Error fetching services: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    return;
                }

                // Keep track of existing service names to avoid duplicates
                Set<String> existingServiceNames = new HashSet<>();

                // Loop through the documents in the snapshot and update the UI accordingly
                for (DocumentSnapshot document : snapshot.getDocuments()) {
                    String serviceName = document.getString("serviceName");

                    if (!existingServiceNames.contains(serviceName)) {
                        addServiceRowWithText(serviceName);
                        existingServiceNames.add(serviceName);
                    }
                }

                for (int i = servicesContainer.getChildCount() - 1; i >= 0; i--) {
                    View serviceRow = servicesContainer.getChildAt(i);
                    TextView textViewServiceName = serviceRow.findViewById(R.id.textViewServiceName);
                    String existingServiceName = textViewServiceName.getText().toString().trim();

                    if (!existingServiceNames.contains(existingServiceName)) {
                        servicesContainer.removeViewAt(i);
                    }
                }
            }
        });
    }
}
