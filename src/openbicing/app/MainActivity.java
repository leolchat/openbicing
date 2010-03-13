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
	private OpenBicingFastDbAdapter mFastDbHelper;
	
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
	    				Log.i("openBicing","LOL");
	    			}
	    		}
	    	}
	    };
	    
	    stations = new StationOverlayList(this,mapOverlays,paintHandler);
		
    	
	    Handler handler = new Handler(){
	    	public void handleMessage(Message msg){
	    		switch(msg.what){
	    		case OpenBicingFastDbAdapter.FETCH:
	    			Log.i("openBicing","Data fetched");
	    			break;
	    		case OpenBicingFastDbAdapter.UPDATE_MAP:
	    			Log.i("openBicing","Map Updated");
	    			progressDialog.dismiss();
	    			break;
	    		case OpenBicingFastDbAdapter.UPDATE_DATABASE:
	    			Log.i("openBicing","Database updated");
	    			break;
	    		case OpenBicingFastDbAdapter.NETWORK_ERROR:
	    			Log.i("openBicing","Network error, last update from "+mFastDbHelper.getLastUpdated());
	    			Toast toast = Toast.makeText(getApplicationContext(),"Network error,  last update from "+mFastDbHelper.getLastUpdated(),Toast.LENGTH_LONG);
			    	toast.show();
	    			break;
	    		}
	    	}
	    };
	    
	    mFastDbHelper = new OpenBicingFastDbAdapter(this, mapView, handler, stations);
	    if (savedInstanceState!=null){
	    	stations.updateHome();
	    	stations.getHome().setRadius(savedInstanceState.getInt("homeRadius"));
	    	this.view_all = savedInstanceState.getBoolean("view_all");
	    }else{
	    	updateHome();
	    }
	    try{
	    	mFastDbHelper.loadStations();
	    	if (savedInstanceState==null){
		    	Toast toast = Toast.makeText(this.getApplicationContext(),"Last Updated: "+mFastDbHelper.getLastUpdated(),Toast.LENGTH_LONG);
		    	toast.show();
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
	
	
	private void fillData(GeoPoint center, int radius) {
		try{
			mFastDbHelper.paintLessStations(center,radius);
		}catch (Exception e){
			Log.i("openBicing","Error Updating?");
			e.printStackTrace();
		};
    }
	
	private void fillData(boolean all) {
		if (!all)
			mFastDbHelper.setHome(stations.getHome().getPoint(), stations.getHome().getRadius());
		
		progressDialog = new ProgressDialog(this);
		progressDialog.setTitle("");
		progressDialog.setMessage("Loading. Please wait...");
    	progressDialog.show();
		try{
			mFastDbHelper.syncStations(all);
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

        // This is our one standard application action -- inserting a
        // new note into the list.
        menu.add(0, MENU_ITEM_SYNC, 0, R.string.menu_sync)
                .setIcon(R.drawable.refresh);
        menu.add(0, MENU_ITEM_LOCATION, 0, R.string.menu_location)
        	.setIcon(android.R.drawable.ic_menu_mylocation);
        menu.add(0, MENU_ITEM_WHATEVER, 0, R.string.menu_view_all)
    	.setIcon(android.R.drawable.ic_menu_mylocation);
        return true;
    }
	
	public void updateHome(){
		stations.updateHome();
		mapView.getController().setCenter(stations.getHome().getPoint());
		mapView.getController().setZoom(16);
	}
	
	public void view_all(){
		try{
			mFastDbHelper.paintAllStations();
		}catch(Exception e){};
	}
	
	public void view_near(){
		this.fillData(stations.getHome().getPoint(),stations.getHome().getRadius());
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
        		view_all();
        	}else{
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
