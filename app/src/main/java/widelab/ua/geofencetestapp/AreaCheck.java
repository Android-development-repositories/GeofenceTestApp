package widelab.ua.geofencetestapp;

import android.location.Location;

import com.google.android.gms.maps.model.LatLng;

class AreaCheck {
    public static final String EXCLUDE_PROVIDER = "MY_WIDELAB_PROVIDER";
    private Area area;

    AreaCheck(Area area) {
        this.area = area;
    }

    boolean isLocationInArea(Location location) {
        LatLng areaCenter = area.getCenter();
        if (areaCenter == null || location.getProvider().equals(EXCLUDE_PROVIDER)) {
            return false;
        }
        float[] result = new float[1];
        Location.distanceBetween(areaCenter.latitude, areaCenter.longitude, location.getLatitude(), location.getLongitude(), result);
        return result[0] < area.getRadius();
    }

}
