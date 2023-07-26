package com.example.ticketingsystem;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminSetService extends AppCompatActivity {

    private FirebaseFirestore db;
    private LinearLayout tellersContainer;
    private Button saveButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_set_service);

        db = FirebaseFirestore.getInstance();
        tellersContainer = findViewById(R.id.tellersContainer);
        saveButton = findViewById(R.id.buttonSave);

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigation);
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_services:
                        startActivity(new Intent(AdminSetService.this, AdminMain.class));
                        finish(); // Call finish() to close the current activity and prevent going back to it
                        return true;
                    case R.id.action_tellers:
                        // Already in the Tellers activity, no need to navigate to it again
                        return true;
                    default:
                        return false;
                }
            }
        });

        bottomNavigationView.setSelectedItemId(R.id.action_tellers);

        // Load tellers and their services from Firestore
        loadTellers();

        // Set a click listener for the "Save" button
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveTellerServices();
            }
        });
    }

    private void loadTellers() {
        CollectionReference tellersCollection = db.collection("users");
        tellersCollection.whereEqualTo("isTeller", true).get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            List<DocumentSnapshot> tellersList = task.getResult().getDocuments();
                            for (DocumentSnapshot tellerSnapshot : tellersList) {
                                String tellerName = tellerSnapshot.getString("name");
                                String assignedService = tellerSnapshot.getString("service");
                                addTellerRow(tellerName, assignedService);
                            }
                        } else {
                            Toast.makeText(AdminSetService.this, "Failed to load tellers.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void addTellerRow(String tellerName, String assignedService) {
        View tellerRow = LayoutInflater.from(this).inflate(R.layout.teller_row_layout, null);
        TextView textViewTellerName = tellerRow.findViewById(R.id.textViewTellerName);
        TextView textViewService = tellerRow.findViewById(R.id.textViewService);
        Spinner spinnerServices = tellerRow.findViewById(R.id.spinnerServices);

        textViewTellerName.setText(tellerName);

        // Load services from Firestore when the activity is created
        loadServices(new OnServicesLoadedListener() {
            @Override
            public void onServicesLoaded(List<String> servicesList) {
                // Use the custom spinner adapter here
                CustomSpinnerAdapter adapter = new CustomSpinnerAdapter(AdminSetService.this, servicesList);
                spinnerServices.setAdapter(adapter);

                // Set the selected service if available
                if (assignedService != null && !assignedService.isEmpty()) {
                    int position = adapter.getPosition(assignedService);
                    spinnerServices.setSelection(position);
                }

                // Set the OnItemSelectedListener for the spinner
                spinnerServices.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        String selectedService = (String) parent.getItemAtPosition(position);
                        textViewService.setText(selectedService);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                        // Do nothing
                    }
                });
            }
        });

        tellersContainer.addView(tellerRow);
    }

    private void loadServices(final OnServicesLoadedListener listener) {
        CollectionReference servicesCollection = db.collection("services");
        servicesCollection.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    List<String> servicesList = new ArrayList<>();
                    for (DocumentSnapshot document : task.getResult()) {
                        String serviceName = document.getString("serviceName");
                        servicesList.add(serviceName);
                    }
                    listener.onServicesLoaded(servicesList);
                } else {
                    Toast.makeText(AdminSetService.this, "Failed to load services.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private interface OnServicesLoadedListener {
        void onServicesLoaded(List<String> servicesList);
    }

    private void saveTellerServices() {
        for (int i = 0; i < tellersContainer.getChildCount(); i++) {
            View tellerRow = tellersContainer.getChildAt(i);
            TextView textViewTellerName = tellerRow.findViewById(R.id.textViewTellerName);
            Spinner spinnerServices = tellerRow.findViewById(R.id.spinnerServices);
            String tellerName = textViewTellerName.getText().toString().trim();
            String selectedService = spinnerServices.getSelectedItem().toString();

            // Update the teller document in Firestore with the selected service
            db.collection("users").whereEqualTo("name", tellerName).limit(1)
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.isSuccessful() && task.getResult() != null) {
                                for (DocumentSnapshot document : task.getResult()) {
                                    String tellerId = document.getId();
                                    Map<String, Object> updateData = new HashMap<>();
                                    updateData.put("service", selectedService);

                                    db.collection("users").document(tellerId)
                                            .set(updateData, SetOptions.merge())
                                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void aVoid) {
                                                    Toast.makeText(AdminSetService.this, "Service assigned to teller: " + tellerName, Toast.LENGTH_SHORT).show();
                                                }
                                            })
                                            .addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                    Toast.makeText(AdminSetService.this, "Failed to update teller: " + tellerName, Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                }
                            }
                        }
                    });
        }
    }
}

