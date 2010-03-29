package openbicing.app;

import java.util.List;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.google.android.maps.Overlay;

public class StationOverlayList {
	
	private List <Overlay> mapOverlays;
	private Context context;
	private HomeOverlay hOverlay;
	private Handler handler;
	private int current = -1;
	private int first;
	private StationOverlay last = null;
	
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
		if (this.current == -1){
			this.current = 1;
			this.first = 1;
			Log.i("openBicing","Set Current: "+Integer.toString(this.current));
		}
	}
	
	public void addHome(){
		mapOverlays.add(0,hOverlay);
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
		current = -1;
		addHome();
	}
	public HomeOverlay getHome(){
		return hOverlay;
	}
	
	public StationOverlay selectNext(){
		if (last!=null)
			last.setSelected(false);
		current++;
		if (current>mapOverlays.size()-1)
			current = first;
		StationOverlay res = (StationOverlay) mapOverlays.get(current);
		res.setSelected(true);
		last = res;
		return res;
	}
	
	public StationOverlay selectPrevious(){
		if (last!=null)
			last.setSelected(false);
		current--;
		if (current <= 0){
			current = mapOverlays.size()-1;
		}
		StationOverlay res = (StationOverlay) mapOverlays.get(current);
		res.setSelected(true);
		last = res;
		return res;
	}
}
