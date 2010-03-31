package openbicing.app;

import java.util.List;

import android.content.Context;
import android.graphics.Paint;
import android.os.Handler;
import android.util.Log;

import com.google.android.maps.Overlay;

public class StationOverlayList {

	private List<Overlay> mapOverlays;
	private Context context;
	private HomeOverlay hOverlay;
	private Handler handler;
	private int current = -1;
	private int first;
	private StationOverlay last = null;

	public StationOverlayList(Context context, List<Overlay> mapOverlays,
			Handler handler) {
		this.context = context;
		this.mapOverlays = mapOverlays;
		this.handler = handler;
		hOverlay = new HomeOverlay(this.context, handler);
		hOverlay.setLastKnownLocation();
		addHome();
	}

	public List<Overlay> getList() {
		return mapOverlays;
	}

	public void addStationOverlay(Overlay overlay) {
		if (overlay instanceof StationOverlay) {
			StationOverlay sht = (StationOverlay) overlay;
			sht.setHandler(handler);
		}
		this.mapOverlays.add(overlay);
		if (this.current == -1) {
			this.current = 0;
			this.first = 0;
		}
	}

	public void setCurrent(int position) {
		if (this.last != null)
			this.last.setSelected(false);
		else {
			StationOverlay sht = (StationOverlay) mapOverlays.get(current);
			sht.setSelected(false);
		}
		this.current = position;
		this.last = (StationOverlay) mapOverlays.get(current);
		this.last.setSelected(true);

	}

	public void updatePositions() {
		int i = 0;
		StationOverlay tmp;
		while (i < mapOverlays.size()) {
			if (mapOverlays.get(i) instanceof StationOverlay) {
				tmp = (StationOverlay) mapOverlays.get(i);
				tmp.setPosition(i);
			}
			i++;
		}
	}

	public void addHome() {
		mapOverlays.add(hOverlay);
	}

	public void addStationOverlay(int location, Overlay overlay) {
		this.mapOverlays.add(location, overlay);
	}

	public void setStationOverlay(int location, Overlay overlay) {
		this.mapOverlays.set(location, overlay);
	}

	public void updateStationOverlay(int location) {
		StationOverlay station = (StationOverlay) this.mapOverlays
				.get(location);
		station.update();
		// TODO: Roc Boronat: He comentat la línia de sota, ja que en teoria
		// sobra. Provar si funciona! :)
		// this.mapOverlays.set(location, station);
	}

	public void updateHome() {
		hOverlay.setLastKnownLocation();
	}

	public void clear() {
		mapOverlays.clear();
		current = -1;
		addHome();
	}

	public HomeOverlay getHome() {
		return hOverlay;
	}

	public StationOverlay getCurrent() {
		if (current!=-1)
			return (StationOverlay) mapOverlays.get(current);
		else
			return null;
	}

	public StationOverlay selectNext() {
		if (last != null)
			last.setSelected(false);
		else {
			StationOverlay sht = (StationOverlay) mapOverlays.get(current);
			sht.setSelected(false);
		}
		do {
			current++;
			if (current > mapOverlays.size() - 1)
				current = first;
		} while (!(mapOverlays.get(current) instanceof StationOverlay)
				&& mapOverlays.size() > 1);

		if (mapOverlays.get(current) instanceof StationOverlay) {
			StationOverlay res = (StationOverlay) mapOverlays.get(current);
			res.setSelected(true);
			last = res;
			Log.i("openBicing", "Magic Number: "
					+ Double.toString(res.getGradialSeparation()));
			return res;
		} else
			return null;

	}

	public StationOverlay selectPrevious() {
		if (last != null)
			last.setSelected(false);
		else {
			StationOverlay sht = (StationOverlay) mapOverlays.get(current);
			sht.setSelected(false);
		}
		do {
			current--;
			if (current < 0) {
				current = mapOverlays.size() - 1;
			}
		} while (!(mapOverlays.get(current) instanceof StationOverlay)
				&& mapOverlays.size() > 1);

		if (mapOverlays.get(current) instanceof StationOverlay) {
			StationOverlay res = (StationOverlay) mapOverlays.get(current);
			res.setSelected(true);
			last = res;
			return res;
		} else {
			return null;
		}
	}
}
