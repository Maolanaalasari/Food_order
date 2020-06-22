package com.example.munche;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import Fragments.ExploreFragment;
import Fragments.FavouriteFragment;
import Fragments.MyProfileFragment;
import Fragments.RestaurantFragment;
import UI.LoginActivity;
import Utils.GPSTracker;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseUser mCurrentUser;
    private List<Address> addresses;
    private Geocoder geocoder;
    private GPSTracker gpsTracker;
    private String address,city,state,country,postalCode,knownName,subLocality,subAdminArea,uid;
    private Toolbar mToolbar;
    private FirebaseFirestore db;
    private FirebaseUser firebaseUser;
    private double latitude,longitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();
        checkPermission();
        getLocation();
        setToolBarLocation();

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnNavigationItemSelectedListener(navListener);

    }

    private void init() {
        mAuth = FirebaseAuth.getInstance();
        firebaseUser = mAuth.getCurrentUser();
        mCurrentUser = mAuth.getCurrentUser();
        db = FirebaseFirestore.getInstance();
    }

    private void checkPermission() {
        try {
            if (ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ) {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 101);
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    private void getLocation() {
        gpsTracker = new GPSTracker(MainActivity.this);
        if(gpsTracker.canGetLocation()){
           latitude = gpsTracker.getLatitude();
           longitude = gpsTracker.getLongitude();

            geocoder = new Geocoder(MainActivity.this, Locale.getDefault());

            try {
                addresses = geocoder.getFromLocation(latitude, longitude, 1); // Here 1 represent max location result to returned, by documents it recommended 1 to 5
            } catch (IOException e) {
                e.printStackTrace();
            }

            address = addresses.get(0).getAddressLine(0); // If any additional address line present than only, check with max available address lines by getMaxAddressLineIndex()
            city = addresses.get(0).getLocality();
            state = addresses.get(0).getAdminArea();
            country = addresses.get(0).getCountryName();
            postalCode = addresses.get(0).getPostalCode();
            knownName = addresses.get(0).getFeatureName();
            subLocality = addresses.get(0).getSubLocality();
            subAdminArea = addresses.get(0).getSubAdminArea();

        }else{
            gpsTracker.showSettingsAlert();
        }
    }

    private void setToolBarLocation() {
        String finalAddress = knownName + ", " + subLocality +  ", " + city + ", " + postalCode;
        mToolbar = findViewById(R.id.customToolBar);
        setSupportActionBar(mToolbar);
//        getSupportActionBar().setTitle(null);
        TextView textView = findViewById(R.id.userLocation);
        textView.setText(finalAddress);
    }

    private BottomNavigationView.OnNavigationItemSelectedListener navListener =
            item -> {
                Fragment selectedFragment = null;
                switch (item.getItemId()) {
                    case R.id.nav_restaurants:
                        selectedFragment = new RestaurantFragment();
                        break;
                    case R.id.nav_explore:
                        selectedFragment = new ExploreFragment();
                        break;
                    case R.id.nav_favourites:
                        selectedFragment = new FavouriteFragment();
                        break;
                    case R.id.nav_profile:
                        selectedFragment = new MyProfileFragment();
                        break;
                }
                getSupportFragmentManager().beginTransaction().replace(R.id.fragmentContainer,
                        selectedFragment).commit();
                return true;
            };

    @Override
    protected void onStart() {
        super.onStart();
        if (mCurrentUser == null) {
            sendUserToLogin();
        }else {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, new RestaurantFragment())
                    .commit();
        }
    }

    private void sendUserToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        gpsTracker.stopUsingGPS();
    }
}
