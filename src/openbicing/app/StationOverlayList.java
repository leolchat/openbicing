package openbicing.app;

import java.util.List;

import android.content.Context;
import android.os.Handler;

import com.google.android.maps.Overlay;

public class StationOverlayList {
	
	private List <Overlay> mapOverlays;
	private Context context;
	private HomeOverlay hOverlay;
	private Handler handler;
	
	public StationOverlayList (Context context, List <Overlay> mapOverlays, Handler handler) {
		this.context = context; 
		this.mapOverlays = mapOverlays;
		this.handler = handler;
		hOverlay = new HomeOverlay(this.context,handler);
    	hOverlay.setLastKnownLocation();
    	addHome();
	}
	
	public void addStationOverlay(Overlay overlay) {
		this.mapOverlays.add(overlay);
	}
	
	public void addHome(){
		mapOverlays.add(hOverlay);
    }
	
	public void addStationOverlay(int location, Overlay overlay) {
		this.mapOverlays.add(location, overlay);
	}
	
	public void setStationOverlay(int location, Overlay overlay){
		this.mapOverlays.set(location, overlay);
	}
	
	public void updateStationOverlay(int location){
		StationOverlay station = (StationOverlay) this.mapOverlays.get(location);
		station.update();
                //TODO: Roc Boronat: He comentat la línia de sota, ja que en teoria sobra. Provar si funciona! :)
		//this.mapOverlays.set(location, station);
	}
	public void updateHome(){
		hOverlay.setLastKnownLocation();
	}
	
	public void clear(){
		mapOverlays.clear();
		addHome();
	}
	public HomeOverlay getHome(){
		return hOverlay;
	}
}
