package widelab.ua.geofencetestapp;

import android.support.annotation.FloatRange;
import android.support.annotation.Nullable;

import com.google.android.gms.maps.model.LatLng;

import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;

public class Area {
    public static final float DEFAULT_RADIUS = 5000; //meters

    @Nullable
    private LatLng center;
    private float radius = DEFAULT_RADIUS;

    private BehaviorSubject<AreaCheck> areaCheck;

    Area() {
        areaCheck = BehaviorSubject.createDefault(new AreaCheck(this));
    }

    public void setCenter(LatLng center) {
        this.center = center;
        updateArea();
    }


    /**
     * @param radius in meters
     */
    public void setRadius(@FloatRange(from = 0f) float radius) {
        this.radius = radius;
        updateArea();
    }

    Observable<AreaCheck> getChangedAreaObservable() {
        return areaCheck;
    }

    private void updateArea() {
        areaCheck.onNext(new AreaCheck(this));
    }

    @Nullable
    LatLng getCenter() {
        return center;
    }

    float getRadius() {
        return radius;
    }
}
