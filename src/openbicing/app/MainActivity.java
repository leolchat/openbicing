package openbicing.app;

import java.util.List;

import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;

public class MainActivity extends MapActivity{

	private OpenBicingDbAdapter mDbHelper;
	private Cursor stationsCursor;
	private MapView mapView;
	public static final int MENU_ITEM_SYNC = Menu.FIRST;
	public static final int MENU_ITEM_LOCATION = Menu.FIRST+1;
	
	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.main);
	    mapView = (MapView) findViewById(R.id.mapview);
	    mapView.setBuiltInZoomControls(true);
	    mDbHelper = new OpenBicingDbAdapter(this);
        try{mDbHelper.open();}catch (Exception e){Log.i("openBicing",e.getLocalizedMessage()); e.printStackTrace();};
        updateMap();
	    fillData();
	}
	
	private void fillData() {
		List <Overlay> mapOverlays = mapView.getOverlays();
    	stationsCursor = mDbHelper.fetchAllStations();
    	Drawable drawable = this.getResources().getDrawable(R.drawable.green_arrow);
    	StationOverlayList stations = new StationOverlayList(this,mapOverlays);
    	while (stationsCursor.moveToNext()){
    		int lat = Integer.parseInt(stationsCursor.getString(3));
    		int lng = Integer.parseInt(stationsCursor.getString(2));
    		
    	    GeoPoint point = new GeoPoint(lat, lng);
    	    int bikes = stationsCursor.getInt(4);
    	    int free = stationsCursor.getInt(5);
    	    String timestamp = "NOW";
    	    int id = stationsCursor.getInt(1);
    	    StationOverlay station = new StationOverlay(point,this,bikes,free,timestamp,id);
    	    stations.addStationOverlay(station);
    	}
    }
	
	@Override
	protected boolean isRouteDisplayed() {
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        // This is our one standard application action -- inserting a
        // new note into the list.
        menu.add(0, MENU_ITEM_SYNC, 0, R.string.menu_sync)
                .setShortcut('3', 'a')
                .setIcon(R.drawable.refresh);
        menu.add(0, MENU_ITEM_LOCATION, 0, R.string.menu_location)
        	.setShortcut('3', 'a')
        	.setIcon(android.R.drawable.ic_menu_mylocation);
        return true;
    }
	
	public void updateMap(){
		
		
		LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Location mylocation = locationManager.getLastKnownLocation("network");
        
        Double lat = mylocation.getLatitude()*1E6;
        Double lng = mylocation.getLongitude()*1E6;
                    
        GeoPoint point = new GeoPoint(lat.intValue(), lng.intValue());
        MapController mapController = mapView.getController();
        mapController.setZoom(18);
        mapController.setCenter(point);
	}
	
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_ITEM_SYNC:
            try{
            	this.mDbHelper.sync(); 
            	List <Overlay> mapOverlays = mapView.getOverlays();
            	mapOverlays.clear();
            	this.fillData();
            }catch(Exception e){};
            return true;
        case MENU_ITEM_LOCATION:
            updateMap();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
