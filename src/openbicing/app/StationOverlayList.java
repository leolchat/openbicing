package openbicing.app;

import java.util.List;

import android.content.Context;

import com.google.android.maps.Overlay;

public class StationOverlayList {
	
	private List <Overlay> mapOverlays;
	private Context context;
	private HomeOverlay hOverlay;
	
	public StationOverlayList (Context context, List <Overlay> mapOverlays) {
		this.context = context; 
		this.mapOverlays = mapOverlays;
		hOverlay = new HomeOverlay(this.context);
    	hOverlay.update();
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
		hOverlay.update();
	}
	public void clear(){
		mapOverlays.clear();
		addHome();
	}
}
