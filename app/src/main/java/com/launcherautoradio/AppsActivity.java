package com.launcherautoradio;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.TextView;
import java.util.Collections;
import java.util.List;

// App drawer with big touch targets. With extra "pick", returns the chosen package instead of launching it.
public class AppsActivity extends Activity {
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_apps);
        PackageManager pm = getPackageManager();
        List<ResolveInfo> apps = pm.queryIntentActivities(
                new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0);
        apps.removeIf(r -> r.activityInfo.packageName.equals(getPackageName()));
        Collections.sort(apps, new ResolveInfo.DisplayNameComparator(pm));
        boolean pick = getIntent().getBooleanExtra("pick", false);
        float dp = getResources().getDisplayMetrics().density;
        int icon = (int) (80 * dp), height = (int) (170 * dp), pad = (int) (16 * dp);

        ((TextView) findViewById(R.id.header)).setText(pick ? "Choisis une appli" : "Applis");
        findViewById(R.id.back).setOnClickListener(v -> finish());

        GridView grid = findViewById(R.id.grid);
        grid.setAdapter(new BaseAdapter() {
            public int getCount() { return apps.size(); }
            public Object getItem(int i) { return apps.get(i); }
            public long getItemId(int i) { return i; }
            public View getView(int i, View v, ViewGroup parent) {
                TextView t = (TextView) v;
                if (t == null) {
                    t = new TextView(AppsActivity.this);
                    t.setLayoutParams(new AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height));
                    t.setBackgroundResource(R.drawable.tile);
                    t.setGravity(Gravity.CENTER);
                    t.setPadding(pad, pad, pad, pad);
                    t.setCompoundDrawablePadding(pad / 2);
                    t.setTextColor(getColor(R.color.text));
                    t.setTextSize(18);
                    t.setMaxLines(2);
                    t.setEllipsize(TextUtils.TruncateAt.END);
                }
                ResolveInfo r = apps.get(i);
                Drawable d = r.loadIcon(pm);
                d.setBounds(0, 0, icon, icon);
                t.setCompoundDrawables(null, d, null, null);
                t.setText(r.loadLabel(pm));
                return t;
            }
        });
        grid.setOnItemClickListener((p, v, i, id) -> {
            ResolveInfo r = apps.get(i);
            if (pick) {
                setResult(RESULT_OK, new Intent().putExtra("pkg", r.activityInfo.packageName));
            } else {
                startActivity(new Intent(Intent.ACTION_MAIN)
                        .setClassName(r.activityInfo.packageName, r.activityInfo.name)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            }
            finish();
        });
    }
}
