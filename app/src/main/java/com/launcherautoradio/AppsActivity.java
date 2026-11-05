package com.launcherautoradio;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.MotionEvent;
import android.widget.GridLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Collections;
import java.util.List;

// App drawer in swipeable pages of big tiles. With extra "pick", returns the chosen package instead of launching it.
public class AppsActivity extends Activity {
    static final int COLS = 5, ROWS = 2, PER_PAGE = COLS * ROWS;

    List<ResolveInfo> apps;
    boolean pick;
    HorizontalScrollView pager;
    TextView dots;
    int pageW, pageCount, startX;

    @SuppressLint("ClickableViewAccessibility") // swipe snapping only; tiles stay real clickable views
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_apps);
        PackageManager pm = getPackageManager();
        apps = pm.queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0);
        apps.removeIf(r -> r.activityInfo.packageName.equals(getPackageName()));
        Collections.sort(apps, new ResolveInfo.DisplayNameComparator(pm));
        pick = getIntent().getBooleanExtra("pick", false);

        ((TextView) findViewById(R.id.header)).setText(pick ? "Choisis une appli" : "Applis");
        findViewById(R.id.back).setOnClickListener(v -> finish());
        dots = findViewById(R.id.dots);
        pager = findViewById(R.id.pager);
        pager.post(() -> buildPages(pm));
        pager.setOnScrollChangeListener((v, x, y, ox, oy) -> showDots());
        pager.setOnTouchListener((v, e) -> {
            if (e.getAction() == MotionEvent.ACTION_DOWN) startX = pager.getScrollX();
            if (e.getAction() == MotionEvent.ACTION_UP || e.getAction() == MotionEvent.ACTION_CANCEL) {
                // Snap to a whole page; a swipe of a sixth of the screen is enough to turn it.
                int page = Math.round((float) startX / pageW), dx = pager.getScrollX() - startX;
                if (dx > pageW / 6) page++; else if (dx < -pageW / 6) page--;
                int target = Math.max(0, Math.min(pageCount - 1, page)) * pageW;
                pager.post(() -> pager.smoothScrollTo(target, 0));
            }
            return false;
        });
    }

    void buildPages(PackageManager pm) {
        pageW = pager.getWidth();
        int h = pager.getHeight();
        float dp = getResources().getDisplayMetrics().density;
        int gap = (int) (20 * dp), icon = (int) (80 * dp), pad = (int) (12 * dp);
        int tileW = (pageW - gap * COLS) / COLS, tileH = (h - gap * ROWS) / ROWS;
        pageCount = Math.max(1, (apps.size() + PER_PAGE - 1) / PER_PAGE);
        LinearLayout pages = findViewById(R.id.pages);

        for (int p = 0; p < pageCount; p++) {
            GridLayout grid = new GridLayout(this);
            grid.setColumnCount(COLS);
            grid.setPadding(gap / 2, 0, gap / 2, 0);
            pages.addView(grid, new LinearLayout.LayoutParams(pageW, h));
            for (int i = p * PER_PAGE; i < Math.min(apps.size(), (p + 1) * PER_PAGE); i++) {
                ResolveInfo r = apps.get(i);
                LinearLayout tile = new LinearLayout(this);
                tile.setOrientation(LinearLayout.VERTICAL);
                tile.setGravity(Gravity.CENTER);
                tile.setPadding(pad, pad, pad, pad);
                tile.setBackgroundResource(R.drawable.tile);
                tile.setClickable(true);
                tile.setFocusable(true);

                ImageView img = new ImageView(this);
                img.setImageDrawable(r.loadIcon(pm));
                img.setImportantForAccessibility(ImageView.IMPORTANT_FOR_ACCESSIBILITY_NO);
                tile.addView(img, new LinearLayout.LayoutParams(icon, icon));

                TextView label = new TextView(this);
                label.setText(r.loadLabel(pm));
                label.setTextColor(getColor(R.color.text));
                label.setTextSize(18);
                label.setGravity(Gravity.CENTER);
                label.setMaxLines(2);
                label.setEllipsize(TextUtils.TruncateAt.END);
                label.setPadding(0, pad, 0, 0);
                tile.addView(label);

                tile.setOnClickListener(v -> open(r));
                // Long press shows the package name (needed to integrate another app's data).
                tile.setOnLongClickListener(v -> {
                    Toast.makeText(this, r.activityInfo.packageName, Toast.LENGTH_LONG).show();
                    return true;
                });
                GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
                lp.width = tileW;
                lp.height = tileH;
                lp.setMargins(gap / 2, gap / 2, gap / 2, gap / 2);
                grid.addView(tile, lp);
            }
        }
        showDots();
    }

    void showDots() {
        if (pageW == 0) return;
        int cur = Math.round((float) pager.getScrollX() / pageW);
        StringBuilder s = new StringBuilder();
        for (int p = 0; p < pageCount; p++) s.append(p == cur ? '●' : '○');
        dots.setText(pageCount > 1 ? s : "");
        dots.setContentDescription("Page " + (cur + 1) + " sur " + pageCount);
    }

    void open(ResolveInfo r) {
        if (pick) {
            setResult(RESULT_OK, new Intent().putExtra("pkg", r.activityInfo.packageName));
        } else {
            startActivity(new Intent(Intent.ACTION_MAIN)
                    .setClassName(r.activityInfo.packageName, r.activityInfo.name)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        }
        finish();
    }
}
