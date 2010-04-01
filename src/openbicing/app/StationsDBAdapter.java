package openbicing.app;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import openbicing.utils.CircleHelper;

import org.json.JSONArray;
import org.json.JSONObject;

import uk.me.jstott.jcoord.LatLng;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;

public class StationsDBAdapter implements Runnable {

	public static final String STATION_PROVIDER_KEY = "sProvider";
	public static final String CENTER_LAT_KEY = "sCenterLat";
	public static final String CENTER_LNG_KEY = "sCenterLng";
	public static final String RADIUS_KEY = "sRadius";
	public static final String BICING_PROVIDER = "http://openbicing.appspot.com/stations.json";
	public static final String PREF_NAME = "openbicing";

	public static final int FETCH = 0;
	public static final int UPDATE_MAP = 1;
	public static final int UPDATE_MAP_LESS = 2;
	public static final int UPDATE_DATABASE = 3;
	public static final int NETWORK_ERROR = 4;
	public static final String TIMESTAMP_FORMAT = "HH:mm:ss dd/MM/yyyy";

	private StationOverlayList stationsDisplayList;

	private HashMap<String, StationOverlay> stationsMemoryMap;

	private RESTHelper mRESTHelper;

	private MapView mapView;

	private Context mCtx;

	private Handler handlerOut;

	private Bundle threadData;

	private Queue<Integer> toDo;

	private String RAWstations;

	private String last_updated;

	public StationsDBAdapter(Context ctx, MapView mapView, Handler handler,
			StationOverlayList stationsDisplayList) {
		this.mCtx = ctx;
		this.mapView = mapView;
		this.handlerOut = handler;
		this.stationsDisplayList = stationsDisplayList;

		this.mRESTHelper = new RESTHelper(this.mCtx, false, null, null);

		this.toDo = new LinkedList();
		this.stationsMemoryMap = new HashMap<String, StationOverlay>();
	}

	public String fetchStations(String provider) throws Exception {
		return mRESTHelper.restGET(provider);
	}

	public String getLastUpdated() {
		return last_updated;
	}

	public void loadStations() throws Exception {
		this.retrieve();
		buildMemory(new JSONArray(this.RAWstations));
	}

	public void buildMemory(JSONArray stations) throws Exception {
		JSONObject station = null;
		int lat, lng, bikes, free;
		String timestamp, id;
		GeoPoint point;
		for (int i = 0; i < stations.length(); i++) {
			station = stations.getJSONObject(i);
			lat = Integer.parseInt(station.getString("y"));
			lng = Integer.parseInt(station.getString("x"));
			bikes = station.getInt("bikes");
			free = station.getInt("free");
			timestamp = station.getString("timestamp");
			id = station.getString("name");
			point = new GeoPoint(lat, lng);
			StationOverlay memoryStation = new StationOverlay(point, mCtx,
					bikes, free, timestamp, id);
			stationsMemoryMap.put(id, memoryStation);
		}
	}

	public void store() {
		SharedPreferences settings = this.mCtx.getSharedPreferences(PREF_NAME,
				0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString("stations", this.RAWstations);
		editor.putString("last_updated", this.last_updated);
		editor.commit();
	}

	public void retrieve() throws Exception {
		SharedPreferences settings = this.mCtx.getSharedPreferences(PREF_NAME,
				0);
		RAWstations = settings.getString("stations", "[]");
		last_updated = settings.getString("last_updated", null);
	}

	public void sync(boolean all, Bundle data) throws Exception {
		this.threadData = data;
		toDo.add(FETCH);
		if (all)
			toDo.add(UPDATE_MAP);
		else
			toDo.add(UPDATE_MAP_LESS);
		toDo.add(UPDATE_DATABASE);
		Thread awesomeThread = new Thread(this);
		awesomeThread.start();
	}

	public void populateStations() throws Exception {
		stationsDisplayList.clear();
		Collection<StationOverlay> everything = stationsMemoryMap.values();
		Iterator<StationOverlay> i = everything.iterator();
		while (i.hasNext()) {
			stationsDisplayList.addStationOverlay(i.next());
		}
		stationsDisplayList.updatePositions();
		mapView.postInvalidate();
	}

	public void populateStations(GeoPoint center, int radius) throws Exception {
		stationsDisplayList.clear();
		Collection<StationOverlay> everything = stationsMemoryMap.values();
		Iterator<StationOverlay> i = everything.iterator();
		StationOverlay tmp;
		LatLng cnt = new LatLng(center.getLatitudeE6()/1E6,center.getLongitudeE6()/1E6);
		LatLng tmpLL;
		while (i.hasNext()) {
			tmp = i.next();
			tmpLL = new LatLng(tmp.getCenter().getLatitudeE6()/1E6, tmp.getCenter().getLongitudeE6()/1E6);
			if (CircleHelper.isOnCircle(tmp.getCenter(), center, radius)) {
				tmp.setGradialSeparation(CircleHelper.gradialDistance(center, tmp.getCenter()));
				tmp.setKmDistance(cnt.distance(tmpLL));
				stationsDisplayList.addStationOverlay(tmp);
			}
		}

		Collections.sort((List<Overlay>) stationsDisplayList.getList(),
				new Comparator() {
					public int compare(Object o1, Object o2) {
						if (o1 instanceof StationOverlay
								&& o2 instanceof StationOverlay) {
							StationOverlay stat1 = (StationOverlay) o1;
							StationOverlay stat2 = (StationOverlay) o2;
							if (stat1.getGradialSeparation() > stat2
									.getGradialSeparation())
								return 1;
							else
								return -1;
						} else {
							if (o1 instanceof HomeOverlay) {
								return 1;
							} else if (o2 instanceof HomeOverlay) {
								return -1;
							} else {
								return 0;
							}
						}
					}
				});
		stationsDisplayList.updatePositions();
		mapView.postInvalidate();
	}

	@Override
	public void run() {
		while (!toDo.isEmpty()) {
			Integer action = (Integer) toDo.poll();
			switch (action) {
			case FETCH:
				try {
					RAWstations = fetchStations(threadData
							.getString(STATION_PROVIDER_KEY));
					SimpleDateFormat sdf = new SimpleDateFormat(
							TIMESTAMP_FORMAT);
					Calendar cal = Calendar.getInstance();
					last_updated = sdf.format(cal.getTime());
					buildMemory(new JSONArray(RAWstations));
				} catch (Exception fetchError) {
					handlerOut.sendEmptyMessage(NETWORK_ERROR);
					try {
						retrieve();
						buildMemory(new JSONArray(RAWstations));
					} catch (Exception internalError) {
						// FUCK EVERYTHING!
					}
				}
				handlerOut.sendEmptyMessage(FETCH);
				break;
			case UPDATE_MAP:
				try {
					populateStations();
				} catch (Exception populateError) {

				}
				handlerOut.sendEmptyMessage(UPDATE_MAP);
				break;
			case UPDATE_MAP_LESS:
				try {
					GeoPoint center = new GeoPoint(threadData
							.getInt(CENTER_LAT_KEY), threadData
							.getInt(CENTER_LNG_KEY));
					populateStations(center, threadData.getInt(RADIUS_KEY));
				} catch (Exception populateError) {

				}
				handlerOut.sendEmptyMessage(UPDATE_MAP);
				break;
			case UPDATE_DATABASE:
				store();
				handlerOut.sendEmptyMessage(UPDATE_DATABASE);
				break;
			}
		}
	}
}
