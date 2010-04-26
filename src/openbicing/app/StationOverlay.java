package openbicing.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Message;
import android.view.MotionEvent;
import android.widget.TextView;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.Projection;

public class StationOverlay extends Overlay {
	private int bikes;
	private int free;
	private String timestamp;
	private int status;
	private String name;
	private Context context;
	private int id;
	
	private String distanceText = "";
	private String walkingText = "";
	private String ocupationText = "";

	private Handler handler;

	private boolean selected = false;

	private GeoPoint point;

	public static final int BLACK_STATE = 0;
	public  static final int RED_STATE = 1;
	public  static final int YELLOW_STATE = 2;
	public  static final int GREEN_STATE = 3;

	public static final int TOUCHED = 10;

	private int radiusInPixels;
	private int radiusInMeters;

	private static final int RED_STATE_MAX = 0;
	private static final int YELLOW_STATE_MAX = 8;

	private static final int RED_STATE_RADIUS = 80;
	private static final int YELLOW_STATE_RADIUS = 80;
	private static final int GREEN_STATE_RADIUS = 80;
	private static final int SELECTED_STATE_RADIUS = 120;

	private Paint currentPaint;
	private Paint currentBorderPaint;
	private Paint selectedPaint;

	
	private double metersDistance;

	private int position = -1;

	public StationOverlay(GeoPoint point, Context context, int id, int bikes, int free,
			String timestamp, String name) {
		this.id = id;
		this.point = point;
		this.bikes = bikes;
		this.free = free;
		this.name = name;
		this.timestamp = timestamp;
		this.context = context;

		this.currentPaint = new Paint();
		this.currentBorderPaint = new Paint();
		this.selectedPaint = new Paint();

		this.currentPaint.setAntiAlias(true);

		this.currentBorderPaint.setStyle(Paint.Style.STROKE);
		this.currentBorderPaint.setStrokeWidth(4);

		this.selectedPaint = new Paint();
		this.selectedPaint.setARGB(75, 0, 0, 0);
		this.selectedPaint.setAntiAlias(true);
		this.selectedPaint.setStrokeWidth(4);
		this.selectedPaint.setStyle(Paint.Style.STROKE);

		this.updateStatus();
	}

	public void setPosition(int position) {
		this.position = position;
	}
	
	public int getPosition(){
		return this.position;
	}

	public void setHandler(Handler handler) {
		this.handler = handler;
	}
	
	public int getId(){
		return this.id;
	}

	public String getName() {
		return this.name;
	}
	
	public double getMetersDistance(){
		return this.metersDistance;
	}
	
	public void setMetersDistance(double distance){
		this.metersDistance = distance;
	}
	
	public int getState(){
		return this.status;
	}
	
	public int getBikes() {
		return this.bikes;
	}

	public int getFree() {
		return this.free;
	}

	public GeoPoint getCenter() {
		return this.point;
	}

	public void updateStatus() {
		if (this.bikes > YELLOW_STATE_MAX) {
			this.status = GREEN_STATE;
			this.radiusInMeters = GREEN_STATE_RADIUS;
			this.currentPaint.setARGB(75, 168, 255, 87);
			this.currentBorderPaint.setARGB(100, 168, 255, 87);
		} else if (this.bikes > RED_STATE_MAX) {
			this.status = YELLOW_STATE;
			this.radiusInMeters = YELLOW_STATE_RADIUS;
			this.currentPaint.setARGB(75, 255, 210, 72);
			this.currentBorderPaint.setARGB(100, 255, 210, 72);

		} else {
			this.status = RED_STATE;
			this.radiusInMeters = RED_STATE_RADIUS;
			this.currentPaint.setARGB(75, 240, 35, 17);
			this.currentBorderPaint.setARGB(100, 240, 35, 17);
		}
	}

	public void setSelected(boolean selected) {
		this.selected = selected;
		if (this.selected)
			this.radiusInMeters = SELECTED_STATE_RADIUS;
		else
			updateStatus();
	}

	public void update() {
		// TODO Update aviability.. :D
		this.updateStatus();
	}

	private void calculatePixelRadius(MapView mapView) {
		this.radiusInPixels = (int) mapView.getProjection()
				.metersToEquatorPixels(this.radiusInMeters);
	}

	@Override
	public boolean draw(Canvas canvas, MapView mapView, boolean shadow,
			long when) {
		// TODO Auto-generated method stub
		return super.draw(canvas, mapView, shadow, when);
	}

	@Override
	public void draw(Canvas canvas, MapView mapView, boolean shadow) {

		calculatePixelRadius(mapView);

		Projection astral = mapView.getProjection();
		Point screenPixels = astral.toPixels(this.point, null);

		RectF oval = new RectF(screenPixels.x - this.radiusInPixels,
				screenPixels.y - this.radiusInPixels, screenPixels.x
						+ this.radiusInPixels, screenPixels.y
						+ this.radiusInPixels);

		canvas.drawOval(oval, this.currentPaint);

		if (this.selected) {
			canvas.drawCircle(screenPixels.x, screenPixels.y, this.radiusInPixels,
					this.selectedPaint);
		}else{
			canvas.drawCircle(screenPixels.x, screenPixels.y, this.radiusInPixels,
					this.currentBorderPaint);
		}
	}

	@Override
	public boolean onTap(GeoPoint p, MapView mapView) {
		// TODO Auto-generated method stub

		if ((p.getLatitudeE6() <= this.point.getLatitudeE6() + 800 && p
				.getLatitudeE6() >= this.point.getLatitudeE6() - 800)
				&& (p.getLongitudeE6() <= this.point.getLongitudeE6() + 800 && p
						.getLongitudeE6() >= this.point.getLongitudeE6() - 800)) {

			if (this.handler != null) {
				Message msg = new Message();
				msg.what = TOUCHED;
				msg.arg1 = this.position;
				this.handler.sendMessage(msg);
			}
		}

		return super.onTap(p, mapView);
	}

	public boolean getSelected() {
		return this.selected;
	}
	
	public void populateStrings(){
		ocupationText = Integer.toString(this.bikes)+" bikes / "+Integer.toString(this.free)+" free";
		
		int meters, km;
		double rawMeters;
		rawMeters = this.metersDistance+this.metersDistance*InfoLayer.ERROR_COEFICIENT;
		km = (int) rawMeters/1000;
		meters = (int) rawMeters - (1000*km);
		if (km>0){
			distanceText = Integer.toString(km)+" km ";
		}
		distanceText = distanceText + Integer.toString(meters)+" m";
		
		double rawMinutes = (rawMeters/5000)*60;
		
		int hours, minutes;
		hours = (int) rawMinutes / 60;
		minutes = (int) rawMinutes - (60*hours);
		if (hours>0){
			walkingText = Integer.toString(hours)+" h ";
		}
		walkingText = walkingText + Integer.toString(minutes)+" min";
	}
	
	public String getOcupation(){
		return this.ocupationText;
	}
	public String getWalking(){
		return this.walkingText;
	}
	public String getDistance(){
		return this.distanceText;
	}

	@Override
	public boolean onTouchEvent(MotionEvent e, MapView mapView) {
		// TODO Auto-generated method stub
		return super.onTouchEvent(e, mapView);
	}

}
