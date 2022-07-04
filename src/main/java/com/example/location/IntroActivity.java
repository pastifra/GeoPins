package com.example.location;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import com.example.location.adapters.IntroViewPagerAdapter;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

public class IntroActivity extends AppCompatActivity {
    private ViewPager screenPager;
    IntroViewPagerAdapter introViewPagerAdapter;
    TabLayout tabIndicator;
    Button btnNext;
    int position = 0;
    SharedPreferences pref;

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        //controlla se è già stato visualizzato il tutorial, se si manda direttamente alla main activity
         if(restorePrefData()== true){
             Intent mainActivity = new Intent(getApplicationContext(),MainActivity.class);
             startActivity(mainActivity);
             finish();;
         }

        setContentView(R.layout.intro_activity);





        btnNext= findViewById(R.id.buttonNext);
        tabIndicator = findViewById(R.id.tabLayout);

        //Aggiunge le schermate del tutorial
        final List<ScreenItem> mList = new ArrayList<>();
        mList.add(new ScreenItem("Welcome","Hi! Nice to meet you, here is a short guide on how to use the app",R.drawable.logo_intro));
        mList.add(new ScreenItem("See the Geopins","Click the button and read all the geopins around you or add a new geopin linked to your position",R.drawable.mappa_intro));
        mList.add(new ScreenItem("Manage your account","From the profile section you can login and see all your comments, click on one of them to make it invisible. Disconnect to remove your account and make all your geopins invisible",R.drawable.settings_intro));

        screenPager = findViewById(R.id.view_pager);
        introViewPagerAdapter = new IntroViewPagerAdapter(this,mList);

        screenPager.setAdapter(introViewPagerAdapter);
        //aggiorna i bottoni e il tab indicator in base alla posizone
        tabIndicator.setupWithViewPager(screenPager);
        tabIndicator.addOnTabSelectedListener(new TabLayout.BaseOnTabSelectedListener() {
                                                      @Override
                                                      public void onTabSelected(TabLayout.Tab tab) {
                                                          if(tab.getPosition() == mList.size() -1){
                                                              position++;
                                                              screenPager.setCurrentItem(position);
                                                              btnNext.setText("START");
                                                          }
                                                          else if(tab.getPosition() < mList.size() -1){
                                                              btnNext.setText("NEXT");
                                                          }

                                                      }

                                                      @Override
                                                      public void onTabUnselected(TabLayout.Tab tab) {

                                                      }

                                                  @Override
                                                  public void onTabReselected(TabLayout.Tab tab) {

                                                  }
                                              });
                //per andare avanti coi bottoni
                btnNext.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        position = screenPager.getCurrentItem();
                        if (position < mList.size() - 2) {
                            position++;
                            screenPager.setCurrentItem(position);
                            btnNext.setText("NEXT");
                        }
                        else if(position == mList.size() -2 ){
                            position++;
                            screenPager.setCurrentItem(position);
                            btnNext.setText("START");
                        }
                        else {
                            Intent mainActivity = new Intent(getApplicationContext(),MainActivity.class);
                            startActivity(mainActivity);

                            savePrefsData();
                            finish();
                        }
                    }
                });

    }

    //controlla in sharepreference se è stata già visualizzata l'introduzione
    private boolean restorePrefData() {
        pref = getApplicationContext().getSharedPreferences("myPrefs",MODE_PRIVATE);
        Boolean isOpenedBefore = pref.getBoolean("isIntroOpened",false);
        return isOpenedBefore;
    }

    //salva se è stato già visualizzato il tutorial
    private void savePrefsData() {
        pref = getApplicationContext().getSharedPreferences("myPrefs",MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putBoolean("isIntroOpened",true);
        editor.commit();
    }
}
