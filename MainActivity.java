package com.example.food_monitoring1;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
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

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;


public class MainActivity extends AppCompatActivity {
    Button calculate , get_data;
    TextView temperature, humidity, methaneConcentration, result;
    String url = "https://food-monitoring-1.onrender.com/predict";

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private boolean isSensorDataAvailable = false;

    private void getSensorData() {
        isSensorDataAvailable = true;
        Query query = db.collection("Sensors")
                .limit(1);

        query.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    QuerySnapshot snapshot = task.getResult();
                    if (snapshot != null && !snapshot.isEmpty()) {
                        DocumentSnapshot document = snapshot.getDocuments().get(0);
                        String temperatureValue = document.getString("Temperature");
                        String humidityValue = document.getString("Humidity");
                        Double methaneConcentrationValue = document.getDouble("Methane_Concentration");

                        // Log retrieved values for inspection
                        Log.d("TAG", "Temperature: " + temperatureValue);
                        Log.d("TAG", "Humidity: " + humidityValue);
                        Log.d("TAG", "Methane Concentration: " + methaneConcentrationValue);

                        // Check for null values before setting textviews
                        if (temperatureValue != null) {
                            temperature.setText(temperatureValue);
                        } else {
                            temperature.setText("N/A"); // Or a placeholder message
                        }

                        if (humidityValue != null) {
                            humidity.setText(humidityValue);
                        } else {
                            humidity.setText("N/A");
                        }

                        if (methaneConcentrationValue != null) {
                            methaneConcentration.setText(String.valueOf(methaneConcentrationValue));
                        } else {
                            methaneConcentration.setText("N/A");
                        }
                    } else {
                        Log.w("TAG", "No sensor data found");
                    }
                } else {
                    Log.w("TAG", "Error getting sensor data", task.getException());
                }
            }
        });
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
        get_data = findViewById(R.id.get_data);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        get_data.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getSensorData();
            }
        });

        calculate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!isSensorDataAvailable) {
                    Toast.makeText(MainActivity.this, "Waiting for sensor data...", Toast.LENGTH_SHORT).show();
                    return;
                }
                double temperatureValue = Double.parseDouble(temperature.getText().toString());
                double humidityValue = Double.parseDouble(humidity.getText().toString());
                double methaneConcentrationValue = Double.parseDouble(methaneConcentration.getText().toString());

                Map<String, String> params = new HashMap<>();
                params.put("temperature", String.valueOf(temperatureValue));  // Convert double to String
                params.put("humidity", String.valueOf(humidityValue));
                params.put("methane_concentration", String.valueOf(methaneConcentrationValue));


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
                    @Override
                    protected Map<String,String> getParams() {
                        Map<String, String> params = new HashMap<String, String>();
                        params.put("temperature", String.valueOf(temperatureValue));  // Convert double
                        params.put("humidity", String.valueOf(humidityValue));
                        params.put("methane_concentration", String.valueOf(methaneConcentrationValue));

                        return params;
                    }

                };
                RequestQueue queue = Volley.newRequestQueue(MainActivity.this);
                queue.add(stringRequest);
            }
        });
    }

}
