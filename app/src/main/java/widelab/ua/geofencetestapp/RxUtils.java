package widelab.ua.geofencetestapp;

import android.databinding.ObservableField;

import io.reactivex.Observable;

class RxUtils {

    private RxUtils() {
    }

    static <T> Observable<T> toObservable(final ObservableField<T> observableField) {
        return Observable.create(emitter -> {
            T value = observableField.get();
            if (value != null) {
                emitter.onNext(value);
            }
            final android.databinding.Observable.OnPropertyChangedCallback onPropertyChangedCallback = new android.databinding.Observable.OnPropertyChangedCallback() {
                @Override
                public void onPropertyChanged(android.databinding.Observable observable, int i) {
                    emitter.onNext(observableField.get());
                }
            };
            observableField.addOnPropertyChangedCallback(onPropertyChangedCallback);
            emitter.setCancellable(() -> observableField.removeOnPropertyChangedCallback(onPropertyChangedCallback));
        });
    }
}
