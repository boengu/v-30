package com.example.phonehud;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/** Local-only foreground HTTP status server. No external API, accounts or remote logging. */
public final class HudSenderService extends Service {
    public static final int PORT = 8765;
    private static final String CHANNEL = "hud_link_sender";
    public static volatile boolean serviceActive = false;
    public static volatile boolean listening = false;
    public static volatile String serverError = "";
    private volatile boolean running;
    private volatile ServerSocket listener;
    private ExecutorService pool;
    private Thread acceptThread;

    @Override public void onCreate() {
        super.onCreate();
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(new NotificationChannel(CHANNEL, "HUD LINK 송신", NotificationManager.IMPORTANCE_LOW));
        }
        Intent screen = new Intent(this, MainActivity.class);
        PendingIntent open = PendingIntent.getActivity(this, 0, screen,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        // Android 7.1(API 25) has no notification channels or two-argument Builder.
        Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CHANNEL) : new Notification.Builder(this);
        Notification notice = builder
                .setContentTitle("HUD LINK 연결 사용 중")
                .setContentText("같은 Wi-Fi의 HUD 기기에 길안내 정보를 전달합니다")
                .setSmallIcon(android.R.drawable.ic_menu_directions)
                .setContentIntent(open).setOngoing(true).build();
        if (Build.VERSION.SDK_INT >= 34) startForeground(106, notice, ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE);
        else startForeground(106, notice);
        serviceActive = true;
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && "STOP".equals(intent.getAction())) { stopSelf(); return START_NOT_STICKY; }
        if (!running) startServer();
        return START_STICKY;
    }

    private synchronized void startServer() {
        if (running) return;
        running = true;
        listening = false;
        serverError = "";
        pool = new ThreadPoolExecutor(2, 3, 20L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(12), new ThreadPoolExecutor.AbortPolicy());
        acceptThread = new Thread(() -> {
            try {
                ServerSocket server = new ServerSocket();
                listener = server;
                server.setReuseAddress(true);
                server.bind(new InetSocketAddress(PORT));
                listening = true;
                while (running) {
                    Socket client = server.accept();
                    try { pool.execute(() -> handle(client)); }
                    catch (Exception e) { try { client.close(); } catch (Exception ignored) { } }
                }
            } catch (Exception e) {
                if (running) serverError = "포트 열기 실패: " + e.getClass().getSimpleName();
            } finally {
                listening = false;
                running = false;
            }
        }, "HudLinkLocalServer");
        acceptThread.start();
    }

    private void handle(Socket socket) {
        try (Socket s = socket) {
            s.setSoTimeout(2500);
            if (!isLocalIpv4(s.getInetAddress())) return;
            BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
            String first = in.readLine();
            if (first == null || first.length() > 48) return;
            String suppliedPin = "";
            for (int lineNo = 0; lineNo < 16; lineNo++) {
                String line = in.readLine();
                if (line == null || line.isEmpty()) break;
                if (line.length() > 512) return;
                if (line.regionMatches(true, 0, "X-HUD-PIN: ", 0, 11)) suppliedPin = line.substring(11).trim();
            }
            String currentPin = getSharedPreferences("hud", MODE_PRIVATE).getString("pin", "");
            boolean ok = "GET /status HTTP/1.1".equals(first)
                    && currentPin != null && currentPin.matches("[0-9]{8}")
                    && secureEqual(currentPin, suppliedPin);
            JSONObject out = new JSONObject();
            if (ok) {
                out = new JSONObject(HudWire.encode(
                        NavStore.get(), NavStore.getCamera(), System.currentTimeMillis(),
                        NavNotificationService.listenerConnected, NavNotificationService.lastNaverNotificationAt));
            } else {
                out.put("error", "PIN_MISMATCH");
            }
            byte[] payload = out.toString().getBytes(StandardCharsets.UTF_8);
            String header = "HTTP/1.1 " + (ok ? "200 OK" : "403 Forbidden") + "\r\n"
                    + "Content-Type: application/json; charset=utf-8\r\n"
                    + "Cache-Control: no-store\r\nContent-Length: " + payload.length
                    + "\r\nConnection: close\r\n\r\n";
            OutputStream stream = s.getOutputStream();
            stream.write(header.getBytes(StandardCharsets.UTF_8)); stream.write(payload); stream.flush();
            if (!ok) Thread.sleep(150); // Small cost against online guessing on a private LAN.
        } catch (Exception ignored) { }
    }
    private static boolean secureEqual(String a, String b) {
        byte[] x = a.getBytes(StandardCharsets.UTF_8), y = b.getBytes(StandardCharsets.UTF_8);
        int diff = x.length ^ y.length;
        for (int i = 0; i < Math.max(x.length, y.length); i++)
            diff |= (i < x.length ? x[i] : 0) ^ (i < y.length ? y[i] : 0);
        return diff == 0;
    }
    private static boolean isLocalIpv4(InetAddress addr) {
        byte[] v = addr.getAddress();
        if (v.length != 4) return false;
        int a = v[0] & 255, b = v[1] & 255;
        return a == 10 || (a == 172 && b >= 16 && b <= 31)
                || (a == 192 && b == 168) || (a == 169 && b == 254)
                || (a == 100 && b >= 64 && b <= 127) || a == 127;
    }
    @Override public void onDestroy() {
        serviceActive = false; running = false; listening = false;
        try { if (listener != null) listener.close(); } catch (Exception ignored) { }
        if (pool != null) pool.shutdownNow();
        super.onDestroy();
    }
    @Override public IBinder onBind(Intent intent) { return null; }
}
