package openbicing.utils;

public class CircleHelper {
	public static final boolean isOnCircle(float x, float y, float centerX, float centerY, float radius){
		double square_dist = Math.pow(centerX - x, 2) + Math.pow(centerY - y,2);
		return square_dist <= Math.pow(radius,2);
	}
}
