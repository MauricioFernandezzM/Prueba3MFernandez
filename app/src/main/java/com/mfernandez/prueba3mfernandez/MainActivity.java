package com.mfernandez.prueba3mfernandez;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.functions.FirebaseFunctions;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private FirebaseFunctions mFunctions;
    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference dbReference;

    MyArrayAdapter arrayAdapter;
    List<Sensor> sensorList;

    private ListView sensor_listView;
    //private String id;
    //private  String sensor;

    private static final String serverUri = "tcp://test.mosquitto.org:1883";
    private static final String userName = "mauri";
    private static final String password = "1235";
    private static final String appName = "app1";
    private static final String clientId = "mauri";
    private static final String TAG = "MQTT Client 01";
    private MqttAndroidClient mqttAndroidClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        sensorList = new ArrayList<>();
        arrayAdapter =  new MyArrayAdapter(sensorList, getBaseContext(), getLayoutInflater());
        sensor_listView = (ListView) findViewById(R.id.list_view);
        sensor_listView.setAdapter(arrayAdapter);

        mFunctions = FirebaseFunctions.getInstance();

        initFirebaseDB();
        loadDataFromFirebase();

        mqttAndroidClient =  new MqttAndroidClient(getApplicationContext(), serverUri, clientId);
        mqttAndroidClient.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                if (reconnect) {
                    Log.d(TAG, "Reconectado a : " + serverURI);
                } else {
                    Log.d(TAG, "Conectado a: " + serverURI);
                }
            }

            @Override
            public void connectionLost(Throwable cause) {
                Log.d(TAG, "Se ha perdido la conexón!.");
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                Log.d(TAG, "Mensaje recibido:" + message.toString());

                //String json;

                JSONObject jsonObject = new JSONObject(message.toString());

                ModeloSensor modeloSensor = new ModeloSensor(
                        UUID.randomUUID().toString(),
                        jsonObject.getString("nombre_sensor"),
                        jsonObject.getString("tipo"),
                        jsonObject.getString("valor"),
                        jsonObject.getString("ubicacion"),
                        jsonObject.getString("fecha-hora"),
                        jsonObject.getString("observ")
                );
                //Toast.makeText(getApplicationContext(), "", Toast.LENGTH_LONG).show();

                dbReference.child("sensor").child(modeloSensor.getIdSensor()).setValue(modeloSensor);

                Toast.makeText(getApplicationContext(), "Sensor guardado!", Toast.LENGTH_LONG).show();

            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });

        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setAutomaticReconnect(true);
        mqttConnectOptions.setCleanSession(false);
        mqttConnectOptions.setUserName(userName);
        mqttConnectOptions.setPassword(password.toCharArray());

        try {
            mqttAndroidClient.connect(mqttConnectOptions, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.d(TAG, "Conectado a: " + serverUri);

                    try {
                        asyncActionToken.getSessionPresent();
                        Log.d(TAG, "Topicos: " + asyncActionToken.getTopics().toString());
                    } catch (Exception e) {
                        String message = e.getMessage();
                        Log.d(TAG, "Error el mensaje es nulo! " + String.valueOf(message == null));
                    }

                    Toast.makeText(MainActivity.this, "Conectado", Toast.LENGTH_SHORT).show();

                    DisconnectedBufferOptions disconnectedBufferOptions = new DisconnectedBufferOptions();
                    disconnectedBufferOptions.setBufferEnabled(true);
                    disconnectedBufferOptions.setBufferSize(100);
                    disconnectedBufferOptions.setPersistBuffer(false);
                    disconnectedBufferOptions.setDeleteOldestMessages(false);
                    mqttAndroidClient.setBufferOpts(disconnectedBufferOptions);

                    try {
                        mqttAndroidClient.subscribe("mauri",2);
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {

                    Log.d(TAG, "Falla al conectar a: " + serverUri);

                }
            });
        } catch (MqttException ex) {
            ex.printStackTrace();
        }


    }

    private void loadDataFromFirebase() {
        dbReference.child("sensor").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                sensorList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    sensorList.add(ds.getValue(Sensor.class));
                }
                arrayAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getBaseContext(), "Error, "+ error.toString(), Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void initFirebaseDB() {
        FirebaseApp.initializeApp(this);
        firebaseDatabase = FirebaseDatabase.getInstance();
        dbReference = firebaseDatabase.getReference();
    }


}