package com.mcuhq.simplebluetooth;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private final static int REQUEST_ENABLE_BT = 1; // utilisé pour identifier l'ajout de noms Bluetooth

    // Composants de l'interface
    private TextView mBluetoothStatus;
    private Button mScanBtn,mOffBtn,mListPairedDevicesBtn,mDiscoverBtn;
    private ImageView icone;
    private ListView mDevicesListView;

    private BluetoothAdapter mBTAdapter;
    private Set<BluetoothDevice> mPairedDevices;
    private ArrayAdapter<String> mBTArrayAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBluetoothStatus = (TextView)findViewById(R.id.bluetooth_status);
        mScanBtn = (Button)findViewById(R.id.scan);
        mOffBtn = (Button)findViewById(R.id.off);
        mDiscoverBtn = (Button)findViewById(R.id.discover);
        mListPairedDevicesBtn = (Button)findViewById(R.id.paired_btn);
        icone = (ImageView)findViewById(R.id.Icone);


        mBTArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        mBTAdapter = BluetoothAdapter.getDefaultAdapter(); //on a creer un adaptateur pour la communication Bluetooth dans mon app

        mDevicesListView = (ListView)findViewById(R.id.devices_list_view);
        mDevicesListView.setAdapter(mBTArrayAdapter); //attribuer un modèle à affiche



        // Demander l'autorisation de localisation si ce n'est déjà fait
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);



        //Verifier si le périphérique  support le Bluetooth
        if (mBTArrayAdapter == null) {
            // Device does not support Bluetooth
            mBluetoothStatus.setText("Status: Bluetooth introuvable");
            Toast.makeText(getApplicationContext(),"Périphérique Bluetooth introuvable !",Toast.LENGTH_SHORT).show();
        }
        else {
            mScanBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    bluetoothOn();
                }
            });

            mOffBtn.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    bluetoothOff();
                }
            });

            mListPairedDevicesBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v){
                    listPairedDevices();
                }
            });

            mDiscoverBtn.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    discover();
                }
            });
        }
    }

    //ON
    private void bluetoothOn(){
        if (!mBTAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            mBluetoothStatus.setText("Bluetooth activé");
            icone.setImageResource(R.drawable.on);
            Toast.makeText(getApplicationContext(),"Bluetooth ON",Toast.LENGTH_SHORT).show();

        }
        else{
            mBluetoothStatus.setText("Bluetooth deja activé !!");
            Toast.makeText(getApplicationContext(),"Le Bluetooth est déjà activé ..", Toast.LENGTH_SHORT).show();
        }
    }

    //OFF
    private void bluetoothOff(){
        if (mBTAdapter.isEnabled()) {
            mBTAdapter.disable(); // turn off
            mBluetoothStatus.setText("Bluetooth est desactivé");
            icone.setImageResource(R.drawable.off);
            Toast.makeText(getApplicationContext(), "Turn Off", Toast.LENGTH_SHORT).show();
        }else {
            mBluetoothStatus.setText("Bluetooth deja desactivé !!!");
            Toast.makeText(getApplicationContext(),"Le Bluetooth est déjà activé ..", Toast.LENGTH_SHORT).show();
        }
    }

    //Discoverable
    private void discover(){
        // Vérifiez si l'appareil est déjà en train de découvrir
        if(mBTAdapter.isDiscovering()){
            mBTAdapter.cancelDiscovery();
            Toast.makeText(getApplicationContext(),"Découverte arrêtée",Toast.LENGTH_SHORT).show();
        }
        else{
            if(mBTAdapter.isEnabled()) {
                mBTArrayAdapter.clear(); // clear items
                mBTAdapter.startDiscovery();
                Toast.makeText(getApplicationContext(),"La découverte a commencé", Toast.LENGTH_SHORT).show();

                // Enregistrer les diffusions lorsqu'un périphérique est détecté.
                registerReceiver(blReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
            }
            else{
                Toast.makeText(getApplicationContext(),"Bluetooth not on" , Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Créer un BroadcastReceiver pour ACTION_FOUND
    final BroadcastReceiver blReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(BluetoothDevice.ACTION_FOUND.equals(action)){
                // La découverte a trouvé un appareil. Obtenir l'objet BluetoothDevice
                // et ses informations à partir de l'Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // ajouter le nom et l'addresse MAC à la liste
                mBTArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                mBTArrayAdapter.notifyDataSetChanged();
            }
        }
    };

    //Paired devices / Rechercher des peripheriaues Bluetooth
    private void listPairedDevices(){
        mBTArrayAdapter.clear();
        mPairedDevices = mBTAdapter.getBondedDevices();
        if(mBTAdapter.isEnabled()) {
            // Récupérer le nom et l'adresse de chaque appareil apparié.
            for (BluetoothDevice device : mPairedDevices)
                mBTArrayAdapter.add(device.getName() + "\n" + device.getAddress());

            Toast.makeText(getApplicationContext(), "Afficher les appareils couplés", Toast.LENGTH_SHORT).show();
        }
        else
            Toast.makeText(getApplicationContext(), "Bluetooth non activé", Toast.LENGTH_SHORT).show();
    }




}
