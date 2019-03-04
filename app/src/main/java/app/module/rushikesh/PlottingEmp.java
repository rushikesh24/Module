package app.module.rushikesh;

import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

import java.util.HashMap;
import java.util.Map;

public class PlottingEmp extends FragmentActivity implements OnMapReadyCallback,Runnable {

    private static final String TAG = "Plotting Map";
    private GoogleMap mMap;
    private GeoPoint geoLocation[];
    private boolean showMap = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plotting_emp);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


        loadData();
    }

    public void loadData(){
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference docRef = db.collection("Details").document("8AsqIQIxVcxWurdlzEIp");
        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        Toast.makeText(PlottingEmp.this, "DocumentSnapshot data: " + document.getData(), Toast.LENGTH_SHORT).show();
                        geoLocation[0] = (GeoPoint) document.get("Location");
                        notifyAll();
                    } else {
                        Toast.makeText(PlottingEmp.this, "No such document", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(PlottingEmp.this, "get failed with " + task.getException(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
       showMap = true;
       mMap = googleMap;
    }


    public void drawMap(){
        // Add a marker in Sydney and move the camera
        LatLng sydney = new LatLng(geoLocation[0].getLatitude(), geoLocation[0].getLongitude());
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        geoLocation[1] = new GeoPoint(100,100);
        sydney = new LatLng(geoLocation[1].getLatitude(), geoLocation[1].getLongitude());
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
    }

    @Override
    public void run() {
        while(showMap)
        {
            drawMap();
            break;
        }
    }
}
