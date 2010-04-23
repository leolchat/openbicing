package openbicing.app;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Shader.TileMode;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.maps.GeoPoint;

public class InfoLayer extends LinearLayout {
	
	private static final int SWIPE_MIN_DISTANCE = 120;
    private static final int SWIPE_MAX_OFF_PATH = 100;
    private static final int SWIPE_THRESHOLD_VELOCITY = 200;
    
    private static final double ERROR_COEFICIENT = 0.35;
    private GestureDetector gestureDetector;
    View.OnTouchListener gestureListener;

	
	private StationOverlay station;
	
	private TextView station_id;
	private TextView ocupation;
	private TextView distance;
	private TextView walking_time;
	private Handler handler;
	
	private Context ctx;
	
	private BitmapDrawable green, red, yellow;
	
	public static final int NEXT_STATION = 200;
	public static final int PREV_STATION = 201;
	
	public InfoLayer(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.ctx = context;
		this.init();
	}
	
	public InfoLayer(Context context) {
		super(context);
		this.ctx = context;
		this.init();
	}
	
	private void init(){
		gestureDetector = new GestureDetector(new MyGestureDetector());
        gestureListener = new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                if (gestureDetector.onTouchEvent(event)) {
                    return true;
                }
                return false;
            }
        };
        this.setOnTouchListener(gestureListener);
	}
	
	public void setElements(
			TextView station_id,
			TextView ocupation,
			TextView distance,
			TextView walking_time,
			Handler hdl){
		this.station_id = station_id;
		this.ocupation = ocupation;
		this.distance = distance;
		this.walking_time = walking_time;
		this.handler = hdl;
		
		green = new BitmapDrawable(BitmapFactory.decodeResource(getResources(), R.drawable.green_gradient_image));
		yellow = new BitmapDrawable(BitmapFactory.decodeResource(getResources(), R.drawable.yellow_gradient_image));
		red = new BitmapDrawable(BitmapFactory.decodeResource(getResources(), R.drawable.red_gradient_image));
		
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
			rawMeters = this.station.getMetersDistance()+this.station.getMetersDistance()*ERROR_COEFICIENT;
			km = (int) rawMeters/1000;
			meters = (int) rawMeters - (1000*km);
			Log.i("openBicing",Integer.toString(km)+" "+Integer.toString(meters));
			String distanceText = "";
			if (km>0){
				distanceText = Integer.toString(km)+" km ";
			}
			distanceText = distanceText + Integer.toString(meters)+" m";
			this.distance.setText(distanceText);
			double rawMinutes = (rawMeters/5000)*60;
			
			int hours, minutes;
			hours = (int) rawMinutes / 60;
			minutes = (int) rawMinutes - (60*hours);
			String walkingText = "";
			if (hours>0){
				walkingText = Integer.toString(hours)+" h ";
			}
			walkingText = walkingText + Integer.toString(minutes)+" min";
			this.walking_time.setText(walkingText);
			BitmapDrawable bg;
			switch(station.getState()){
				case StationOverlay.GREEN_STATE:
					bg = this.green;
					break;
				case StationOverlay.RED_STATE:
					bg = this.red;
					break;
				case StationOverlay.YELLOW_STATE:
					bg = this.yellow;
					break;
				default:
					bg = new BitmapDrawable(BitmapFactory.decodeResource(getResources(), R.drawable.fancy_gradient));
			}
			
			bg.setBounds(this.getBackground().getBounds());
			bg.setAlpha(200);
			bg.setTileModeX(TileMode.REPEAT);
			this.setBackgroundDrawable(bg);
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

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		// TODO Auto-generated method stub
		return super.onTouchEvent(event);
	}
	
	class MyGestureDetector extends SimpleOnGestureListener {
	    @Override
	    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
	        try {
	            if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH){
	            	Log.i("openBicing","down?");
	            	return false;
	            }
	            // right to left swipe
	            if(e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
	            	handler.sendEmptyMessage(NEXT_STATION);
	            }  else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
	                handler.sendEmptyMessage(PREV_STATION);
	            }
	        } catch (Exception e) {
	            // nothing
	        }
	        return false;
	    }
	}
}



