package widelab.ua.geofencetestapp;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.content.Context;
import android.databinding.ObservableBoolean;
import android.databinding.ObservableField;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.support.annotation.NonNull;

import com.github.pwittchen.reactivenetwork.library.rx2.ConnectivityPredicate;
import com.github.pwittchen.reactivenetwork.library.rx2.ReactiveNetwork;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class MapsViewModel extends AndroidViewModel {

    // TODO: 08.01.18 get current position
    // TODO: 08.01.18 two implementations of position check

    public ObservableBoolean viewEnabled = new ObservableBoolean(false);
    public ObservableField<String> wifiName = new ObservableField<>();
    public ObservableField<String> currentStatus = new ObservableField<>();

    public MapsViewModel(@NonNull Application application) {
        super(application);
        Observable.combineLatest(getWifiNameObservable(), getNetworkConnectivityObservable(application),
                String::equalsIgnoreCase)
                .observeOn(AndroidSchedulers.mainThread())
                .distinct()
                .subscribe(this::setCurrentStatus, Throwable::printStackTrace);
    }

    private void setCurrentStatus(boolean inside) {
        currentStatus.set(getApplication().getString(inside ? R.string.point_inside : R.string.point_outside));
    }

    private Observable<String> getWifiNameObservable() {
        return RxUtils.toObservable(wifiName)
                .debounce(100, TimeUnit.MILLISECONDS)
                .map(String::trim);
    }

    private Observable<String> getNetworkConnectivityObservable(Application application) {
        WifiManager wifiManager = (WifiManager) application.getSystemService(Context.WIFI_SERVICE);
        if (wifiManager == null) {
            return Observable.error(new Throwable("wifi manager is null"));
        }
        return ReactiveNetwork.observeNetworkConnectivity(application)
                .subscribeOn(Schedulers.io())
                .filter(ConnectivityPredicate.hasState(NetworkInfo.State.CONNECTED))
                .filter(ConnectivityPredicate.hasType(ConnectivityManager.TYPE_WIFI))
                .map(__ -> wifiManager.getConnectionInfo().getSSID().replace("\"", "").trim());
    }

    void mapReady() {
        viewEnabled.set(true);
    }
}
