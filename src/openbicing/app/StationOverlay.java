package openbicing.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.Projection;

public class StationOverlay extends Overlay {
	private int bikes;
	private int free;
	private String timestamp;
	private int status;
	private int id;
	private Context context;
	private Drawable icon;
	private GeoPoint point;
	
	private static final int RED_STATE = 1;
	private static final int YELLOW_STATE = 2;
	private static final int GREEN_STATE = 3;
	
	private static final int RED_STATE_MAX = 5;
	private static final int YELLOW_STATE_MAX = 10;
	
	private static final int RED_MARKER = R.drawable.bullet_green;
	private static final int YELLOW_MARKER = R.drawable.bullet_green;
	private static final int GREEN_MARKER = R.drawable.bullet_green;
	
	public StationOverlay(GeoPoint point,Context context, int bikes, int free, String timestamp, int id) {
		this.point = point;
		this.bikes = bikes;
		this.free = free;
		this.id = id;
		this.timestamp = timestamp;
		this.context = context;
		this.updateStatus();
	}
	
	public void updateStatus(){
		
		if (this.bikes>YELLOW_STATE_MAX){
			this.status = GREEN_STATE;
			this.icon = this.context.getResources().getDrawable(GREEN_MARKER);
		}
		else if (this.bikes>RED_STATE_MAX){
			this.status = YELLOW_STATE;
			this.icon = this.context.getResources().getDrawable(YELLOW_MARKER);
		}
		else{
			this.status = RED_STATE;
			this.icon = this.context.getResources().getDrawable(RED_MARKER);
		}
	}
	
	public void update(){
		// TODO Update aviability.. :D
		this.updateStatus();
	}


	@Override
	public boolean draw(Canvas canvas, MapView mapView, boolean shadow,
			long when) {
		// TODO Auto-generated method stub
		return super.draw(canvas, mapView, shadow, when);
	}

	@Override
	public void draw(Canvas canvas, MapView mapView, boolean shadow) {
		Projection astral = mapView.getProjection();
		Point screenPixels = astral.toPixels(this.point, null);
		RectF oval = new RectF(screenPixels.x-20,screenPixels.y-20,screenPixels.x+20,screenPixels.y+20);
		Paint paint = new Paint();
		if (this.status == RED_STATE)
			paint.setARGB(50,255,0,0);
		else if (this.status == YELLOW_STATE)
			paint.setARGB(20,255,255,0);
		else
			paint.setARGB(20,0,255,0);
		
		Paint paint2 = new Paint();
		paint2.set(paint);
		paint2.setStrokeWidth(5);
		paint2.setAlpha(255);
		paint2.setAntiAlias(true);
		paint2.setStyle(Paint.Style.STROKE);
		canvas.drawCircle(screenPixels.x, screenPixels.y, 20, paint2);
		canvas.drawOval(oval,paint);
	}

	@Override
	public boolean onTap(GeoPoint p, MapView mapView) {
		// TODO Auto-generated method stub
		return super.onTap(p, mapView);
	}

	@Override
	public boolean onTouchEvent(MotionEvent e, MapView mapView) {
		// TODO Auto-generated method stub
		return super.onTouchEvent(e, mapView);
	}

}
