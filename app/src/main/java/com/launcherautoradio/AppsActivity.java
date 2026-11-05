package com.launcherautoradio;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.TextView;
import java.util.Collections;
import java.util.List;

// App drawer. With extra "pick", returns the chosen package instead of launching it.
public class AppsActivity extends Activity {
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        PackageManager pm = getPackageManager();
        List<ResolveInfo> apps = pm.queryIntentActivities(
                new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0);
        Collections.sort(apps, new ResolveInfo.DisplayNameComparator(pm));
        boolean pick = getIntent().getBooleanExtra("pick", false);
        int px = (int) (getResources().getDisplayMetrics().density * 72);

        GridView grid = new GridView(this);
        grid.setNumColumns(GridView.AUTO_FIT);
        grid.setColumnWidth(px * 2);
        grid.setVerticalSpacing(px / 3);
        grid.setPadding(px / 2, px / 2, px / 2, px / 2);
        grid.setBackgroundColor(getColor(R.color.bg));
        grid.setAdapter(new BaseAdapter() {
            public int getCount() { return apps.size(); }
            public Object getItem(int i) { return apps.get(i); }
            public long getItemId(int i) { return i; }
            public View getView(int i, View v, ViewGroup parent) {
                TextView t = v != null ? (TextView) v : new TextView(AppsActivity.this);
                ResolveInfo r = apps.get(i);
                Drawable d = r.loadIcon(pm);
                d.setBounds(0, 0, px, px);
                t.setCompoundDrawables(null, d, null, null);
                t.setText(r.loadLabel(pm));
                t.setGravity(Gravity.CENTER);
                t.setTextColor(getColor(R.color.text));
                t.setTextSize(16);
                t.setMaxLines(2);
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
        setContentView(grid);
    }
}
