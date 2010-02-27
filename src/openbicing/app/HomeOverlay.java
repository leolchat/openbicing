package openbicing.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Paint.Cap;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationManager;
import android.util.Log;
import android.view.MotionEvent;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.Projection;

public class HomeOverlay extends Overlay {

	private Context context;
	private GeoPoint point;
	
	private float radiusInPixels;
	private int radiusInMeters = 500;
	
	private float centerXInPixels;
	private float centerYInPixels;
	
	private int status = 0;
		
	
	public HomeOverlay(Context context){
		this.context = context;
		this.update();
		
		
	}
	
	public void update(){
		LocationManager locationManager = (LocationManager) this.context.getSystemService(Context.LOCATION_SERVICE);
        Location mylocation = locationManager.getLastKnownLocation("network");
        Double lat = mylocation.getLatitude()*1E6;
        Double lng = mylocation.getLongitude()*1E6;
        this.point = new GeoPoint(lat.intValue(), lng.intValue());
        
        
        
        
	}
	
	public void setRadius(int meters){
		this.radiusInMeters = meters;
	}
	
	private void calculatePixelRadius(MapView mapView){
		
	}
	
	public int getRadius(int meters){
		return this.radiusInMeters;
	}
	
	public GeoPoint getPoint(){
		return this.point;
	}
	
	@Override
	public boolean draw(Canvas canvas, MapView mapView, boolean shadow,
			long when) {
		// TODO Auto-generated method stub
		Projection astral = mapView.getProjection();
		Point screenPixels = astral.toPixels(this.point, null);
		this.radiusInPixels = (float) astral.metersToEquatorPixels(this.radiusInMeters);
		this.centerXInPixels = screenPixels.x;
		this.centerYInPixels = screenPixels.y;
		
		Paint paint = new Paint();
		
		paint.setARGB(100,147,186,228);
		paint.setStrokeWidth(2);
		paint.setAntiAlias(true);
		paint.setStyle(Paint.Style.STROKE);
		canvas.drawCircle(screenPixels.x, screenPixels.y, this.radiusInPixels, paint);
		canvas.drawLine(this.centerXInPixels-10, this.centerYInPixels, this.centerXInPixels+10, this.centerYInPixels, paint);
		canvas.drawLine(this.centerXInPixels, this.centerYInPixels-10, this.centerXInPixels, this.centerYInPixels+10, paint);
		
		paint.setStyle(Paint.Style.FILL);
		paint.setAlpha(20);
		canvas.drawCircle(screenPixels.x, screenPixels.y, this.radiusInPixels, paint);
		
		Paint txtPaint = new Paint();
		txtPaint.setARGB(255,255,255,255);
		txtPaint.setAntiAlias(true);
		txtPaint.setTextSize(this.radiusInPixels/3);
		String text;
		if (this.radiusInMeters > 1000){
			int km = this.radiusInMeters/1000;
			int m = this.radiusInMeters%1000;
			text = Integer.toString(km)+ " km, "+Integer.toString(m)+" m";
		}else{
			text = Integer.toString(this.radiusInMeters)+" m";
		}
		
		float x = (float) (this.centerXInPixels + this.radiusInPixels*Math.cos(Math.PI));
		float y = (float) (this.centerYInPixels + this.radiusInPixels*Math.sin(Math.PI));
		
		
		//lol
		txtPaint.setTextAlign(Paint.Align.CENTER);
		Path tPath = new Path();
		tPath.moveTo(x,y+this.radiusInPixels/3);
		tPath.lineTo(x+this.radiusInPixels*2,y+this.radiusInPixels/3);
		canvas.drawTextOnPath(text, tPath, 0,0, txtPaint);
		canvas.drawPath(tPath, txtPaint);

		return super.draw(canvas, mapView, shadow, when);
	}
	
	public void drawArrow(Canvas canvas, Point sPC, float length, double angle, float arrLen){
		Paint paint = new Paint();
		paint.setARGB(75,210,228,252);
		paint.setStrokeWidth(1);
		paint.setAntiAlias(true);
		paint.setStrokeCap(Cap.ROUND);
		paint.setStyle(Paint.Style.STROKE);
		float x = (float) (sPC.x + length*Math.cos(angle));
		float y = (float) (sPC.y + length*Math.sin(angle));
		canvas.drawLine(sPC.x, sPC.y, x, y, paint);
		
		double arrw_angle = Math.PI/12;
		float arrw_x = (float) (arrLen*Math.cos(arrw_angle)); 
		float arrw_y = (float) (arrLen*Math.sin(arrw_angle));
		
		canvas.drawLine(x,y, x - arrw_x, y - arrw_y, paint);	
		canvas.drawLine(x,y, x - arrw_x, y + arrw_y, paint);
		
		canvas.drawLine(sPC.x,sPC.y, sPC.x + arrw_x, sPC.y - arrw_y, paint);
		canvas.drawLine(sPC.x,sPC.y, sPC.x + arrw_x, sPC.y + arrw_y, paint);
		
	}


	@Override
	public boolean onTap(GeoPoint p, MapView mapView) {
		// TODO Auto-generated method stub
			
		return super.onTap(p, mapView);
	}

	private boolean isOnCircle(float x, float y, float centerX, float centerY, float radius){
		double square_dist = Math.pow(centerX - x, 2) + Math.pow(centerY - y,2);
		return square_dist <= Math.pow(radius,2);
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent e, MapView mapView) {
		// TODO Auto-generated method stub
		float x = e.getX();
		float y = e.getY();
		
		int action = e.getAction();
		
		boolean onCircle = isOnCircle(x,y,this.centerXInPixels,this.centerYInPixels,50);
		
		
		if (action == MotionEvent.ACTION_DOWN){
			if (onCircle){
				if (this.status == 1)
					this.status = 2;
				else
					this.status = 1;
			}else{
				if (this.status==1){
					this.status = 2;
				}else
					this.status = 0;
			}
			
			if (this.status == 2){
				// resize
				double dist = Math.sqrt(Math.pow(Math.abs(this.centerXInPixels - x), 2) +
						  Math.pow(Math.abs(this.centerYInPixels - y), 2));
				
				this.radiusInMeters = (int) ((int) (dist * this.radiusInMeters)/this.radiusInPixels);
			}
		}		
		return super.onTouchEvent(e, mapView);
	}
}
