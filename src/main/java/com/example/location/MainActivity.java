package com.example.location;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.example.location.adapters.DatabaseAdapter;
import com.example.location.adapters.ListAdapter;
import com.example.location.services.LocationService;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.VisibleRegion;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {
    //inizializzo la mappa, il fragment per la mappa e il marker che verrà mostrato sulla mappa
    public static DatabaseAdapter database;
    static GeoPoint localPosition, minCoord, maxCoord;
    static String localProfileId, localProfileName;
    static boolean loggedOnDatabase = false;
    public final static boolean LOG_SWITCH = true;
    public final static String SHARED_PREF_PROFILE_ID = "Local_Profile_Id", SHARED_PREF_PROFILE_NAME = "Local_Profile_Name"
            , SAVE_SHARED_PREF_ACCOUNT = "Save_Shared_Pref_Account"
            , LATITUDE_FROM_COMMENTS = "Latitude_From_Comments", LONGITUDE_FROM_COMMENTS = "Longitude_From_Comments";
    final static String MIN_LAT = "Min_Lat", MIN_LOG = "Min_Log", MAX_LAT = "Max_Lat", MAX_LOG = "Max_Log", EXTRA_TAG = "Extra_Tag";
    private final String MAIN_ACTIVITY = "Main_Activity";
    private final String VISUALIZED_LONGITUDE = "Visualized_Longitude", VISUALIZED_LATITUDE = "Visualized_Latitude", VISUALIZED_ZOOM = "Visualized_Zoom";
    private boolean minZoomOk = false;
    private final float MIN_CAMERA_ZOOM = (float) 14.5, DEFAULT_CAMERA_ZOOM = 18;
    private boolean alreadyPositionedCamera = false;
    public static LinkedList<Map> CommentList = new LinkedList<>();
    public static double lat;
    public static double longitude;
    private boolean receivedPositionFromRecycler = false;
    private LatLng lastPosition = null;
    private float lastZoom=0;
    Map< String, Marker> markerMap = new HashMap<>();
    SupportMapFragment mapFragment;
    GoogleMap mMap;
    Marker marker;
    Circle circle;
    Toolbar toolbar;
    SharedPreferences sharedPref;
    LatLng positionFromRecycler = null;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        alreadyPositionedCamera = false;
        sharedPref = getPreferences(Context.MODE_PRIVATE);
        database = new DatabaseAdapter( FirebaseFirestore.getInstance());
        //valori profilo salvati
        if( sharedPref == null){
            Log.d( "Shared_Pref", " Null");
        }else{
            localProfileId = sharedPref.getString( SHARED_PREF_PROFILE_ID, "No_Id");
            localProfileName = sharedPref.getString( SHARED_PREF_PROFILE_NAME, "No_Name");
        }

        //posizione mappa
        if( savedInstanceState != null){
            lastPosition = new LatLng( savedInstanceState.getDouble(VISUALIZED_LATITUDE), savedInstanceState.getDouble( VISUALIZED_LONGITUDE));
            lastZoom = savedInstanceState.getFloat( VISUALIZED_ZOOM);
            if( LOG_SWITCH)
                Log.d( MAIN_ACTIVITY, "onRestoreInstanceState : cameraPosition : " + lastPosition.toString());
        }


        //Da android Marshmallow SDK 23 devo richiedere i permessi per accedere alla posizione
        if(Build.VERSION.SDK_INT >= 23){
            if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
                //Se non è stato garantito il permesso di accesso lo richiedo
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }
            else{
                startService();
            }
        }
        else {
            startService();
        }
        //dichiaro la mappa
        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapFrag);
        mapFragment.getMapAsync(this);
        //dichiaro la toolbar
        toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("Geopins");
        setSupportActionBar(toolbar);
    }

    @Override
    protected void onSaveInstanceState( Bundle savedInstanceState){ //salvo la posizione della mappa
        super.onSaveInstanceState(savedInstanceState);
        LatLng cameraPosition = mMap.getCameraPosition().target;
        savedInstanceState.putDouble( VISUALIZED_LONGITUDE, cameraPosition.longitude);
        savedInstanceState.putDouble( VISUALIZED_LATITUDE, cameraPosition.latitude);
        savedInstanceState.putFloat( VISUALIZED_ZOOM, mMap.getCameraPosition().zoom);
        if( LOG_SWITCH)
            Log.d( MAIN_ACTIVITY, "onSaveInstanceState : instantState : " + savedInstanceState.toString());
    }

    protected void onRestoreInstanceState( Bundle savedInstanceState){ //ricarico la posizione della mappa
        if( savedInstanceState != null){
            lastPosition = new LatLng( savedInstanceState.getDouble( VISUALIZED_LATITUDE), savedInstanceState.getDouble( VISUALIZED_LONGITUDE));
            lastZoom = savedInstanceState.getFloat( VISUALIZED_ZOOM);
            if( LOG_SWITCH)
                Log.d( MAIN_ACTIVITY, "onRestoreInstanceState : cameraPosition : " + lastPosition.toString());
        }
    }


    /*@Override
    protected void onRestart(){
        super.onRestart();
        Intent caller = getIntent();
        if( caller != null && caller.getAction() != null && caller.getAction().equals(ListAdapter.POSITION_FROM_COMMENTS)){
            receivedPositionFromRecycler = true;
            Log.d( MAIN_ACTIVITY, "Received Position");
            positionFromRecycler = new LatLng( caller.getDoubleExtra("Latitude", 0), caller.getDoubleExtra("Longitude", 0));
        }
    }*/

    //salva valori profilo
    final void savePreferenceNow( String key, String value){
        SharedPreferences.Editor sharedPrefEditor = sharedPref.edit();
        sharedPrefEditor.putString( key, value);
        sharedPrefEditor.commit();
        if( LOG_SWITCH)
            Log.d( MAIN_ACTIVITY, "savePreferenceNow : " + key + " : " + value);
    }


    void startService(){//Creo un BroadcastReceiver per la mia posizione
        MainActivityBroadcastReceiver receiver = new MainActivityBroadcastReceiver();
        IntentFilter filter = new IntentFilter("ACT_LOG");
        filter.addAction( SAVE_SHARED_PREF_ACCOUNT);
        registerReceiver(receiver, filter);
        Intent intent = new Intent(MainActivity.this, LocationService.class);
        startService(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        switch(item.getItemId()) {
            //VADO A SETTINGS
            case R.id.account_button:
                Intent nextActivity = new Intent(this, Settings.class);
                startActivity(nextActivity);
                break;
            //RICARICO LA MAPPA CON NUOVE COORDINATE
            case R.id.gps_button:
                    Toast.makeText(MainActivity.this,"GPS refresh request received, please try again if your location is still incorrect",Toast.LENGTH_LONG).show();

                    LatLng latLng = new LatLng(lat,longitude);
                    MarkerOptions markerOptions = new MarkerOptions();
                    markerOptions.position(latLng);
                    markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
                    markerOptions.title("You are here!");
                    if(marker != null) {
                        marker.setPosition(latLng);
                    }
                    else{
                        marker = mMap.addMarker(markerOptions);
                    }
                    CircleOptions circleOptions = new CircleOptions();
                    circleOptions.center(latLng);
                    circleOptions.radius(5.5); //in metri
                    circleOptions.strokeColor(Color.CYAN);
                    circleOptions.fillColor(Color.argb(60,3,169,244 ));
                    if(circle != null) {
                       circle.setCenter(latLng);
                    }
                    else{
                        circle = mMap.addCircle(circleOptions);
                    }

                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng,DEFAULT_CAMERA_ZOOM));
                    alreadyPositionedCamera = true;

                break;

        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) { //RICHIEDO LOCATION PERMISSIONS SE NON MI SONO STATI DATI
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch(requestCode){
            case 1:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    startService();
                }
                else{
                    Toast.makeText(this, "Please give the app Location permission", Toast.LENGTH_LONG).show();
                }
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);

        if( lastPosition != null){  //ricevo la posizione precedente dalla mappa se è stata salvata
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom( lastPosition, lastZoom));
            alreadyPositionedCamera = true;
        }
        if( receivedPositionFromRecycler) {  //se ricevo il comando dalla recycler metto la mappa al centro
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(positionFromRecycler, DEFAULT_CAMERA_ZOOM));
            alreadyPositionedCamera = true;
        }
        //LISTENER DEL MARKER
        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                if( LOG_SWITCH)
                    Log.d( MAIN_ACTIVITY, "onMarkerClick : marker tag : " + marker.getTag() + " marker id : " + marker.getId());
                return false;
            }
        });

        mMap.getUiSettings().setRotateGesturesEnabled( false);

        mMap.setOnCameraIdleListener(new GoogleMap.OnCameraIdleListener() {
            @Override
            public void onCameraIdle() {    //NELLA VISIBLE REGION INSERISCO MARKER ASSOCIATI AI GEOPIN
                VisibleRegion displayed = mMap.getProjection().getVisibleRegion(); //PRENDO COME LATITUDINI MINIME E MASSIME QUELLE DELLA MAPPA VISUALIZZATA
                LatLng mMin = displayed.nearLeft;
                minCoord = new GeoPoint( mMin.latitude, mMin.longitude);
                LatLng mMax = displayed.farRight;
                maxCoord = new GeoPoint( mMax.latitude, mMax.longitude);
                minZoomOk = mMap.getCameraPosition().zoom > MIN_CAMERA_ZOOM; //zoom minimo per gestire la ricerca dei commenti
                if( minZoomOk) {
                    putMarkerReceivedFromDatabase();
                }
                if( LOG_SWITCH) {
                    Log.d(MAIN_ACTIVITY, "onMapReady : minCoord " + minCoord.toString());
                    Log.d(MAIN_ACTIVITY, "onMapReady : maxCoord " + maxCoord.toString());
                    Log.d(MAIN_ACTIVITY, "onMapReady : minZoomOk " + minZoomOk);
                }
            }
        });
    }

    public class MainActivityBroadcastReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            //RICEVO IL BROADCAST DA LOCATION SERVICE
            Log.d( "Brodcast_receiver", "Received by : " + context);
            if(intent.getAction().equals("ACT_LOG")){
                lat = intent.getDoubleExtra("latitude", 0f);
                longitude = intent.getDoubleExtra("longitude", 0f);
                localPosition = new GeoPoint( lat, longitude);
                //controllo che la mappa sia pronta
                if(mMap != null && !alreadyPositionedCamera){
                    LatLng latLng = new LatLng(lat,longitude);

                    MarkerOptions markerOptions = new MarkerOptions();
                    markerOptions.position(latLng);
                    markerOptions.title("You are here!");
                    markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN));
                    if(marker != null) {
                        marker.setPosition(latLng);
                    }
                    else{
                        marker = mMap.addMarker(markerOptions);
                    }
                    CircleOptions circleOptions = new CircleOptions();
                    circleOptions.center(latLng);
                    circleOptions.radius(5.0); //in metri
                    circleOptions.strokeColor(Color.CYAN);
                    circleOptions.fillColor(Color.argb(60,3,169,244 ));
                    if(circle != null) {
                        circle.setCenter(latLng);
                    }
                    else{
                        circle = mMap.addCircle(circleOptions);
                    }
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng,DEFAULT_CAMERA_ZOOM));
                    alreadyPositionedCamera = true;
                }
            }
            if(intent.getAction().equals( SAVE_SHARED_PREF_ACCOUNT)){ //mi arrivano i dati del profilo che servono per scrivere nel datatbase
                String buffer = intent.getStringExtra( SHARED_PREF_PROFILE_ID);
                localProfileId = buffer;
                savePreferenceNow( SHARED_PREF_PROFILE_ID, buffer);
                buffer = intent.getStringExtra( SHARED_PREF_PROFILE_NAME);
                localProfileName = buffer;
                savePreferenceNow( SHARED_PREF_PROFILE_NAME, buffer);
            }
            /*if( intent.getAction().equals(ListAdapter.POSITION_FROM_COMMENTS)){
                double latitude = intent.getDoubleExtra( LATITUDE_FROM_COMMENTS, 0);
                double longitude = intent.getDoubleExtra( LONGITUDE_FROM_COMMENTS, 0);
                if( mMap != null){
                    LatLng latLng = new LatLng( latitude, longitude);
                    if( LOG_SWITCH)
                        Log.d( MAIN_ACTIVITY, "broadcastReceiver : POS_FROM_COMM : latLng : " + latLng.toString());
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng,DEFAULT_CAMERA_ZOOM));
                    alreadyPositionedCamera = true;
                }else{
                    if(LOG_SWITCH)
                        Log.d( MAIN_ACTIVITY, "broadcastReceiver : POS_FROM_COMM : mMap : Null");
                }
            }*/
        }
    }

    public void geopinMe(View view){
        //VADO A COMMENTS SOLO SE LO ZOOM NON E' TROPPO PICCOLO
        if( minZoomOk){
            Intent nextActivity = new Intent(this, Comments.class);
            Bundle extra = new Bundle();
            //DOVE CERCO I GEOPIN
            extra.putDouble( MIN_LAT, minCoord.getLatitude());
            extra.putDouble( MIN_LOG, minCoord.getLongitude());
            extra.putDouble( MAX_LAT, maxCoord.getLatitude());
            extra.putDouble( MAX_LOG, maxCoord.getLongitude());
            nextActivity.putExtra( EXTRA_TAG, extra);
            startActivity(nextActivity);
            if( LOG_SWITCH)
                Log.d( MAIN_ACTIVITY, "geopinMe : switch to Comments");
        }else{
        Toast.makeText(MainActivity.this,"The area selected is too big, please zoom in!",Toast.LENGTH_LONG).show();
            if( LOG_SWITCH)
                Log.d( MAIN_ACTIVITY, "geopinMe : zoom too low, can't handle");
        }
    }

    private void putMarkerReceivedFromDatabase(){
        MainActivity.database.getCommentAroundYou(MainActivity.minCoord, MainActivity.maxCoord, 100, 0);//scarico i commenti attorno a me
        MainActivity.database.setReceiveDataListener(new DatabaseAdapter.OnCommentAroundYouReceived() {
            @Override
            public void OnComplete(boolean isQueryEmpty, int numberOfComment, Map<Integer, Map> commentAroundYou) {
                if (!isQueryEmpty) {
                    CommentList = DatabaseAdapter.toListConverter(commentAroundYou);
                    for( int i = commentAroundYou.size()-1; i >= 0; i--){
                        Map< String, Object> thatComment = commentAroundYou.get( i); //salvo i risultati
                        CommentList =  DatabaseAdapter.toListConverter(commentAroundYou);
                        if( markerMap.get( thatComment.get("Document_Id")) == null) { //ho già questo marker?
                            Marker localMarkerRef = mMap.addMarker(new MarkerOptions().position(DatabaseAdapter.toLatLngConverter((GeoPoint) thatComment.get("Geo_Point")))
                                    .title((String) thatComment.get("Title"))
                                    .snippet((String) thatComment.get("Body"))
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));//aggiungo il marker sulla mappa
                            localMarkerRef.setTag( thatComment.get("Document_Id")); //aggiungo al marker il "Document_Id" del commento
                            if( LOG_SWITCH)
                                Log.d( MAIN_ACTIVITY, "setReceiveDataListener : marker Id : " + localMarkerRef.getId());
                            markerMap.put((String) thatComment.get("Document_Id"), localMarkerRef); //aggiungo il marker alla lista dei marker
                        }
                    }
                    if (MainActivity.LOG_SWITCH) {
                        Log.d(MAIN_ACTIVITY, "setReceiveDataListener : " + commentAroundYou.size() + " comments");
                    }
                }
            }
        });
    }

}
