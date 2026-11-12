package nz.cassey.compass;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

public class CompassView extends View {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF btnL = new RectF(), btnR = new RectF();
    private float heading = 0f, lockedAt = Float.NaN;
    private boolean noSensor = false, updateReady = false, trueNorth = true;
    private String versionText = "", statusText = "", northLine = "";
    private Runnable onFooterTap, onToggleNorth, onToggleLock;
    private static final String[] NAMES = {"N","NE","E","SE","S","SW","W","NW"};

    public CompassView(Context c) { super(c); setBackgroundColor(Color.rgb(18, 22, 28)); }

    public void setHeading(float h) { heading = h; invalidate(); }
    public void setNoSensor() { noSensor = true; invalidate(); }
    public void setVersion(String s) { versionText = s; invalidate(); }
    public void setStatus(String s, boolean update) { statusText = s; updateReady = update; invalidate(); }
    public void setNorth(boolean trueMode, String line) { trueNorth = trueMode; northLine = line; invalidate(); }
    public void setLock(float deg) { lockedAt = deg; invalidate(); }
    public void setOnFooterTap(Runnable r) { onFooterTap = r; }
    public void setOnToggleNorth(Runnable r) { onToggleNorth = r; }
    public void setOnToggleLock(Runnable r) { onToggleLock = r; }

    @Override public boolean onTouchEvent(MotionEvent e) {
        if (e.getAction() == MotionEvent.ACTION_UP) {
            float x = e.getX(), y = e.getY();
            if (btnL.contains(x, y) && onToggleNorth != null) onToggleNorth.run();
            else if (btnR.contains(x, y) && onToggleLock != null) onToggleLock.run();
            else if (y > getHeight() * 0.88f && onFooterTap != null) onFooterTap.run();
        }
        return true;
    }

    @Override protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight();
        float r = Math.min(w * 0.40f, h * 0.29f);
        float cx = w / 2f, cy = h * 0.39f;
        float u = Math.min(w, h) * 0.04f;
        p.setTextAlign(Paint.Align.CENTER);
        drawBottom(c, w, h, u);

        if (noSensor) {
            p.setStyle(Paint.Style.FILL); p.setColor(Color.WHITE); p.setTextSize(u * 1.4f);
            c.drawText("No compass sensor on this device", cx, cy, p);
            return;
        }

        int deg = Math.round(heading) % 360;
        int idx = Math.round(heading / 45f) % 8;
        p.setStyle(Paint.Style.FILL); p.setColor(Color.WHITE); p.setTextSize(r * 0.30f);
        c.drawText(deg + "\u00B0 " + NAMES[idx], cx, cy - r - r * 0.30f, p);
        p.setTextSize(u * 1.05f); p.setColor(trueNorth ? Color.rgb(102, 187, 106) : Color.rgb(255, 167, 38));
        c.drawText(northLine, cx, cy - r - r * 0.12f, p);

        Path tri = new Path();
        tri.moveTo(cx, cy - r - r * 0.02f); tri.lineTo(cx - r * 0.06f, cy - r - r * 0.10f); tri.lineTo(cx + r * 0.06f, cy - r - r * 0.10f); tri.close();
        p.setColor(Color.rgb(255, 193, 7)); c.drawPath(tri, p);

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

        if (!Float.isNaN(lockedAt)) {
            c.save(); c.rotate(lockedAt, cx, cy);
            p.setColor(Color.rgb(41, 182, 246));
            Path m = new Path();
            m.moveTo(cx, cy - r * 0.46f); m.lineTo(cx - r * 0.07f, cy - r * 0.56f); m.lineTo(cx + r * 0.07f, cy - r * 0.56f); m.close();
            c.drawPath(m, p);
            p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(r * 0.015f);
            c.drawLine(cx, cy - r * 0.56f, cx, cy - r * 0.90f, p);
            p.setStyle(Paint.Style.FILL);
            c.restore();
        }

        Path n = new Path();
        n.moveTo(cx, cy - r * 0.45f); n.lineTo(cx - r * 0.07f, cy); n.lineTo(cx + r * 0.07f, cy); n.close();
        p.setColor(Color.rgb(255, 82, 82)); c.drawPath(n, p);
        Path s = new Path();
        s.moveTo(cx, cy + r * 0.45f); s.lineTo(cx - r * 0.07f, cy); s.lineTo(cx + r * 0.07f, cy); s.close();
        p.setColor(Color.rgb(230, 230, 230)); c.drawPath(s, p);
        c.restore();

        p.setColor(Color.rgb(18, 22, 28)); c.drawCircle(cx, cy, r * 0.035f, p);

        // Lock readout under dial
        p.setTextSize(u * 1.3f);
        if (!Float.isNaN(lockedAt)) {
            float d = ((lockedAt - heading + 540f) % 360f) - 180f;
            int off = Math.round(Math.abs(d));
            String t = off <= 1 ? "On course" : "Turn " + off + "\u00B0 " + (d > 0 ? "right" : "left");
            p.setColor(Color.rgb(41, 182, 246));
            c.drawText("Locked " + Math.round(lockedAt) % 360 + "\u00B0  \u2022  " + t, cx, cy + r + u * 2.2f, p);
        }
    }

    private void drawBottom(Canvas c, float w, float h, float u) {
        float top = h * 0.76f, bh = h * 0.075f, pad = w * 0.06f, gap = w * 0.04f;
        float bw = (w - 2 * pad - gap) / 2f;
        btnL.set(pad, top, pad + bw, top + bh);
        btnR.set(pad + bw + gap, top, w - pad, top + bh);
        p.setStyle(Paint.Style.FILL); p.setTextSize(u * 1.25f); p.setFakeBoldText(true);
        p.setColor(Color.rgb(45, 55, 70)); c.drawRoundRect(btnL, u, u, p);
        p.setColor(Float.isNaN(lockedAt) ? Color.rgb(45, 55, 70) : Color.rgb(2, 119, 189)); c.drawRoundRect(btnR, u, u, p);
        p.setColor(Color.WHITE);
        float ty = top + bh / 2f + u * 0.45f;
        c.drawText(trueNorth ? "True north \u2713" : "Magnetic north", btnL.centerX(), ty, p);
        c.drawText(Float.isNaN(lockedAt) ? "Lock heading" : "Unlock", btnR.centerX(), ty, p);
        p.setFakeBoldText(false);

        p.setTextSize(u * 1.1f);
        p.setColor(updateReady ? Color.rgb(255, 193, 7) : Color.rgb(170, 180, 195));
        c.drawText(statusText, w / 2f, h - u * 3.2f, p);
        p.setTextSize(u); p.setColor(Color.rgb(120, 130, 145));
        c.drawText(versionText, w / 2f, h - u * 1.6f, p);
    }
}
