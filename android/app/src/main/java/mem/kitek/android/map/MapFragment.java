package mem.kitek.android.map;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.qozix.tileview.TileView;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.FragmentArg;
import org.androidannotations.annotations.ViewById;

import javax.inject.Inject;

import autodagger.AutoComponent;
import autodagger.AutoInjector;
import dagger.Module;
import dagger.Provides;
import lombok.Getter;
import mem.kitek.R;
import mem.kitek.android.MemeApplication;
import mem.kitek.android.MemeApplicationComponent;
import mem.kitek.android.meta.BaseFragment;
import mem.kitek.android.meta.scope.FragmentScope;
import mem.kitek.android.recommend.CompositeImage;
import mem.kitek.android.view.LoadingOverlayer;

/**
 * Created by cat on 10/21/17.
 */

@FragmentScope
@AutoInjector
@AutoComponent(dependencies = {MemeApplication.class}, modules = MapFragment.Exposer.class)
@EFragment(R.layout.map_fragment)
public class MapFragment extends BaseFragment {
    public static final String TAG = "Map";
    @ViewById @Getter
    TileView map;
    @Nullable @Getter
    private ViewGroup container;
    @Inject
    MapFragmentPresenter presenter;
    @Nullable @FragmentArg
    CompositeImage.Holder resolution;
    @ViewById
    LoadingOverlayer overlayer;

    @Override
    protected void performInject(MemeApplicationComponent component) {
        Log.d(TAG, "performInject: yey!");

        DaggerMapFragmentComponent
                .builder()
                .memeApplicationComponent(component)
                .exposer(new Exposer())
                .build()
                .inject(this);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        this.container = container;
        Log.d(TAG, "onCreateView: creating it yo");
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onStart() {
        Log.d(TAG, "onStart: STARTING!");
        super.onStart();
    }

    @Module
    public class Exposer {
        @Provides
        MapFragment provideIt() {
            return MapFragment.this;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        presenter.dropAll();
    }

    @AfterViews
    void init() {
        Log.d(TAG, "init: happening!");
        presenter.setupMap();
        disableOverlay();

        if (resolution != null) {
            enableOverlay("Высчитываем маршрут...");
            presenter.handlePrefs(resolution.getImageList());
        }
    }

    void enableOverlay(String text) {
        overlayer.setVisibility(View.VISIBLE);

        overlayer.setText(text);
    }

    void disableOverlay() {
        overlayer.setVisibility(View.GONE);
    }
}
