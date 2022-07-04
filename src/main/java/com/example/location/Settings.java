package com.example.location;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.location.adapters.DatabaseAdapter;
import com.example.location.adapters.ListAdapter;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.squareup.picasso.Picasso;

import java.util.LinkedList;
import java.util.Map;

import static com.google.android.gms.common.api.CommonStatusCodes.getStatusCodeString;

public class Settings extends AppCompatActivity implements View.OnClickListener{
    Toolbar toolbar;
    private static final String TAG = "SignInActivity";
    private static final int RC_SIGN_IN = 9001;
    public static GoogleSignInAccount account;
    private GoogleSignInClient mGoogleSignInClient;
    private TextView mStatusTextView;
    private TextView mEmailTextView;
    private ImageView profileImage;
    private boolean LOG_ENABLE = false;
    public static Context settingsContext;
    private LinkedList<Map> CommentList = new LinkedList<>();;
    private RecyclerView mRecyclerView;
    private ListAdapter mAdapter;
    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        if(LOG_ENABLE) Log.d( "onCreateSettings", "Creazione activity Settings");
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);
        Settings.settingsContext = this;
        //Creazione della toolbar
        toolbar = findViewById(R.id.settingsToolbar);
        toolbar.setTitle("App settings");
        toolbar.setNavigationIcon(R.drawable.ic_back);
        setSupportActionBar(toolbar);
        //-----------------------------

        mRecyclerView = findViewById(R.id.recyclerview);

        // Gestione Profilo
        // Views
        mStatusTextView = findViewById(R.id.status);
        mEmailTextView = findViewById(R.id.email);

        //ProfilePhoto
        profileImage=findViewById(R.id.profileImage);

        // Button listeners
        findViewById(R.id.sign_in_button).setOnClickListener(this);
        findViewById(R.id.sign_out_button).setOnClickListener(this);
        findViewById(R.id.disconnect_button).setOnClickListener(this);

        // Configurazione sign-in per richiedere ID, email address, e informazioni di base
        // del profilo dell'Utente. ID e info di base sono insclusi in DEFAULT_SIGN_IN.
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail().build();

        // Creazione GoogleSignInClient con le caratteristiche specificate da gso.
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Sign-in Button.
        SignInButton signInButton = findViewById(R.id.sign_in_button);
        signInButton.setSize(SignInButton.SIZE_STANDARD);
        signInButton.setColorScheme(SignInButton.COLOR_LIGHT);
    }
    @Override
    //Per tornare indietro
    public boolean onOptionsItemSelected(MenuItem item) {
        // Auto-generated method stub
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if(LOG_ENABLE) Log.d( "onStartSettings", "updateUI");
        // Controllo l'esistenza di un account Google loggato, se account collegato
        // GoogleSignInAccount sarà non-null.
        account = GoogleSignIn.getLastSignedInAccount(this);
        updateUI(account);

    }

    //popolo la lista dei commenti dell'utente
    private void setMapList( LinkedList<Map> listInput){
        CommentList = listInput;
        mAdapter = new ListAdapter(this, CommentList);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    // Risultato dall'Activity
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Risultato ottenuto dal lancio dell' Intent da GoogleSignInClient.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            // Task sempre verificata, non è necessario un listener.
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }
    }

    // Prendo ciò che mi serve dall'account
    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
             account = completedTask.getResult(ApiException.class);
            if(LOG_ENABLE) Log.d( "SignIn", "login avvenuto con successo");
            // Login avvenuto con successo, comunico le info al DB
            Intent intent = new Intent( MainActivity.SAVE_SHARED_PREF_ACCOUNT);
            MainActivity.database.addProfile(account.getId(), account.getDisplayName(), account.getFamilyName(), "2020");
            intent.putExtra( MainActivity.SHARED_PREF_PROFILE_ID, account.getId());
            intent.putExtra( MainActivity.SHARED_PREF_PROFILE_NAME, account.getDisplayName());
            MainActivity.database.updateProfileVisibility( MainActivity.localProfileId, true); //rendo visibile il profilo e i relativi commenti
            sendBroadcast(intent);
            updateUI(account);
        } catch (ApiException e) {
            // Catturo la possibile eccezione e ne controllo il motivo
            Log.w(TAG, "signInResult:failed code=" + getStatusCodeString(e.getStatusCode()));
            updateUI(null);
        }
    }

    // SignIn
    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    // SignOut
    private void signOut() {
        mGoogleSignInClient.signOut()
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        //logOut
                        updateUI(null);
                    }
                });
    }


    // Disconnect
    private void revokeAccess() {
        mGoogleSignInClient.revokeAccess()
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        //logOut e revoco i permessi che ho dato all'app accedendo
                        updateUI(null);
                        MainActivity.database.updateProfileVisibility( MainActivity.localProfileId, false);//metto non visibile il profilo e i relativi commenti
                    }
                });
    }

    //aggiorno l'interfaccia utente nel caso sia loggato mostrando i suoi dati e i commenti postati, altrimenti chiedo l'accesso
    private void updateUI(@Nullable GoogleSignInAccount account) {
        if (account != null) {
            if(LOG_ENABLE) Log.d( "updateUI", "Signed in");
            mStatusTextView.setText(getString(R.string.signed_in_fmt, account.getDisplayName()));
            mEmailTextView.setText(account.getEmail());
            Picasso.get().load(account.getPhotoUrl()).placeholder(R.drawable.g__logo1200px).into(profileImage);

            findViewById(R.id.email).setVisibility(View.VISIBLE);
            findViewById(R.id.sign_out_and_disconnect).setVisibility(View.VISIBLE);

            findViewById(R.id.sign_in_button).setVisibility(View.GONE);

            MainActivity.database.getCommentFromProfile( account.getId());
            MainActivity.database.setReceiveDataListener(new DatabaseAdapter.OnCommentOfProfileReceived() {
                @Override
                public void OnComplete(boolean isQueryEmpty, int numberOfComment, Map<Integer, Map> commentOfProfile) {//scarico i commenti del profilo che è loggato
                    if (!isQueryEmpty) {
                        setMapList(DatabaseAdapter.toListConverter(commentOfProfile));
                        if (MainActivity.LOG_SWITCH) {
                            Log.d("Settings", "setReceiveDataListener : " + commentOfProfile.size() + " comments");
                        }
                    }
                }
            });
            mAdapter = new ListAdapter(this, CommentList);
            mRecyclerView.setAdapter(mAdapter);
            mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        } else {
            if(LOG_ENABLE) Log.d( "updateUI", "Signed out");
            mStatusTextView.setText(R.string.signed_out);
            profileImage.setImageResource(R.drawable.g__logo1200px);

            findViewById(R.id.sign_in_button).setVisibility(View.VISIBLE);
            findViewById(R.id.email).setVisibility(View.GONE);
            findViewById(R.id.sign_out_and_disconnect).setVisibility(View.GONE);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.sign_in_button:
                signIn();
                break;
            case R.id.sign_out_button:
                signOut();
                break;
            case R.id.disconnect_button:
                revokeAccess();
                break;
        }
    }
}
