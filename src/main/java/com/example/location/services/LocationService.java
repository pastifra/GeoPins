package com.example.location.services;


import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

public class LocationService extends Service {
    //inizializzo i metodi dell'API Location
    FusedLocationProviderClient fusedLocationProviderClient;
    LocationCallback locationCallback;
    private final String LOCATION_SERVICE = "Location_Service";
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    @Override
    public void onCreate() {
        super.onCreate();
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        locationCallback = new LocationCallback(){
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                //scrivo nel log i risultati della location
                Log.d(LOCATION_SERVICE,"Lat is :" + locationResult.getLastLocation().getLatitude() + ", " + "Lng is :" + locationResult.getLastLocation().getLongitude());
                //inizializzo un intent per mandare un broadcast al main lat e long
                Intent intent = new Intent("ACT_LOG");
                intent.putExtra("latitude", locationResult.getLastLocation().getLatitude());
                intent.putExtra("longitude", locationResult.getLastLocation().getLongitude());
                //invio il broadcast al main
                sendBroadcast(intent);
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //richiedo la location
        requestLocation();
        return super.onStartCommand(intent, flags, startId);
    }

    private void requestLocation() {
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(3000); //ogni quanto tempo aggiorno la location (15 secondi)
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY); //power consumption del gps
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper()); //faccio un update del Location Client
    }
}
