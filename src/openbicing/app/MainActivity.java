package openbicing.app;

import java.util.List;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;

public class MainActivity extends MapActivity{

	
	
	private MapView mapView;
	public static final int MENU_ITEM_SYNC = Menu.FIRST;
	public static final int MENU_ITEM_LOCATION = Menu.FIRST+1;	
	private StationOverlayList stations;
	private OpenBicingFastDbAdapter mFastDbHelper;
	
	private ProgressDialog progressDialog;
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.main);
	    
	    mapView = (MapView) findViewById(R.id.mapview);
	    mapView.setBuiltInZoomControls(true);
	    
	    List <Overlay> mapOverlays = mapView.getOverlays();
	    
	    stations = new StationOverlayList(this,mapOverlays);
		
    	
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
	    		}
	    	}
	    };
	    
	    mFastDbHelper = new OpenBicingFastDbAdapter(this, mapView, handler, stations);
	    if (savedInstanceState!=null){
	    	stations.updateHome();
	    	stations.getHome().setRadius(savedInstanceState.getInt("homeRadius"));
	    }else{
	    	updateHome();
	    }
	    fillData();
	    
	    Log.i("openBicing","CREATE!");
	}
	
	private void fillData() {
		progressDialog = new ProgressDialog(this);
		progressDialog.setTitle("");
		progressDialog.setMessage("Loading. Please wait...");
    	progressDialog.show();
		try{
			mFastDbHelper.syncStations();
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
        return true;
    }
	
	public void updateHome(){
		stations.updateHome();
		mapView.getController().setCenter(stations.getHome().getPoint());
		mapView.getController().setZoom(16);
	}
	
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_ITEM_SYNC:
            try{
            	this.fillData();
            }catch(Exception e){};
            return true;
        case MENU_ITEM_LOCATION:
            updateHome();
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
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i("openBicing","PAUSE!");
    }
}
