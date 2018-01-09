package widelab.ua.geofencetestapp;

import android.annotation.SuppressLint;
import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;
import android.databinding.ObservableBoolean;
import android.databinding.ObservableField;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.github.pwittchen.reactivenetwork.library.rx2.ConnectivityPredicate;
import com.github.pwittchen.reactivenetwork.library.rx2.ReactiveNetwork;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.model.LatLng;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import pl.charmas.android.reactivelocation2.ReactiveLocationProvider;

public class MapsViewModel extends AndroidViewModel {

    private final static int DEBOUNCE = 100;

    public ObservableBoolean viewEnabled = new ObservableBoolean(false);
    public ObservableField<String> wifiName = new ObservableField<>();
    public ObservableField<String> radius = new ObservableField<>(Float.toString(Area.DEFAULT_RADIUS));
    public ObservableField<String> currentStatus = new ObservableField<>();

    private MutableLiveData<Area> areaChangeEvent = new MutableLiveData<>();

    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    private BehaviorSubject<Boolean> locationEnabled = BehaviorSubject.create();

    private Area area;

    public MapsViewModel(@NonNull Application application) {
        super(application);
        area = new Area();
        compositeDisposable.add(Observable.combineLatest(getWifiNameObservable(), getNetworkConnectivityObservable(), getLocationObservable(), getAreaObservable(),
                (inputName, networkName, location, areaCheck) -> (!TextUtils.isEmpty(inputName) && inputName.equalsIgnoreCase(networkName)) || areaCheck.isLocationInArea(location))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::setCurrentStatus, Throwable::printStackTrace));
        compositeDisposable.add(RxUtils.toObservable(radius)
                .filter(radius -> radius.matches("\\d+\\.?\\d*"))
                .map(Float::parseFloat)
                .subscribe(radius -> area.setRadius(radius)));
    }

    private void setCurrentStatus(boolean inside) {
        currentStatus.set(getApplication().getString(inside ? R.string.point_inside : R.string.point_outside));
    }

    @SuppressLint("MissingPermission")
    void setLocationEnabled(boolean locationPermissionGranted) {
        locationEnabled.onNext(locationPermissionGranted);
    }

    private Observable<String> getWifiNameObservable() {
        return RxUtils.toObservable(wifiName)
                .startWith("")
                .debounce(DEBOUNCE, TimeUnit.MILLISECONDS)
                .map(String::trim);
    }

    private Observable<String> getNetworkConnectivityObservable() {
        WifiManager wifiManager = (WifiManager) getApplication().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager == null) {
            return Observable.error(new Throwable("wifi manager is null"));
        }
        return ReactiveNetwork.observeNetworkConnectivity(getApplication())
                .subscribeOn(Schedulers.io())
                .filter(ConnectivityPredicate.hasType(ConnectivityManager.TYPE_WIFI))
                .map(connectivity -> {
                    if (connectivity.getState() == NetworkInfo.State.CONNECTED) {
                        return wifiManager.getConnectionInfo().getSSID().replace("\"", "").trim();
                    } else {
                        return "";
                    }
                });
    }

    @SuppressLint("MissingPermission")
    private Observable<Location> getLocationObservable() {
        return locationEnabled
                .flatMap(locationEnabled -> {
                    if (locationEnabled) {
                        LocationRequest request = LocationRequest.create()
                                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                        ReactiveLocationProvider locationProvider = new ReactiveLocationProvider(getApplication());
                        return locationProvider.getUpdatedLocation(request);
                    } else {
                        return Observable.empty();
                    }
                })
                .startWith(Observable.just(new Location(AreaCheck.EXCLUDE_PROVIDER)));
    }

    private Observable<AreaCheck> getAreaObservable() {
        return area.getChangedAreaObservable()
                .doOnNext(__ -> {
                    if (area.getCenter() != null) {
                        areaChangeEvent.postValue(this.area);
                    }
                });

    }

    void mapReady() {
        viewEnabled.set(true);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        locationEnabled.onComplete();
        compositeDisposable.dispose();
    }

    void onMapClick(LatLng latLng) {
        area.setCenter(latLng);
    }

    MutableLiveData<Area> getAreaChangeEvent() {
        return areaChangeEvent;
    }
}
