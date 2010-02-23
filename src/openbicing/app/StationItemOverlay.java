package openbicing.app;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.OverlayItem;

public class StationItemOverlay extends OverlayItem {
	
	private int bikes;
	private int free;
	private String timestamp;
	private int status;
	private int id;
	private Context context;
	private Drawable icon;
	
	private static final int RED_STATE = 1;
	private static final int YELLOW_STATE = 2;
	private static final int GREEN_STATE = 3;
	
	private static final int RED_STATE_MAX = 1;
	private static final int YELLOW_STATE_MAX = 5;
	
	private static final int RED_MARKER = R.drawable.bullet_green;
	private static final int YELLOW_MARKER = R.drawable.bullet_green;
	private static final int GREEN_MARKER = R.drawable.bullet_green;
	
	

	
	public StationItemOverlay(GeoPoint point, String title, String snippet, Context context, int bikes, int free, String timestamp, int id) {
		super(point, title, snippet);
		this.bikes = bikes;
		this.free = free;
		this.id = id;
		this.timestamp = timestamp;
		this.status = RED_STATE;
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
		super.setMarker(this.icon);
	}

	@Override
	public Drawable getMarker(int stateBitset) {
		// TODO Auto-generated method stub
		return super.getMarker(stateBitset);
		
	}
	
	public void update(){
		// TODO Update aviability.. :D
		this.updateStatus();
	}
	
}