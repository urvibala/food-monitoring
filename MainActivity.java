package com.example.food_monitoring1;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.android.volley.RequestQueue;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;


public class MainActivity extends AppCompatActivity {
    Button calculate;
    TextView temperature, humidity, methaneConcentration, result;
    String url = "https://food-monitoring-1.onrender.com/predict";

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private boolean isSensorDataAvailable = false;

    private void getSensorData() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {  // Check if user is signed in
            String userId = user.getUid();  // Get the user's unique ID

            // Construct a query based on user ID (optional, for user-specific data)
            Query query = db.collection("Users")
                    .document(userId)  // Filter by user ID
                    .collection("Sensors")  // Subcollection for sensor data
                    .document("Data")
                    .collection("DataCollection")
                    .orderBy("timestamp", Query.Direction.DESCENDING).limit(1);

            query.addSnapshotListener(new EventListener<QuerySnapshot>() {
                @Override
                public void onEvent(@Nullable QuerySnapshot snapshot,
                                    @Nullable FirebaseFirestoreException e) {
                    if (e != null) {
                        Log.w("TAG", "Listen failed.", e);
                        return;
                    }

                    if (snapshot != null && !snapshot.isEmpty()) {
                        DocumentSnapshot document = snapshot.getDocuments().get(0);
                        if (document.contains("Temperature") && document.contains("Humidity") && document.contains("Methane_Concentration")) {
                            String temperatureValue = document.getString("Temperature");
                            String humidityValue = document.getString("Humidity");
                            String methaneConcentrationValue = document.getString("Methane_Concentration");

                            temperature.setText(temperatureValue);
                            humidity.setText(humidityValue);
                            methaneConcentration.setText(methaneConcentrationValue);
                            isSensorDataAvailable = true;
                        }
                    }
                }
            });
        } else {
            // Handle the case where the user is not signed in
            Log.w("TAG", "User is not signed in. Please sign in to access data.");
            // You can display a message or redirect to a sign-in screen here
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        temperature = findViewById(R.id.temperature);
        humidity = findViewById(R.id.humidity);
        methaneConcentration = findViewById(R.id.methaneConcentration);
        calculate = findViewById(R.id.calculate);
        result = findViewById(R.id.result);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        getSensorData();


        calculate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!isSensorDataAvailable) {
                    Toast.makeText(MainActivity.this, "Waiting for sensor data...", Toast.LENGTH_SHORT).show();
                    return;
                }
                String temperatureStr = temperature.getText().toString();
                String humidityStr = humidity.getText().toString();
                String methaneConcentrationStr = methaneConcentration.getText().toString();
                // hit the API
                StringRequest stringRequest = new StringRequest(Request.Method.POST,url,
                        new Response.Listener<String>(){

                            @Override
                            public void onResponse(String response){
                                result.setText(response);
                            }
                        },
                        new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                Toast.makeText(MainActivity.this, "An error occurred", Toast.LENGTH_SHORT).show();

                            }
                        }){

                };
                RequestQueue queue = Volley.newRequestQueue(MainActivity.this);
                queue.add(stringRequest);
            }
        });
    }

}