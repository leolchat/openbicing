package openbicing.app;

import java.util.ArrayList;

import android.graphics.drawable.Drawable;

import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.OverlayItem;

public class StationItemizedOverlay extends ItemizedOverlay {

	private ArrayList<StationItemOverlay> mOverlays = new ArrayList<StationItemOverlay>();
	
	public StationItemizedOverlay(Drawable defaultMarker) {
		  super(boundCenterBottom(defaultMarker));
		  
		// TODO Auto-generated constructor stub
		  
	}
	
	@Override
	protected OverlayItem createItem(int i) {
		return mOverlays.get(i);
	}

	@Override
	public int size() {
		return mOverlays.size();
	}
	
	public void addOverlay(StationItemOverlay overlay) {
		mOverlays.add(overlay);
	}
	
	public void updateStation(int i){
		mOverlays.get(i).update();
	}
	
	public void populateAll(){
		this.populate();
	}
}