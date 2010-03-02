/* Copyright (C) 2008 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package openbicing.app;

import java.util.LinkedList;
import java.util.Queue;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;


/**
 * Simple notes database access helper class. Defines the basic CRUD operations
 * for the notepad example, and gives the ability to list all notes as well as
 * retrieve or modify a specific note.
 * 
 * This has been improved from the first version of this tutorial through the
 * addition of better error handling and also using returning a Cursor instead
 * of using a collection of inner classes (which is less scalable and not
 * recommended).
 */
public class OpenBicingDbAdapter implements Runnable{

    
	public static final String KEY_ROWID = "_id";
    public static final String KEY_NAME = "name";
    public static final String KEY_COORDINATES = "coordinates";
    public static final String KEY_X = "x";
    public static final String KEY_Y = "y";
    public static final String KEY_BIKE = "bike";
    public static final String KEY_FREE = "free";
    public static final String KEY_TIMESTAMP = "timestamp";
    
    public static final Integer UPDATE_MAP_JSON = 0;
    public static final Integer UPDATE_DATABASE = 1;
    
    public static final String STATIONS_URL = "http://openbicing.appspot.com/stations.json";
    
    private ProgressDialog dialog;
    private static final String TAG = "OpenBicingDbAdapter";
    private DatabaseHelper mDbHelper;
    private SQLiteDatabase mDb;
    
    private StationOverlayList stationsMemoryList;
    private JSONArray lastJSONInfo = null;
    private RESTHelper mRESTHelper;
    
    private MapView mapView;
    
    private Queue<Integer> toDo;
    
    
    /**
     * Database creation sql statement
     */
    private static final String DATABASE_CREATE =
    		"create table stations (_id integer primary key autoincrement, "
    			+ "name text not null, coordinates text not null, x text not null, "
    			+ "y text not null, bike integer not null, free integer not null, timestamp text not null);";
    
    private static final String DATABASE_NAME = "openbicing_data";
    private static final String STATIONS_TABLE = "stations";
    private static final int DATABASE_VERSION = 2;

    private final Context mCtx;

    private static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db){
            db.execSQL(DATABASE_CREATE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                    + newVersion + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS stations");
            onCreate(db);
        }
    }

    /**
     * Constructor - takes the context to allow the database to be
     * opened/created
     * 
     * @param ctx the Context within which to work
     */
    public OpenBicingDbAdapter(Context ctx, StationOverlayList stationsMemoryList, MapView mapView) {
    	this.stationsMemoryList = stationsMemoryList;
        this.mCtx = ctx;
        toDo = new LinkedList();
        this.mapView = mapView;
    }

    /**
     * Open the notes database. If it cannot be opened, try to create a new
     * instance of the database. If it cannot be created, throw an exception to
     * signal the failure
     * 
     * @return this (self reference, allowing this to be chained in an
     *         initialization call)
     * @throws SQLException if the database could be neither opened or created
     */
    public OpenBicingDbAdapter open(){
        mDbHelper = new DatabaseHelper(mCtx);
        this.mRESTHelper = new RESTHelper(this.mCtx,false,null,null);
        mDb = mDbHelper.getWritableDatabase();
        return this;
    }
    
    public void close() {
        mDbHelper.close();
    }
    
    public void populateFromJSON(JSONArray stations) throws Exception{
    	this.lastJSONInfo = stations;
    	JSONObject station = null;
    	stationsMemoryList.clear();
    	for (int i = 0; i<stations.length(); i++){
    		station = stations.getJSONObject(i);
    		int lat = Integer.parseInt(station.getString("y"));
    		int lng = Integer.parseInt(station.getString("x"));
    		  		
    		int bikes = station.getInt("bikes");
    		int free = station.getInt("free");
    		String timestamp = station.getString("timestamp");
    		String id = station.getString("name");
    		GeoPoint point = new GeoPoint(lat, lng);

    		StationOverlay memoryStation = new StationOverlay(point,mCtx,bikes,free,timestamp,id);
    		
    	    stationsMemoryList.addStationOverlay(memoryStation);
    	}
    	mapView.postInvalidate();
    	
    	if (dialog.isShowing())
			dialog.dismiss();
    }
    
    public void populateFromJSON() throws Exception{
    	String listStations = mRESTHelper.restGET(STATIONS_URL);
    	JSONArray stations = new JSONArray(listStations);
    	this.lastJSONInfo = stations;
    	JSONObject station = null;
    	stationsMemoryList.clear();
    	for (int i = 0; i<stations.length(); i++){
    		station = stations.getJSONObject(i);
    		int lat = Integer.parseInt(station.getString("y"));
    		int lng = Integer.parseInt(station.getString("x"));
    		  		
    		int bikes = station.getInt("bikes");
    		int free = station.getInt("free");
    		String timestamp = station.getString("timestamp");
    		String id = station.getString("name");
    		GeoPoint point = new GeoPoint(lat, lng);

    		StationOverlay memoryStation = new StationOverlay(point,mCtx,bikes,free,timestamp,id);
    		
    	    stationsMemoryList.addStationOverlay(memoryStation);
    	}
    	mapView.postInvalidate();
    	toDo.add(UPDATE_DATABASE);
    	if (dialog.isShowing())
			dialog.dismiss();
    }
    
    
    public void syncStations() throws Exception{
		    	Log.i("openBicing","Launching work thread..");
		    	dialog = new ProgressDialog(mCtx);
		    	dialog.setTitle("");
		    	dialog.setMessage("Loading. Please wait...");
		    	dialog.show();
		    	toDo.add(UPDATE_MAP_JSON);
		    	Thread happyThread = new Thread(this);
		        happyThread.start();
    }

    
    @Override
	public void run() {
		// TODO Auto-generated method stub
		while (!toDo.isEmpty()){
			Integer shit = (Integer) toDo.poll();
			if(shit == UPDATE_MAP_JSON){
				Log.i("openBicing","Updating JSON");
				try{
					populateFromJSON();
				}catch (Exception e){
					populateFromDatabase();
				};
			}else if (shit == UPDATE_DATABASE){
				Log.i("openBicing","Updating Database");
				try{
					updateDBStations();
				}catch (Exception e){
					Log.i("openBicing","I dont know shit");
				};
			}
		}
		toDo.clear();
	}
	
	
	private void updateDBStations() throws Exception{
			mDb.execSQL("DELETE FROM "+STATIONS_TABLE);
			if (lastJSONInfo!=null){
				/*JSONObject station = null;
				for (int i = 0; i<lastJSONInfo.length(); i++){
		    		station = lastJSONInfo.getJSONObject(i);
		    		createStation(station.get("name").toString(),station.get("coordinates").toString(),station.get("x").toString(),station.get("y").toString(),Integer.parseInt(station.get("bikes").toString()),Integer.parseInt(station.get("free").toString()),station.get("timestamp").toString());
		    	}*/
				SharedPreferences settings = this.mCtx.getSharedPreferences("openbicing", 0);
			    SharedPreferences.Editor editor = settings.edit();
			    editor.putString("stations", lastJSONInfo.toString());

			      // Don't forget to commit your edits!!!
			    editor.commit();
				
			    Log.i("openBicing","I'm happily finished");
			}else{
				Log.i("openBicing","See?");
			}
	}
	
	public Cursor fetchAllStations() {
        return mDb.query(STATIONS_TABLE, new String[] {KEY_ROWID, KEY_NAME, KEY_X, KEY_Y, KEY_BIKE, KEY_FREE, KEY_TIMESTAMP}, null, null, null, null, KEY_NAME);
    }
    
    public void populateFromDatabase(){
    	Log.i("openBicing","Shit there's no internet, trying to get database");
    	stationsMemoryList.clear();
    	/*Cursor stationsCursor = fetchAllStations();
    	while (stationsCursor.moveToNext()){
    		// This should be integers in the database.. :/
    		
    		int lat = Integer.parseInt(stationsCursor.getString(stationsCursor.getColumnIndexOrThrow(OpenBicingDbAdapter.KEY_Y)));
    		int lng = Integer.parseInt(stationsCursor.getString(stationsCursor.getColumnIndexOrThrow(OpenBicingDbAdapter.KEY_X)));
    		
    		int bikes = stationsCursor.getInt(stationsCursor.getColumnIndexOrThrow(OpenBicingDbAdapter.KEY_BIKE));
    	    int free = stationsCursor.getInt(stationsCursor.getColumnIndexOrThrow(OpenBicingDbAdapter.KEY_FREE));
    	    String timestamp = stationsCursor.getString(stationsCursor.getColumnIndexOrThrow(OpenBicingDbAdapter.KEY_TIMESTAMP));
    	    String id = stationsCursor.getString(stationsCursor.getColumnIndexOrThrow(OpenBicingDbAdapter.KEY_NAME));
    	    GeoPoint point = new GeoPoint(lat, lng);
    	    
    	    
    	    StationOverlay station = new StationOverlay(point,mCtx,bikes,free,timestamp,id);
    	    stationsMemoryList.addStationOverlay(station);
    	}
    	mapView.postInvalidate();
    	if (dialog.isShowing())
			dialog.dismiss();*/
    	SharedPreferences settings = this.mCtx.getSharedPreferences("openbicing", 0);
    	String strStations = settings.getString("stations", "[]");
    	try {
			JSONArray stations = new JSONArray(strStations);
			try {
				this.populateFromJSON(stations);
			} catch (Exception e) {
				Log.i("openBicing","DIE FUCKA DIE 2");
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (JSONException e) {
			Log.i("openBicing","DIE FUCKA DIE");
			// TODO Auto-generated catch block
			e.printStackTrace();
			
		}
    	
    }
    
    public long createStation(String name, String coordinates, String x, String y, Integer bike, Integer free, String timestamp){
    	ContentValues initialValues = new ContentValues();
    	initialValues.put(KEY_NAME, name);
    	initialValues.put(KEY_COORDINATES, coordinates);
    	initialValues.put(KEY_X, x);
    	initialValues.put(KEY_Y, y);
    	initialValues.put(KEY_BIKE, bike);
    	initialValues.put(KEY_FREE, free);
    	initialValues.put(KEY_TIMESTAMP, timestamp);
    	return mDb.insert(STATIONS_TABLE, null, initialValues);
    }
}