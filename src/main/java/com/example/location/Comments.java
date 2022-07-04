package com.example.location;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.location.adapters.DatabaseAdapter;
import com.example.location.adapters.ListAdapter;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.firebase.firestore.GeoPoint;

import java.util.LinkedList;
import java.util.Map;

public class Comments extends AppCompatActivity {
    Toolbar toolbar;
    GoogleSignInAccount account;
    private GeoPoint minCoord, maxCoord;
    private boolean reload = false;

    private LinkedList<Map> CommentList = new LinkedList<>();;
    private RecyclerView mRecyclerView;
    private ListAdapter mAdapter;
    private final String COMMENTS = "Comments";
    public static Context commentsContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.comments_view);
        Comments.commentsContext = this;

        Bundle extra = getIntent().getBundleExtra( MainActivity.EXTRA_TAG);
        if( extra != null){
            minCoord = new GeoPoint( extra.getDouble(MainActivity.MIN_LAT), extra.getDouble(MainActivity.MIN_LOG));
            maxCoord = new GeoPoint( extra.getDouble(MainActivity.MAX_LAT), extra.getDouble(MainActivity.MAX_LOG));
        }else{
            reload = true;
            minCoord = MainActivity.minCoord;
            maxCoord = MainActivity.maxCoord;
        }

        //creo la toolbar
        toolbar = findViewById(R.id.toolbar3);
        toolbar.setTitle("Geopin Comments");
        toolbar.setNavigationIcon(R.drawable.ic_back);
        setSupportActionBar(toolbar);
        mRecyclerView = findViewById(R.id.recyclerview);
        // se non ho la lista di commenti la scarico dal database
        if( MainActivity.CommentList == null || reload) {
            reload = false;
            CommentList = new LinkedList<>();
            MainActivity.database.getCommentAroundYou( minCoord,  maxCoord, 100, 0);
            MainActivity.database.setReceiveDataListener(new DatabaseAdapter.OnCommentAroundYouReceived() {
                @Override
                public void OnComplete(boolean isQueryEmpty, int numberOfComment, Map<Integer, Map> commentAroundYou) {
                    if (!isQueryEmpty) {
                        setMapList(DatabaseAdapter.toListConverter(commentAroundYou));
                        if (MainActivity.LOG_SWITCH) {
                            Log.d(COMMENTS, "setReceiveDataListener : " + commentAroundYou.size() + " comments");
                        }
                    }
                }
            });
        }else{
            if( MainActivity.CommentList.size() == 0){
                CommentList = new LinkedList<>();
                MainActivity.database.getCommentAroundYou( minCoord,  maxCoord, 100, 0);
                MainActivity.database.setReceiveDataListener(new DatabaseAdapter.OnCommentAroundYouReceived() {
                    @Override
                    public void OnComplete(boolean isQueryEmpty, int numberOfComment, Map<Integer, Map> commentAroundYou) {
                        if (!isQueryEmpty) {
                            setMapList(DatabaseAdapter.toListConverter(commentAroundYou));
                            if (MainActivity.LOG_SWITCH) {
                                Log.d(COMMENTS, "setReceiveDataListener : " + commentAroundYou.size() + " comments");
                            }
                        }
                    }
                });
            }else {
                CommentList = MainActivity.CommentList;
            }
        }
        mAdapter = new ListAdapter(this, CommentList);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Create recycler view.
    }

    //carica la lista di commenti nella recyclerView
    private void setMapList( LinkedList<Map> listInput){
        CommentList = listInput;
        mAdapter = new ListAdapter(this, CommentList);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    //controlla se si è loggati e crea l'oggetto account
    public  void onStart(){
        super.onStart();
        account = GoogleSignIn.getLastSignedInAccount(this);
    }

    //tasto che porta a text_insert per mettere il commento
    public void insertMe(View view){
        if(account!=null){
        Intent nextActivity = new Intent(this, TextInsert.class);
        startActivity(nextActivity);
        }else{
            Intent nextActivity = new Intent(this, Settings.class);
            startActivity(nextActivity);
            Toast.makeText(Comments.this,"You should be logged to comment!",Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    //per tornare indietro
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    //invia alla Main Activity le coordinate del commento selezionato, dopo 500ms riporta nella main activity
    public void finishComments( double latitude, double longitude){
        Intent positionFromComments = new Intent( );
        positionFromComments.setAction(ListAdapter.POSITION_FROM_COMMENTS);
        positionFromComments.putExtra( MainActivity.LATITUDE_FROM_COMMENTS, latitude);
        positionFromComments.putExtra( MainActivity.LONGITUDE_FROM_COMMENTS, longitude);
        Log.d( "Comments", "finishComments : positionFromComments : " + positionFromComments.toString());
        sendBroadcast( positionFromComments);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                finish();
            }
        }, 500);
    }

}
