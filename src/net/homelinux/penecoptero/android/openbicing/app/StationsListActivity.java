/*
 * Copyright (C) 2010 Lluís Esquerda
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.homelinux.penecoptero.android.openbicing.app;

import java.util.List;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.maps.GeoPoint;

public class StationsListActivity extends ListActivity {
	private StationsDBAdapter mDbHelper;
	private List<StationOverlay> stations;
	private int green, red, yellow;
	private int radius;
	private boolean view_all;

	public static final String KEY_POSITION = "sPosition";

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.stations_list);
		Handler handler = new Handler();
		mDbHelper = new StationsDBAdapter(this, handler);
		Bundle extras = getIntent().getExtras();
		int lat = savedInstanceState != null ? savedInstanceState
				.getInt(StationsDBAdapter.CENTER_LAT_KEY) : -1;
		if (lat == -1) {
			lat = extras != null ? extras
					.getInt(StationsDBAdapter.CENTER_LAT_KEY) : -1;
		}

		int lng = savedInstanceState != null ? savedInstanceState
				.getInt(StationsDBAdapter.CENTER_LNG_KEY) : -1;
		if (lng == -1) {
			lng = extras != null ? extras
					.getInt(StationsDBAdapter.CENTER_LNG_KEY) : -1;
		}

		radius = savedInstanceState != null ? savedInstanceState
				.getInt(StationsDBAdapter.RADIUS_KEY) : -1;
		if (radius == -1) {
			radius = extras != null ? extras
					.getInt(StationsDBAdapter.RADIUS_KEY) : -1;
		}

		view_all = savedInstanceState != null ? savedInstanceState
				.getBoolean(StationsDBAdapter.VIEW_ALL_KEY) : false;
		if (!view_all) {
			view_all = extras != null ? extras
					.getBoolean(StationsDBAdapter.VIEW_ALL_KEY) : false;
		}

		if (radius == -1) {
			radius = extras != null ? extras
					.getInt(StationsDBAdapter.RADIUS_KEY) : -1;
		}

		if (lat != -1 && lng != -1) {
			GeoPoint point = new GeoPoint(lat, lng);
			mDbHelper.setCenter(point);
		}

		registerForContextMenu(getListView());
		fillData();

		green = R.drawable.green_gradient;
		yellow = R.drawable.yellow_gradient;
		red = R.drawable.red_gradient;
	}

	private void fillData() {
		try {
			mDbHelper.loadStations();
			if (view_all) {
				stations = mDbHelper.getMemory();
			} else {
				stations = mDbHelper.getMemory(this.radius);
			}
			setListAdapter(new ArrayAdapter(this, R.layout.stations_list_item,
					stations) {
				LayoutInflater mInflater = getLayoutInflater();

				@Override
				public View getView(int position, View convertView,
						ViewGroup parent) {
					View row;

					if (convertView == null) {
						row = mInflater.inflate(R.layout.stations_list_item,
								null);
					} else {
						row = convertView;
					}
					StationOverlay tmp = (StationOverlay) getItem(position);
					TextView stId = (TextView) row
							.findViewById(R.id.station_list_item_id);
					stId.setText(tmp.getName());
					TextView stOc = (TextView) row
							.findViewById(R.id.station_list_item_ocupation);
					stOc.setText(tmp.getOcupation());
					TextView stDst = (TextView) row
							.findViewById(R.id.station_list_item_distance);
					stDst.setText(tmp.getDistance());
					TextView stWk = (TextView) row
							.findViewById(R.id.station_list_item_walking_time);
					stWk.setText(tmp.getWalking());

					int bg;
					switch (tmp.getState()) {
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
					LinearLayout sq = (LinearLayout) row
							.findViewById(R.id.station_list_item_square);
					sq.setBackgroundResource(bg);
					row.setId(tmp.getId());
					return row;
				}
			});
		} catch (Exception e) {

		}
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		Intent data = new Intent();
		data.putExtra(StationsListActivity.KEY_POSITION, v.getId());
		setResult(RESULT_OK, data);
		finish();
	}
}
