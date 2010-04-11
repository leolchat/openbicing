package openbicing.app;

import android.content.Context;
import android.graphics.Canvas;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.maps.GeoPoint;

public class InfoLayer extends LinearLayout {
	
	private StationOverlay station;
	
	private ImageButton nextButton;
	private ImageButton prevButton;
	private TextView station_id;
	private TextView ocupation;
	private TextView distance;
	private TextView walking_time;
	private Handler handler;
	
	public static final int NEXT_STATION = 0;
	public static final int PREV_STATION = 1;
	
	public InfoLayer(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	public InfoLayer(Context context) {
		super(context);
	}
	
	public void setElements(
			ImageButton nextButton, 
			ImageButton prevButton,
			TextView station_id,
			TextView ocupation,
			TextView distance,
			TextView walking_time,
			Handler hdl){
		this.nextButton = nextButton;
		this.prevButton = prevButton;
		this.station_id = station_id;
		this.ocupation = ocupation;
		this.distance = distance;
		this.walking_time = walking_time;
		this.handler = hdl;
		
		this.nextButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				handler.sendEmptyMessage(NEXT_STATION);
			}
		});
		
		this.prevButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				handler.sendEmptyMessage(PREV_STATION);
			}
		});
		
	}
	
	
	public void setStation(StationOverlay station){
		this.station = station;
	}
	public void populateFields(StationOverlay station){
		this.setStation(station);
		this.populateFields();
	}
	
	public void populateFields(){
		if (this.station!=null){
			this.station_id.setText(this.station.getId());
			this.ocupation.setText(this.station.getBikes()+" bikes / "+station.getFree()+" free");
			int meters, km;
			double rawMeters;
			rawMeters = this.station.getMetersDistance();
			km = (int) rawMeters/1000;
			meters = (int) rawMeters - (1000*km);
			Log.i("openBicing",Integer.toString(km)+" "+Integer.toString(meters));
			String distanceText = "";
			if (km>0){
				distanceText = Integer.toString(km)+" km ";
			}
			distanceText = distanceText + Integer.toString(meters)+" m";
			this.distance.setText(distanceText);
			double rawMinutes = (this.station.getMetersDistance()/5000)*60;
			
			int hours, minutes;
			hours = (int) rawMinutes / 60;
			minutes = (int) rawMinutes - (60*hours);
			String walkingText = "";
			if (hours>0){
				walkingText = Integer.toString(hours)+" h ";
			}
			walkingText = walkingText + Integer.toString(minutes)+" min";
			this.walking_time.setText(walkingText);
		}
	}
	
	public GeoPoint getCurrentCenter(){
		return this.station.getCenter();
	}
	public StationOverlay getCurrent(){
		return this.station;
	}
	@Override
    protected void dispatchDraw(Canvas canvas) {
		super.dispatchDraw(canvas);
	}
}
