package by.aleks.chordbro;

import android.app.Application;
import com.facebook.stetho.Stetho;

/**
 * Created by Alex on 2/1/16.
 */
public class ChordbroApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Stetho.initialize(
                Stetho.newInitializerBuilder(this)
                .enableWebKitInspector(Stetho.defaultInspectorModulesProvider(this))
                .build());
    }
}
