package openbicing.app;

import java.util.List;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SlidingDrawer;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.LinearLayout.LayoutParams;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;

public class MainActivity extends MapActivity {

	private MapView mapView;
	public static final int MENU_ITEM_SYNC = Menu.FIRST;
	public static final int MENU_ITEM_LOCATION = Menu.FIRST + 1;
	public static final int MENU_ITEM_WHATEVER = Menu.FIRST + 2;
	public static final int MENU_ITEM_LIST = Menu.FIRST + 3;
	public static final int KEY_LAT = 0;
	public static final int KEY_LNG = 1;
	public static final int LIST_STATIONS_ACTIVITY = 0;
	
	private StationOverlayList stations;
	private StationsDBAdapter mDbHelper;
	private InfoLayer infoLayer;
	private boolean view_all = false;
	private HomeOverlay hOverlay;
	private ProgressDialog progressDialog;
	private FrameLayout fl;
	private SlidingDrawer sd;
	
	private int green, red, yellow;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		mapView = (MapView) findViewById(R.id.mapview);
		fl = (FrameLayout) findViewById(R.id.content);
		sd = (SlidingDrawer) findViewById(R.id.drawer);
		mapView.setBuiltInZoomControls(true);

		List<Overlay> mapOverlays = mapView.getOverlays();

		Handler paintHandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				if (msg.what == hOverlay.MOTION_CIRCLE_STOP && !view_all) {
					try {
						view_near();
					} catch (Exception e) {

					}
				} else if (msg.what == StationOverlay.TOUCHED && msg.arg1 != -1) {
					Log.i("openBicing", "touched: "
							+ Integer.toString(msg.arg1));
					stations.setCurrent(msg.arg1);
					infoLayer.populateFields(stations.getCurrent());
				}else if (msg.what == hOverlay.LOCATION_CHANGED){
					mDbHelper.setCenter(hOverlay.getPoint());
					try{
						mDbHelper.loadStations();
						
						if(view_all){
							view_all();
						}else{
							view_near();
						}
					}catch(Exception e){};
				}
			}
		};

		stations = new StationOverlayList(this, mapOverlays, paintHandler);

		Handler handler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				switch (msg.what) {
				case StationsDBAdapter.FETCH:
					Log.i("openBicing", "Data fetched");
					break;
				case StationsDBAdapter.UPDATE_MAP:
					Log.i("openBicing", "Map Updated");
					progressDialog.dismiss();
					StationOverlay current = stations.getCurrent();
					if (current==null){
							view_all = true;
							view_all();
							Toast toast = Toast.makeText(getApplicationContext(),
									"No bikes around you, so I am showing all of them :D ",Toast.LENGTH_LONG);
							toast.show();
							current = stations.getCurrent();
					}
					if (current!=null){
						current.setSelected(true);
						infoLayer.populateFields(current);
					}else{
						Log.i("openBicing","Error getting an station..");
					}
					mapView.invalidate();
					break;
				case StationsDBAdapter.UPDATE_DATABASE:
					Log.i("openBicing", "Database updated");
					break;
				case StationsDBAdapter.NETWORK_ERROR:
					Log.i("openBicing", "Network error, last update from "
							+ mDbHelper.getLastUpdated());
					Toast toast = Toast.makeText(getApplicationContext(),
							"Network error,  last update from "
									+ mDbHelper.getLastUpdated(),
							Toast.LENGTH_LONG);
					toast.show();
					break;
				}
			}
		};
		mDbHelper = new StationsDBAdapter(this, mapView, handler, stations);
		mDbHelper.setCenter(stations.getHome().getPoint());
		if (savedInstanceState != null) {
			stations.updateHome();
			stations.getHome().setRadius(
					savedInstanceState.getInt("homeRadius"));
			this.view_all = savedInstanceState.getBoolean("view_all");
		} else {
			updateHome();
		}
		try {
			mDbHelper.loadStations();
			if (savedInstanceState == null) {
				String strUpdated = mDbHelper.getLastUpdated();
				if (strUpdated == null) {
					this.fillData(view_all);
				} else {
					Toast toast = Toast.makeText(this.getApplicationContext(),
							"Last Updated: " + mDbHelper.getLastUpdated(),
							Toast.LENGTH_LONG);
					toast.show();
				}
			}

		} catch (Exception e) {
			Log.i("openBicing", "SHIT ... SUCKS");
		}
		;

		if (view_all)
			view_all();
		else
			view_near();
		hOverlay = stations.getHome();
		Log.i("openBicing", "CREATE!");

		infoLayer = (InfoLayer) findViewById(R.id.info_layer);

		Handler infoHandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				StationOverlay tmp;
				switch (msg.what) {
				case InfoLayer.NEXT_STATION:
					tmp = stations.selectNext();
					break;
				case InfoLayer.PREV_STATION:
					tmp = stations.selectPrevious();
					break;
				default:
					tmp = null;
				}
				if (tmp != null) {
					infoLayer.populateFields(tmp);
					mapView.postInvalidate();
					mapView.getController().animateTo(tmp.getCenter());
				}
			}
		};

		infoLayer.setElements((TextView) findViewById(R.id.station_id),
				(TextView) findViewById(R.id.ocupation),
				(TextView) findViewById(R.id.distance),
				(TextView) findViewById(R.id.walking_time), infoHandler);

		infoLayer.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mapView.getController().animateTo(infoLayer.getCurrentCenter());
			}
		});

		StationOverlay current = stations.getCurrent();
		if (current!=null){
				current.setSelected(true);
				infoLayer.populateFields(current);
		}
	}

	private void fillData(boolean all) {
		Bundle data = new Bundle();
		if (!all) {
			GeoPoint center = stations.getHome().getPoint();
			data.putInt(StationsDBAdapter.CENTER_LAT_KEY, center
					.getLatitudeE6());
			data.putInt(StationsDBAdapter.CENTER_LNG_KEY, center
					.getLongitudeE6());
			data.putInt(StationsDBAdapter.RADIUS_KEY, stations.getHome()
					.getRadius());
		}

		data.putString(StationsDBAdapter.STATION_PROVIDER_KEY,
				StationsDBAdapter.BICING_PROVIDER);

		progressDialog = new ProgressDialog(this);
		progressDialog.setTitle("");
		progressDialog.setMessage("Loading. Please wait...");
		progressDialog.show();
		try {
			mDbHelper.sync(all, data);
		} catch (Exception e) {
			Log.i("openBicing", "Error Updating?");
			e.printStackTrace();
			progressDialog.dismiss();
		}
		;
	}

	@Override
	protected boolean isRouteDisplayed() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.add(0, MENU_ITEM_SYNC, 0, R.string.menu_sync).setIcon(
				R.drawable.ic_menu_refresh);
		menu.add(0, MENU_ITEM_LOCATION, 0, R.string.menu_location).setIcon(
				android.R.drawable.ic_menu_mylocation);
		menu.add(0, MENU_ITEM_WHATEVER, 0, R.string.menu_view_all).setIcon(
				android.R.drawable.checkbox_off_background);
		return true;
	}

	public void updateHome() {
		try{
			stations.updateHome();
			mapView.getController().setCenter(stations.getHome().getPoint());
			mapView.getController().setZoom(16);
		}catch (Exception e){
			Log.i("openBicing","center is null..");
		}
	}

	public void view_all() {
		try {
			mDbHelper.populateStations();
			populateList(true);
		} catch (Exception e) {
		
		};
	}

	public void view_near() {
		try {
			mDbHelper.populateStations(stations.getHome().getPoint(), stations
					.getHome().getRadius());
			populateList(false);
		} catch (Exception e) {
			
		};
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_ITEM_SYNC:
			try {
				this.fillData(view_all);
			} catch (Exception e) {
			}
			;
			return true;
		case MENU_ITEM_LOCATION:
			updateHome();
			return true;
		case MENU_ITEM_WHATEVER:
			if (!view_all) {
				item.setIcon(android.R.drawable.checkbox_on_background);
				view_all();
			} else {
				item.setIcon(android.R.drawable.checkbox_off_background);
				view_near();
			}
			view_all = !view_all;
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onResume() {
		super.onResume();
		Log.i("openBicing", "RESUME!");
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		Log.i("openBicing", "SaveInstanceState!");
		outState.putInt("homeRadius", stations.getHome().getRadius());
		outState.putBoolean("view_all", view_all);
	}

	@Override
	protected void onPause() {
		super.onPause();
		Log.i("openBicing", "PAUSE!");
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == LIST_STATIONS_ACTIVITY){
			Log.i("openBicing","Coming back from list");
			if (resultCode == StationsListActivity.RESULT_OK){
				Log.i("openBicing","Everything looks ok");
				int position = data.getIntExtra(StationsListActivity.KEY_POSITION, -1);
				Log.i("openBicing","And position was "+Integer.toString(position));
				if (position!= -1){
					StationOverlay selected = stations.findById(position);
					if (selected!=null){
						stations.setCurrent(selected.getPosition());
						infoLayer.populateFields(selected);
						mapView.getController().setCenter(selected.getCenter());
						mapView.getController().setZoom(16);
					}
				}
			}
		}
	}
	
	public void populateList(boolean all){
		
		try {
			ListView lv = new ListView(this);
			List sts;
			if (all){
				sts = mDbHelper.getMemory();
			}else{
				sts = mDbHelper.getMemory(hOverlay.getRadius());	
			}
			
			green = R.drawable.green_gradient;
	        yellow = R.drawable.yellow_gradient;
	        red = R.drawable.red_gradient;
	        ArrayAdapter adapter = new ArrayAdapter(this,R.layout.stations_list_item, sts){
				LayoutInflater mInflater = getLayoutInflater();
	        	@Override
	        	public View getView(int position, View convertView, ViewGroup parent){
	        		View row;
	        		if (convertView == null){
	        			row = mInflater.inflate(R.layout.stations_list_item,null);
	        		}else{
	        			row = convertView;
	        		}
	        			StationOverlay tmp = (StationOverlay) getItem(position);
		        		TextView stId = (TextView) row.findViewById(R.id.station_list_item_id);
		        		stId.setText(tmp.getName());
		        		TextView stOc = (TextView) row.findViewById(R.id.station_list_item_ocupation);
		        		stOc.setText(tmp.getOcupation());
		        		TextView stDst = (TextView) row.findViewById(R.id.station_list_item_distance);
		        		stDst.setText(tmp.getDistance());
		        		TextView stWk = (TextView) row.findViewById(R.id.station_list_item_walking_time);
		        		stWk.setText(tmp.getWalking());
		        		
		    			int bg;
		    			switch(tmp.getState()){
		    				case StationOverlay.GREEN_STATE:
		    					bg = green;
		    					break;
		    				case StationOverlay.RED_STATE:
		    					bg = red;
		    					break;
		    				case StationOverlay.YELLOW_STATE:
		    					bg = yellow;
		    					break;
		    				default:
		    					bg = R.drawable.fancy_gradient;
		    			}
		    			LinearLayout sq = (LinearLayout) row.findViewById(R.id.station_list_item_square);
		    			sq.setBackgroundResource(bg);
		    			row.setId(tmp.getId());
		        		return row;
	        	}
	        };
			lv.setAdapter(adapter);
			
			lv.setOnItemClickListener(new OnItemClickListener(){

				@Override
				public void onItemClick(AdapterView<?> arg0, View v,
						int position, long id) {
					
					int pos = v.getId();
					if (pos!=-1){
						StationOverlay selected = stations.findById(pos);
						if (selected!=null){
							stations.setCurrent(selected.getPosition());
							infoLayer.populateFields(selected);
							mapView.getController().animateTo(selected.getCenter());
							mapView.getController().setZoom(16);
							int height = arg0.getHeight();
							DisplayMetrics dm = new DisplayMetrics();
							getWindowManager().getDefaultDisplay().getMetrics(dm);
							int w_height = dm.heightPixels;
							if (height > w_height/2){
								sd.animateClose();
							}
						}
					}
				}});
			lv.setBackgroundColor(Color.BLACK);
			lv.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
			fl.setBackgroundColor(Color.BLACK);
			fl.removeAllViews();
			fl.addView(lv);
			
			DisplayMetrics dm = new DisplayMetrics();
			getWindowManager().getDefaultDisplay().getMetrics(dm);
			int height = dm.heightPixels;
			int calc = (lv.getCount()*50)+40;
			if (calc == 0 || calc > height-90)
				calc = height - 90;
			sd.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, calc));
			Log.i("openBicing",Integer.toString(fl.getHeight()));
		} catch (Exception e) {
			Log.i("openBicing","SHIT THIS SUCKS MEN ARGH FUCK IT!");
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
