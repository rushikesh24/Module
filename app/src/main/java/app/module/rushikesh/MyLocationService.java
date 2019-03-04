package app.module.rushikesh;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.IBinder;

import com.google.android.gms.location.LocationResult;

public class MyLocationService extends BroadcastReceiver {

    public static final String ACTION_PROCESS_UPDATE = "app.module.rushikesh.UPDATE_LOCAION";

    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent != null)
        {
            final String action = intent.getAction();
            if(ACTION_PROCESS_UPDATE.equals(action))
            {
                LocationResult result = LocationResult.extractResult(intent);
                if(result != null){
                    Location location = result.getLastLocation();
                    String value = new StringBuilder(""+location.getLatitude()).append("/").append(location.getLongitude()).toString();
                    try{
                        MainActivity.getInstance().toastLocation(value);
                    }catch (Exception e){

                    }
                }
            }
        }
    }
}
