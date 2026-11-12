package nz.cassey.compass;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

public class CompassView extends View {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float heading = 0f;
    private boolean noSensor = false;
    private static final String[] NAMES = {"N","NE","E","SE","S","SW","W","NW"};

    public CompassView(Context c) { super(c); setBackgroundColor(Color.rgb(18, 22, 28)); }

    public void setHeading(float h) { heading = h; invalidate(); }
    public void setNoSensor() { noSensor = true; invalidate(); }

    @Override protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight();
        float cx = w / 2f, cy = h * 0.5f, r = Math.min(w, h) * 0.40f;
        p.setTextAlign(Paint.Align.CENTER);

        if (noSensor) {
            p.setStyle(Paint.Style.FILL); p.setColor(Color.WHITE); p.setTextSize(r * 0.14f);
            c.drawText("No compass sensor on this device", cx, cy, p);
            return;
        }

        // Heading readout
        int deg = Math.round(heading) % 360;
        int idx = Math.round(heading / 45f) % 8;
        p.setStyle(Paint.Style.FILL); p.setColor(Color.WHITE); p.setTextSize(r * 0.34f);
        c.drawText(deg + "\u00B0 " + NAMES[idx], cx, cy - r - r * 0.22f, p);

        // Fixed pointer at top
        Path tri = new Path();
        tri.moveTo(cx, cy - r - r * 0.02f); tri.lineTo(cx - r * 0.06f, cy - r - r * 0.12f); tri.lineTo(cx + r * 0.06f, cy - r - r * 0.12f); tri.close();
        p.setColor(Color.rgb(255, 193, 7)); c.drawPath(tri, p);

        // Rotating dial
        c.save();
        c.rotate(-heading, cx, cy);
        p.setStyle(Paint.Style.FILL); p.setColor(Color.rgb(32, 38, 48)); c.drawCircle(cx, cy, r, p);
        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(r * 0.02f); p.setColor(Color.rgb(120, 130, 145)); c.drawCircle(cx, cy, r, p);

        for (int a = 0; a < 360; a += 5) {
            boolean major = a % 45 == 0, mid = a % 15 == 0;
            float len = major ? r * 0.12f : mid ? r * 0.08f : r * 0.04f;
            p.setStrokeWidth(major ? r * 0.02f : r * 0.01f);
            p.setColor(major ? Color.WHITE : Color.rgb(140, 150, 165));
            c.save(); c.rotate(a, cx, cy);
            c.drawLine(cx, cy - r, cx, cy - r + len, p);
            c.restore();
        }

        p.setStyle(Paint.Style.FILL); p.setTextSize(r * 0.22f);
        String[] card = {"N", "E", "S", "W"};
        for (int i = 0; i < 4; i++) {
            c.save(); c.rotate(i * 90, cx, cy);
            p.setColor(i == 0 ? Color.rgb(255, 82, 82) : Color.WHITE);
            p.setFakeBoldText(true);
            c.drawText(card[i], cx, cy - r + r * 0.36f, p);
            c.restore();
        }
        p.setFakeBoldText(false); p.setTextSize(r * 0.1f); p.setColor(Color.rgb(170, 180, 195));
        String[] inter = {"NE", "SE", "SW", "NW"};
        for (int i = 0; i < 4; i++) {
            c.save(); c.rotate(45 + i * 90, cx, cy);
            c.drawText(inter[i], cx, cy - r + r * 0.30f, p);
            c.restore();
        }

        // Needle (north = red, south = white)
        Path n = new Path();
        n.moveTo(cx, cy - r * 0.55f); n.lineTo(cx - r * 0.07f, cy); n.lineTo(cx + r * 0.07f, cy); n.close();
        p.setColor(Color.rgb(255, 82, 82)); c.drawPath(n, p);
        Path s = new Path();
        s.moveTo(cx, cy + r * 0.55f); s.lineTo(cx - r * 0.07f, cy); s.lineTo(cx + r * 0.07f, cy); s.close();
        p.setColor(Color.rgb(230, 230, 230)); c.drawPath(s, p);
        c.restore();

        p.setColor(Color.rgb(18, 22, 28)); c.drawCircle(cx, cy, r * 0.035f, p);

        p.setColor(Color.rgb(120, 130, 145)); p.setTextSize(r * 0.09f);
        c.drawText("Keep away from magnets and metal. Wave in a figure-8 to calibrate.", cx, cy + r + r * 0.3f, p);
    }
}
