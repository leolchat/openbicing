package openbicing.app;

import java.util.List;

import android.app.ProgressDialog;
import android.content.Context;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;

public class MainActivity extends MapActivity{

	
	
	private MapView mapView;
	public static final int MENU_ITEM_SYNC = Menu.FIRST;
	public static final int MENU_ITEM_LOCATION = Menu.FIRST+1;
	public static final int MENU_ITEM_WHATEVER = Menu.FIRST+2;	
	private StationOverlayList stations;	
	private StationsDBAdapter mDbHelper;
	private boolean view_all = false;
	private HomeOverlay hOverlay;
	private ProgressDialog progressDialog;
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.main);
	    
	    mapView = (MapView) findViewById(R.id.mapview);
	    mapView.setBuiltInZoomControls(true);
	    
	    List <Overlay> mapOverlays = mapView.getOverlays();
	    
	    Handler paintHandler = new Handler(){
	    	public void handleMessage(Message msg){
	    		if(msg.what == hOverlay.MOTION_CIRCLE_STOP && !view_all){
	    			try{
	    				view_near();
	    			}catch(Exception e){
	    				
	    			}
	    		}
	    	}
	    };
	    
	    stations = new StationOverlayList(this,mapOverlays,paintHandler);
		
    	
	    Handler handler = new Handler(){
	    	public void handleMessage(Message msg){
	    		switch(msg.what){
	    		case StationsDBAdapter.FETCH:
	    			Log.i("openBicing","Data fetched");
	    			break;
	    		case StationsDBAdapter.UPDATE_MAP:
	    			Log.i("openBicing","Map Updated");
	    			progressDialog.dismiss();
	    			break;
	    		case StationsDBAdapter.UPDATE_DATABASE:
	    			Log.i("openBicing","Database updated");
	    			break;
	    		case StationsDBAdapter.NETWORK_ERROR:
	    			Log.i("openBicing","Network error, last update from "+mDbHelper.getLastUpdated());
	    			Toast toast = Toast.makeText(getApplicationContext(),"Network error,  last update from "+mDbHelper.getLastUpdated(),Toast.LENGTH_LONG);
			    	toast.show();
	    			break;
	    		}
	    	}
	    };
	    mDbHelper = new StationsDBAdapter(this, mapView, handler, stations);
	    if (savedInstanceState!=null){
	    	stations.updateHome();
	    	stations.getHome().setRadius(savedInstanceState.getInt("homeRadius"));
	    	this.view_all = savedInstanceState.getBoolean("view_all");
	    }else{
	    	updateHome();
	    }
	    try{
	    	mDbHelper.loadStations();
	    	if (savedInstanceState==null){
	    		String strUpdated = mDbHelper.getLastUpdated();
	    		if (strUpdated==null){
	    			this.fillData(view_all);	
	    		}else{
			    	Toast toast = Toast.makeText(this.getApplicationContext(),"Last Updated: "+mDbHelper.getLastUpdated(),Toast.LENGTH_LONG);
			    	toast.show();
	    		}
		    }
	    	
	    }catch (Exception e){
	    	Log.i("openBicing","SHIT ... SUCKS");
	    };
	    
	    
	    
	    if(view_all)
	    	view_all();
	    else
	    	view_near();
	    hOverlay = stations.getHome();
	    Log.i("openBicing","CREATE!");
	}
	
	
	private void fillData(boolean all) {
		Bundle data = new Bundle();
		if (!all){
			GeoPoint center = stations.getHome().getPoint();
			data.putInt(StationsDBAdapter.CENTER_LAT_KEY, center.getLatitudeE6());
			data.putInt(StationsDBAdapter.CENTER_LNG_KEY, center.getLongitudeE6());
			data.putInt(StationsDBAdapter.RADIUS_KEY, stations.getHome().getRadius());
		}
		
		data.putString(StationsDBAdapter.STATION_PROVIDER_KEY, StationsDBAdapter.BICING_PROVIDER);
		
		progressDialog = new ProgressDialog(this);
		progressDialog.setTitle("");
		progressDialog.setMessage("Loading. Please wait...");
    	progressDialog.show();
		try{
			mDbHelper.sync(all,data);
		}catch (Exception e){
			Log.i("openBicing","Error Updating?");
			e.printStackTrace();
			progressDialog.dismiss();
		};
    }
	
	@Override
	protected boolean isRouteDisplayed() {
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, MENU_ITEM_SYNC, 0, R.string.menu_sync)
                .setIcon(R.drawable.refresh);
        menu.add(0, MENU_ITEM_LOCATION, 0, R.string.menu_location)
        	.setIcon(android.R.drawable.ic_menu_mylocation);
        menu.add(0, MENU_ITEM_WHATEVER, 0, R.string.menu_view_all)
    	.setIcon(android.R.drawable.checkbox_off_background);
        return true;
    }
	
	public void updateHome(){
		stations.updateHome();
		mapView.getController().setCenter(stations.getHome().getPoint());
		mapView.getController().setZoom(16);
	}
	
	public void view_all(){
		try{
			mDbHelper.populateStations();
		}catch(Exception e){};
	}
	
	public void view_near(){
		try{
			mDbHelper.populateStations(stations.getHome().getPoint(), stations.getHome().getRadius());
		}catch(Exception e){};
	}
	
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_ITEM_SYNC:
            try{
            	this.fillData(view_all);
            }catch(Exception e){};
            return true;
        case MENU_ITEM_LOCATION:
            updateHome();
            return true;
        case MENU_ITEM_WHATEVER:
        	if (!view_all){
        		item.setIcon(android.R.drawable.checkbox_on_background);
        		view_all();
        	}else{
        		item.setIcon(android.R.drawable.checkbox_off_background);
        		view_near();	
        	}
        	view_all=!view_all;
        	return true;
        }
        return super.onOptionsItemSelected(item);
    }
	
	@Override
    protected void onResume() {
        super.onResume();
        Log.i("openBicing","RESUME!");
	}
	
	protected void onSaveInstanceState(Bundle outState) {
        Log.i("openBicing","SaveInstanceState!");
        outState.putInt("homeRadius", stations.getHome().getRadius());
        outState.putBoolean("view_all", view_all);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i("openBicing","PAUSE!");
    }
}
