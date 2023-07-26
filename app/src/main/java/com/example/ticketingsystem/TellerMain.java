package com.example.ticketingsystem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

public class TellerMain extends AppCompatActivity {

    private FirebaseFirestore db;
    private TextView textViewServiceName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teller_main);

        db = FirebaseFirestore.getInstance();
        textViewServiceName = findViewById(R.id.textViewServiceName);

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

        String tellerUID = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Retrieve the service assigned to the teller from Firestore
        db.collection("users")
                .document(tellerUID)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        DocumentSnapshot documentSnapshot = task.getResult();
                        String assignedService = documentSnapshot.getString("service");
                        if (assignedService != null) {
                            textViewServiceName.setText(assignedService);
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
                            String assignedService = documentSnapshot.getString("service");
                            if (assignedService != null) {
                                textViewServiceName.setText(assignedService);
                            } else {
                                textViewServiceName.setText("No Service Assigned");
                            }
                        }
                    }
                });
    }


}
