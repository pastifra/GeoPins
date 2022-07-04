package com.example.location;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.widget.Toolbar;

import androidx.appcompat.app.AppCompatActivity;

import com.example.location.adapters.ListAdapter;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.picasso.Picasso;

import java.util.Calendar;

public class TextInsert extends AppCompatActivity {
    EditText commentInput, commentTitleInput;
    Toolbar toolbar;
    GoogleSignInAccount account;
    private final int delayToBeBack = 1000; //ms

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.text_insert);

        //creo l'edit text
        commentInput = findViewById(R.id.commentInput);
        commentTitleInput = findViewById(R.id.commentTitleInput);
        //creo la toolbar
        toolbar = findViewById(R.id.toolbar2);
        toolbar.setNavigationIcon(R.drawable.ic_back);
        toolbar.setTitle("");
        setSupportActionBar(toolbar);
    }

    public  void onStart(){
        super.onStart();
        account = GoogleSignIn.getLastSignedInAccount(this);
    }

    //metodo per inserire il commento , torna a comoment_view , salva la stringa e restituisce un toast
    public void doneMe(View v){
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);
        String date = day + "/" + month + "/" + year;
        if(commentTitleInput.getText().length()>1 && commentInput.getText().length()> 4) {
            //aggiungo il profilo nel database
            MainActivity.database.addComment(MainActivity.localProfileId, MainActivity.localProfileName, MainActivity.localPosition, commentTitleInput.getText().toString(), commentInput.getText().toString(), date);
            Toast.makeText(TextInsert.this, "Congratulations, your geopin comment has been registered", Toast.LENGTH_LONG).show();
            //dopo 500ms ritorno alla lista di commenti
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    finish();
                }
            }, delayToBeBack);
        }
        else{
            Toast.makeText(TextInsert.this, "Congratulations, you are a dummy \uD83D\uDE1C" +"\n" + "Please insert a longer title or comment \uD83D\uDE01", Toast.LENGTH_LONG).show();
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
}
