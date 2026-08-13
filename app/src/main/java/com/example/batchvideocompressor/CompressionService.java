package com.example.batchvideocompressor;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;
import android.provider.MediaStore;

import androidx.media3.common.MediaItem;
import androidx.media3.common.MimeTypes;
import androidx.media3.transformer.EditedMediaItem;
import androidx.media3.transformer.Composition;
import androidx.media3.transformer.ExportException;
import androidx.media3.transformer.ExportResult;
import androidx.media3.transformer.ProgressHolder;
import androidx.media3.transformer.Transformer;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CompressionService extends Service {
    public static final String ACTION_START = "START";
    public static final String ACTION_STOP = "STOP";
    public static final String EXTRA_URIS = "URIS";
    private static final String CHANNEL = "compression";
    private static final int NOTIFICATION_ID = 7;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private volatile boolean stopping = false;
    private Transformer transformer;
    private ArrayList<String> uris = new ArrayList<>();
    private int index = 0;
    private long batchStart;

    @Override public void onCreate() {
        super.onCreate();
        createChannel();
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopping = true;
            if (transformer != null) transformer.cancel();
            stopForeground(STOP_FOREGROUND_REMOVE);
            stopSelf();
            return START_NOT_STICKY;
        }

        if (intent != null && ACTION_START.equals(intent.getAction())) {
            ArrayList<String> incoming = intent.getStringArrayListExtra(EXTRA_URIS);
            if (incoming != null && !incoming.isEmpty() && executor.isShutdown() == false) {
                uris = incoming;
                stopping = false;
                index = 0;
                batchStart = SystemClock.elapsedRealtime();
                startForeground(NOTIFICATION_ID, notification("Starting batch...", 0, false));
                executor.execute(this::processBatch);
            }
        }
        return START_NOT_STICKY;
    }

    private void processBatch() {
        try {
            for (index = 0; index < uris.size() && !stopping; index++) {
                compressOne(Uri.parse(uris.get(index)), index, uris.size());
            }
            if (!stopping) updateNotification("Batch complete", 100, true);
        } catch (Exception e) {
            updateNotification("Compression error: " + safeMessage(e), 0, true);
        } finally {
            if (!stopping) {
                try { Thread.sleep(1200); } catch (InterruptedException ignored) {}
                stopForeground(STOP_FOREGROUND_REMOVE);
                stopSelf();
            }
        }
    }

    private void compressOne(Uri input, int itemIndex, int total) throws Exception {
        File temp = new File(getCacheDir(), "compressed_" + System.currentTimeMillis() + ".mp4");
        final Object lock = new Object();
        final boolean[] done = {false};
        final Exception[] error = {null};

        MediaItem mediaItem = MediaItem.fromUri(input);
        EditedMediaItem edited = new EditedMediaItem.Builder(mediaItem).build();

        transformer = new Transformer.Builder(this)
                .setVideoMimeType(MimeTypes.VIDEO_H264)
                .setAudioMimeType(MimeTypes.AUDIO_AAC)
                .addListener(new Transformer.Listener() {
                    @Override public void onCompleted(Composition composition, ExportResult exportResult) {
                        synchronized (lock) {
                            done[0] = true;
                            lock.notifyAll();
                        }
                    }

                    @Override public void onError(Composition composition, ExportResult exportResult, ExportException exportException) {
                        synchronized (lock) {
                            error[0] = exportException;
                            done[0] = true;
                            lock.notifyAll();
                        }
                    }
                })
                .build();

        updateNotification("Compressing " + (itemIndex + 1) + "/" + total, overall(itemIndex, total, 0), false);
        transformer.start(edited, temp.getAbsolutePath());

        ProgressHolder holder = new ProgressHolder();
        while (true) {
            synchronized (lock) {
                if (done[0]) break;
            }
            if (stopping) {
                transformer.cancel();
                throw new InterruptedException("Stopped");
            }
            int state = transformer.getProgress(holder);
            if (state == Transformer.PROGRESS_STATE_AVAILABLE) {
                int p = Math.max(0, Math.min(100, holder.progress));
                updateNotification("Compressing " + (itemIndex + 1) + "/" + total,
                        overall(itemIndex, total, p), false);
            }
            Thread.sleep(500);
        }

        if (error[0] != null) throw error[0];
        saveToMovies(temp, input);
        temp.delete();
    }

    private int overall(int index, int total, int current) {
        return Math.max(0, Math.min(100, (index * 100 + current) / Math.max(1, total)));
    }

    private void saveToMovies(File temp, Uri source) throws Exception {
        String display = "compressed_" + System.currentTimeMillis() + ".mp4";
        ContentValues values = new ContentValues();
        values.put(MediaStore.Video.Media.DISPLAY_NAME, display);
        values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
        if (Build.VERSION.SDK_INT >= 29) {
            values.put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/BatchVideoCompressor");
            values.put(MediaStore.Video.Media.IS_PENDING, 1);
        }
        Uri out = getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);
        if (out == null) throw new IllegalStateException("Could not create output file");

        try (FileInputStream in = new FileInputStream(temp);
             OutputStream os = getContentResolver().openOutputStream(out)) {
            if (os == null) throw new IllegalStateException("Could not open output");
            byte[] buf = new byte[1024 * 1024];
            int n;
            while ((n = in.read(buf)) != -1) os.write(buf, 0, n);
        }
        if (Build.VERSION.SDK_INT >= 29) {
            ContentValues done = new ContentValues();
            done.put(MediaStore.Video.Media.IS_PENDING, 0);
            getContentResolver().update(out, done, null, null);
        }
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel c = new NotificationChannel(CHANNEL, "Video compression",
                    NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(c);
        }
    }

    private Notification notification(String text, int progress, boolean done) {
        android.app.Notification.Builder b = Build.VERSION.SDK_INT >= 26
                ? new android.app.Notification.Builder(this, CHANNEL)
                : new android.app.Notification.Builder(this);
        b.setContentTitle("Batch Video Compressor")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.stat_sys_upload)
                .setOngoing(!done);
        if (!done) b.setProgress(100, progress, false);
        return b.build();
    }

    private void updateNotification(String text, int progress, boolean done) {
        getSystemService(NotificationManager.class).notify(NOTIFICATION_ID, notification(text, progress, done));
    }

    private String safeMessage(Exception e) {
        String s = e.getMessage();
        return s == null ? e.getClass().getSimpleName() : s;
    }

    @Override public void onDestroy() {
        if (transformer != null && stopping) transformer.cancel();
        executor.shutdownNow();
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
