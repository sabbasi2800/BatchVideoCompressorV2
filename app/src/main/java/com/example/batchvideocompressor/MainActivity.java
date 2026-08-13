package com.example.batchvideocompressor;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.view.ViewGroup;
import java.util.ArrayList;

public class MainActivity extends Activity {
    private static final int PICK_VIDEOS = 1001;
    private static final int NOTIFICATION_PERMISSION = 1002;
    private TextView status;
    private ProgressBar progress;
    private ArrayList<Uri> selected = new ArrayList<>();

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(40, 48, 40, 40);

        TextView title = new TextView(this);
        title.setText("Batch Video Compressor V2");
        title.setTextSize(25);
        title.setPadding(0, 0, 0, 20);
        root.addView(title, new LinearLayout.LayoutParams(-1, -2));

        TextView info = new TextView(this);
        info.setText("Select several videos at once. The app compresses them one-by-one and saves copies in Movies/BatchVideoCompressor.");
        info.setTextSize(16);
        root.addView(info, new LinearLayout.LayoutParams(-1, -2));

        Button choose = new Button(this);
        choose.setText("Select videos");
        root.addView(choose, new LinearLayout.LayoutParams(-1, -2));

        Button start = new Button(this);
        start.setText("Start compression");
        root.addView(start, new LinearLayout.LayoutParams(-1, -2));

        Button stop = new Button(this);
        stop.setText("Stop");
        root.addView(stop, new LinearLayout.LayoutParams(-1, -2));

        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(100);
        progress.setProgress(0);
        root.addView(progress, new LinearLayout.LayoutParams(-1, -2));

        status = new TextView(this);
        status.setText("No videos selected.");
        status.setTextSize(15);
        status.setPadding(0, 20, 0, 0);
        root.addView(status, new LinearLayout.LayoutParams(-1, -2));

        setContentView(root);

        choose.setOnClickListener(v -> pickVideos());
        start.setOnClickListener(v -> startCompression());
        stop.setOnClickListener(v -> {
            Intent i = new Intent(this, CompressionService.class);
            i.setAction(CompressionService.ACTION_STOP);
            startService(i);
        });

        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION);
        }
    }

    private void pickVideos() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.setType("video/*");
        i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(i, PICK_VIDEOS);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != PICK_VIDEOS || resultCode != RESULT_OK || data == null) return;

        selected.clear();
        if (data.getClipData() != null) {
            for (int i = 0; i < data.getClipData().getItemCount(); i++) {
                selected.add(data.getClipData().getItemAt(i).getUri());
            }
        } else if (data.getData() != null) {
            selected.add(data.getData());
        }
        status.setText(selected.size() + " video(s) selected.");
    }

    private void startCompression() {
        if (selected.isEmpty()) {
            status.setText("Please select at least one video.");
            return;
        }
        ArrayList<String> uris = new ArrayList<>();
        for (Uri u : selected) uris.add(u.toString());

        Intent i = new Intent(this, CompressionService.class);
        i.setAction(CompressionService.ACTION_START);
        i.putStringArrayListExtra(CompressionService.EXTRA_URIS, uris);

        if (android.os.Build.VERSION.SDK_INT >= 26) startForegroundService(i);
        else startService(i);

        status.setText("Compression started. You can leave this screen.");
    }
}
