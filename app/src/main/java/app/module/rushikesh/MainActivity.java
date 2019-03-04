package app.module.rushikesh;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStates;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.GeoPoint;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;

public class MainActivity extends AppCompatActivity {//implements LocationListener {

    private static final int PERMISSION_REQUEST = 1;
    public final static String TAG = "MainActivity";

    //GPS
    FirebaseFirestore db;
    FirebaseFirestoreSettings settings;
    LocationManager locationManager;
    private double latitude;
    private double longitude;
    static MainActivity instance;
    private LocationRequest locationRequest;
    FusedLocationProviderClient fusedLocationProviderClient;

    //Google Sign in
    private static final int RC_SIGN_IN = 9001;

    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private ProgressDialog registerDialog;

    public static MainActivity getInstance() {
        return instance;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //To Grant the permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            requestPermission();
        instance = this;
        Dexter.withActivity(this).withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse response) {
                        updateLocation();
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse response) {
                        Toast.makeText(MainActivity.this, "You must allow this permission", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {

                    }
                }).check();
        FirebaseApp.initializeApp(this);

        locationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            enableGPSAutoMatically();
        }
        registerDialog=new ProgressDialog(this);
        registerDialog.setMessage("Please Wait a While...");
        setup();
        mAuth = FirebaseAuth.getInstance();
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    public void onSignUpClicked(View view){
        signIn();
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        Log.d(TAG, "firebaseAuthWithGoogle:" + acct.getId());
        registerDialog.show();
        registerDialog.setCanceledOnTouchOutside(false);
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithCredential:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            registerDialog.dismiss();
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            updateUI(null);
                        }
                    }
                });
    }

    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }



    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                // Google Sign In failed, update UI appropriately
                Log.w(TAG, "Google sign in failed", e);
                // ...
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        updateUI(mAuth.getCurrentUser());
    }

    private void updateUI(FirebaseUser user) {
        if (user != null) {
            Toast.makeText(MainActivity.this, "User already Logged in", Toast.LENGTH_SHORT).show();
        } else { }
    }


    public void enableGPSAutoMatically() {
        GoogleApiClient googleApiClient = null;
        if (googleApiClient == null) {
            googleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(LocationServices.API).addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                        @Override
                        public void onConnected(@Nullable Bundle bundle) {

                        }

                        @Override
                        public void onConnectionSuspended(int i) {

                        }
                    })
                    .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                        @Override
                        public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
                        }
                    }).build();
            googleApiClient.connect();
            buildLocationRequest();

            LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                    .addLocationRequest(locationRequest);

            // **************************
            builder.setAlwaysShow(true); // this is the key ingredient
            // **************************

            PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi
                    .checkLocationSettings(googleApiClient, builder.build());
            result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
                @Override
                public void onResult(LocationSettingsResult result) {
                    final Status status = result.getStatus();
                    final LocationSettingsStates state = result
                            .getLocationSettingsStates();
                    switch (status.getStatusCode()) {
                        case LocationSettingsStatusCodes.SUCCESS:

                            break;
                        case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                            Toast.makeText(getApplicationContext(), "GPS is not on", Toast.LENGTH_SHORT).show();
                            // Location settings are not satisfied. But could be fixed by showing the user a dialog.
                            try {
                                // Show the dialog by calling startResolutionForResult(), and check the result in onActivityResult().
                                status.startResolutionForResult(MainActivity.this, 1000);
                                updateLocation();

                            } catch (IntentSender.SendIntentException e) {
                                // Ignore the error.
                            }
                            break;
                        case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                            Toast.makeText(getApplicationContext(), "Setting change not allowed", Toast.LENGTH_SHORT).show();
                            // Location settings are not satisfied. However, we have no way to fix the settings so we won't show the dialog.
                            break;
                    }
                }
            });
        }
    }

    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST);
        }
    }


    public void checkOnMap(View view) {
        startActivity(new Intent(MainActivity.this, PlottingEmp.class));
    }



    //Background
    private void buildLocationRequest() {
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(1000);
        locationRequest.setSmallestDisplacement(0);
    }


    private void updateLocation() {
        buildLocationRequest();
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, getPendingIntent());
    }

    public void toastLocation(final String loc){
        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, loc, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private PendingIntent getPendingIntent() {
        Intent intent = new Intent(this,MyLocationService.class);
        intent.setAction(MyLocationService.ACTION_PROCESS_UPDATE);
        return PendingIntent.getBroadcast(this,0,intent,PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public void setup() {
        db = FirebaseFirestore.getInstance();
        settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build();
        db.setFirestoreSettings(settings);
        Toast.makeText(this, "Done", Toast.LENGTH_SHORT).show();
    }
/*
    private void upload() {
        GeoPoint geoPoint = new GeoPoint(latitude, longitude);
        Map<String, Object> city = new HashMap<>();
        city.put("Name", "Rushikesh Znawar");
        city.put("GeoPoint", geoPoint);

        db.collection("Working").document("Ahmednagar").set(city)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "DocumentSnapshot successfully written!");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error writing document", e);
                    }
                });
    }


     @Override
    public void onLocationChanged(Location location) {
        Toast.makeText(MainActivity.this, "Longitude : " + location.getLongitude() + "Latitude :" + location.getLatitude(), Toast.LENGTH_SHORT).show();
        latitude = location.getLatitude();
        longitude = location.getLongitude();
        upload();
    }

    @SuppressLint("MissingPermission")
    public Location getLocation() {
        Location location;
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER, 500, 1, this);
            location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

        } else {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500, 1, this);
            location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        }
        return location;
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }
*/
}
