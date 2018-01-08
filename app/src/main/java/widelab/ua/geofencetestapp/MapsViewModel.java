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

import com.github.pwittchen.reactivenetwork.library.rx2.ConnectivityPredicate;
import com.github.pwittchen.reactivenetwork.library.rx2.ReactiveNetwork;
import com.google.android.gms.location.LocationRequest;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import pl.charmas.android.reactivelocation2.ReactiveLocationProvider;

public class MapsViewModel extends AndroidViewModel {

    // TODO: 08.01.18 get current position
    // TODO: 08.01.18 two implementations of position check

    public ObservableBoolean viewEnabled = new ObservableBoolean(false);
    public ObservableField<String> wifiName = new ObservableField<>();
    public ObservableField<String> currentStatus = new ObservableField<>();

    private MutableLiveData<Location> locationChangeEvent = new MutableLiveData<>();

    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    private BehaviorSubject<Boolean> locationEnabled = BehaviorSubject.create();

    public MapsViewModel(@NonNull Application application) {
        super(application);
        compositeDisposable.add(Observable.combineLatest(getWifiNameObservable(), getNetworkConnectivityObservable(), getLocationObservable(),
                (s, s2, location) -> s.equalsIgnoreCase(s2))
                .observeOn(AndroidSchedulers.mainThread())
                .distinct()
                .subscribe(this::setCurrentStatus, Throwable::printStackTrace));
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
                .debounce(100, TimeUnit.MILLISECONDS)
                .map(String::trim);
    }

    private Observable<String> getNetworkConnectivityObservable() {
        WifiManager wifiManager = (WifiManager) getApplication().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager == null) {
            return Observable.error(new Throwable("wifi manager is null"));
        }
        return ReactiveNetwork.observeNetworkConnectivity(getApplication())
                .subscribeOn(Schedulers.io())
                .filter(ConnectivityPredicate.hasState(NetworkInfo.State.CONNECTED))
                .filter(ConnectivityPredicate.hasType(ConnectivityManager.TYPE_WIFI))
                .map(__ -> wifiManager.getConnectionInfo().getSSID().replace("\"", "").trim());
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
}
