package com.example.project111;


import androidx.appcompat.app.AppCompatActivity;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONException;
import org.json.JSONObject;

public class VerifikasiActivity extends AppCompatActivity {

    private static final String TAG = "MQTTClient";
    private MqttAndroidClient mqttAndroidClient;
    private Button connectButton;
    private TextView connectionStatus;
    private TextView temperatureValue, humidityValue, pressureValue, ppmValue, distanceValue, airQualityStatus;
    private ProgressBar temperatureProgress, humidityProgress, ppmProgress, distanceProgress;

    // MQTT Configuration
    private static final String MQTT_SERVER = "ssl://7bf8eb8dc92a4636b2ec3632ce6b177a.s1.eu.hivemq.cloud:8883";
    private static final String MQTT_USERNAME = "irsyad26";
    private static final String MQTT_PASSWORD = "Irsyad261203";
    private static final String MQTT_TOPIC = "iot/esp32/data";
    private static final String CLIENT_ID = MqttClient.generateClientId();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.verification);

        // Initialize UI components
        connectButton = findViewById(R.id.connectButton);
        connectionStatus = findViewById(R.id.connectionStatus);

        temperatureValue = findViewById(R.id.temperatureValue);
        humidityValue = findViewById(R.id.humidityValue);
        pressureValue = findViewById(R.id.pressureValue);
        ppmValue = findViewById(R.id.ppmValue);
        distanceValue = findViewById(R.id.distanceValue);
        airQualityStatus = findViewById(R.id.airQualityStatus);

        temperatureProgress = findViewById(R.id.temperatureProgress);
        humidityProgress = findViewById(R.id.humidityProgress);
        ppmProgress = findViewById(R.id.ppmProgress);
        distanceProgress = findViewById(R.id.distanceProgress);

        // Initialize MQTT Client
        mqttAndroidClient = new MqttAndroidClient(this, MQTT_SERVER, CLIENT_ID);

        // Set MQTT callback
        mqttAndroidClient.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                runOnUiThread(() -> {
                    connectionStatus.setText("Status: Disconnected");
                    connectionStatus.setTextColor(Color.RED);
                    connectButton.setText("Connect to MQTT");
                    Toast.makeText(VerifikasiActivity.this, "Connection lost", Toast.LENGTH_SHORT).show();
                });
                Log.d(TAG, "Connection lost: " + cause.getMessage());
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                String payload = new String(message.getPayload());
                Log.d(TAG, "Message arrived: " + payload);

                runOnUiThread(() -> updateUI(payload));
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                Log.d(TAG, "Delivery complete");
            }
        });

        // Connect button click listener
        connectButton.setOnClickListener(v -> {
            if (mqttAndroidClient.isConnected()) {
                disconnectFromMqtt();
            } else {
                connectToMqtt();
            }
        });
    }

    private void connectToMqtt() {
        try {
            MqttConnectOptions options = new MqttConnectOptions();
            options.setUserName(MQTT_USERNAME);
            options.setPassword(MQTT_PASSWORD.toCharArray());
            options.setCleanSession(true);
            options.setAutomaticReconnect(true);

            mqttAndroidClient.connect(options, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    runOnUiThread(() -> {
                        connectionStatus.setText("Status: Connected");
                        connectionStatus.setTextColor(Color.GREEN);
                        connectButton.setText("Disconnect");
                        Toast.makeText(VerifikasiActivity.this, "Connected to MQTT", Toast.LENGTH_SHORT).show();
                    });
                    subscribeToTopic();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    runOnUiThread(() -> {
                        connectionStatus.setText("Status: Connection failed");
                        connectionStatus.setTextColor(Color.RED);
                        Toast.makeText(VerifikasiActivity.this, "Connection failed: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                    Log.e(TAG, "Connection failure: " + exception.getMessage());
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
            Log.e(TAG, "MQTT Exception: " + e.getMessage());
        }
    }

    private void disconnectFromMqtt() {
        try {
            mqttAndroidClient.disconnect().setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    runOnUiThread(() -> {
                        connectionStatus.setText("Status: Disconnected");
                        connectionStatus.setTextColor(Color.RED);
                        connectButton.setText("Connect to MQTT");
                        Toast.makeText(VerifikasiActivity.this, "Disconnected from MQTT", Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    runOnUiThread(() -> {
                        connectionStatus.setText("Status: Disconnect failed");
                        connectionStatus.setTextColor(Color.RED);
                        Toast.makeText(VerifikasiActivity.this, "Disconnect failed", Toast.LENGTH_SHORT).show();
                    });
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
            Log.e(TAG, "MQTT Disconnect Exception: " + e.getMessage());
        }
    }

    private void subscribeToTopic() {
        try {
            mqttAndroidClient.subscribe(MQTT_TOPIC, 0, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.d(TAG, "Subscribed to topic: " + MQTT_TOPIC);
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.e(TAG, "Subscription failed: " + exception.getMessage());
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
            Log.e(TAG, "MQTT Subscribe Exception: " + e.getMessage());
        }
    }

    private void updateUI(String payload) {
        try {
            JSONObject jsonObject = new JSONObject(payload);

            // Update temperature
            double temperature = jsonObject.getDouble("suhu");
            temperatureValue.setText(String.format("%.1f °C", temperature));
            temperatureProgress.setProgress((int) temperature);

            // Update humidity
            double humidity = jsonObject.getDouble("kelembapan");
            humidityValue.setText(String.format("%.1f %%", humidity));
            humidityProgress.setProgress((int) humidity);

            // Update pressure
            double pressure = jsonObject.getDouble("tekanan");
            pressureValue.setText(String.format("%.1f hPa", pressure));

            // Update air quality (PPM)
            double ppm = jsonObject.getDouble("mq135_ppm");
            ppmValue.setText(String.format("%.1f PPM", ppm));
            ppmProgress.setProgress((int) ppm);

            // Update air quality status
            if (ppm > 200) {
                airQualityStatus.setText("Status: Poor Air Quality");
                airQualityStatus.setTextColor(Color.RED);
            } else {
                airQualityStatus.setText("Status: Good Air Quality");
                airQualityStatus.setTextColor(Color.GREEN);
            }

            // Update distance
            double distance = jsonObject.getDouble("jarak_ultrasonik");
            distanceValue.setText(String.format("%.1f cm", distance));
            distanceProgress.setProgress((int) distance);

        } catch (JSONException e) {
            e.printStackTrace();
            Log.e(TAG, "JSON parsing error: " + e.getMessage());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mqttAndroidClient != null && mqttAndroidClient.isConnected()) {
            try {
                mqttAndroidClient.disconnect();
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
    }
}