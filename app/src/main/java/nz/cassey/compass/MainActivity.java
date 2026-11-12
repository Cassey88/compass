package nz.cassey.compass;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import android.view.Surface;
import android.view.WindowManager;

public class MainActivity extends Activity implements SensorEventListener {
    private SensorManager sm;
    private Sensor rotation, accel, magnet;
    private CompassView view;
    private final float[] gravity = new float[3], geomag = new float[3];
    private boolean haveG, haveM;
    private float smoothed = Float.NaN;
    private String current = "0", updateUrl = null;
    private final Handler ui = new Handler(Looper.getMainLooper());

    private void checkUpdate() {
        post("Checking for updates...", false);
        new Thread(new Runnable() { public void run() {
            try {
                HttpURLConnection con = (HttpURLConnection) new URL("https://api.github.com/repos/Cassey88/compass/releases/latest").openConnection();
                con.setConnectTimeout(8000); con.setReadTimeout(8000);
                con.setRequestProperty("Accept", "application/vnd.github+json");
                BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));
                StringBuilder sb = new StringBuilder(); String line;
                while ((line = br.readLine()) != null) sb.append(line);
                br.close();
                JSONObject j = new JSONObject(sb.toString());
                String tag = j.getString("tag_name").replace("v", "");
                String url = j.getJSONArray("assets").getJSONObject(0).getString("browser_download_url");
                if (newer(tag, current)) { updateUrl = url; post("Update available: v" + tag + " - tap here to download", true); }
                else { updateUrl = null; post("You have the latest version", false); }
            } catch (Exception e) { post("Could not check for updates (offline?)", false); }
        } }).start();
    }

    private boolean newer(String a, String b) {
        String[] x = a.split("\\."), y = b.split("\\.");
        for (int i = 0; i < Math.max(x.length, y.length); i++) {
            int p = i < x.length ? Integer.parseInt(x[i]) : 0, q = i < y.length ? Integer.parseInt(y[i]) : 0;
            if (p != q) return p > q;
        }
        return false;
    }

    private void post(final String s, final boolean upd) { ui.post(new Runnable() { public void run() { view.setStatus(s, upd); } }); }

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        view = new CompassView(this);
        setContentView(view);
        String ver = "?";
        try { ver = getPackageManager().getPackageInfo(getPackageName(), 0).versionName; } catch (Exception ignored) {}
        current = ver;
        view.setVersion("Compass v" + ver + "  \u2022  tap here to check for updates");
        view.setOnFooterTap(new Runnable() { public void run() { if (updateUrl != null) startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(updateUrl))); else checkUpdate(); } });
        checkUpdate();
        sm = (SensorManager) getSystemService(SENSOR_SERVICE);
        rotation = sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        accel = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnet = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        if (rotation == null && (accel == null || magnet == null)) view.setNoSensor();
    }

    @Override protected void onResume() {
        super.onResume();
        if (rotation != null) sm.registerListener(this, rotation, SensorManager.SENSOR_DELAY_GAME);
        else if (accel != null && magnet != null) {
            sm.registerListener(this, accel, SensorManager.SENSOR_DELAY_GAME);
            sm.registerListener(this, magnet, SensorManager.SENSOR_DELAY_GAME);
        }
    }

    @Override protected void onPause() { super.onPause(); sm.unregisterListener(this); }

    @Override public void onSensorChanged(SensorEvent e) {
        float[] r = new float[9], out = new float[9], o = new float[3];
        if (e.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            SensorManager.getRotationMatrixFromVector(r, e.values);
        } else {
            if (e.sensor.getType() == Sensor.TYPE_ACCELEROMETER) { lp(gravity, e.values); haveG = true; }
            else if (e.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) { lp(geomag, e.values); haveM = true; }
            if (!haveG || !haveM || !SensorManager.getRotationMatrix(r, null, gravity, geomag)) return;
        }
        int rot = getWindowManager().getDefaultDisplay().getRotation();
        int ax = SensorManager.AXIS_X, ay = SensorManager.AXIS_Y;
        if (rot == Surface.ROTATION_90) { ax = SensorManager.AXIS_Y; ay = SensorManager.AXIS_MINUS_X; }
        else if (rot == Surface.ROTATION_180) { ax = SensorManager.AXIS_MINUS_X; ay = SensorManager.AXIS_MINUS_Y; }
        else if (rot == Surface.ROTATION_270) { ax = SensorManager.AXIS_MINUS_Y; ay = SensorManager.AXIS_X; }
        SensorManager.remapCoordinateSystem(r, ax, ay, out);
        SensorManager.getOrientation(out, o);
        float az = (float) Math.toDegrees(o[0]);
        if (az < 0) az += 360f;
        if (Float.isNaN(smoothed)) smoothed = az;
        else {
            float d = ((az - smoothed + 540f) % 360f) - 180f;
            smoothed = (smoothed + 0.15f * d + 360f) % 360f;
        }
        view.setHeading(smoothed);
    }

    private void lp(float[] out, float[] in) { for (int i = 0; i < 3; i++) out[i] += 0.1f * (in[i] - out[i]); }

    @Override public void onAccuracyChanged(Sensor s, int a) {}
}
