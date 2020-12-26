package pro.ansori.sig;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import android.telephony.SmsManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.preference.PreferenceManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class SMSGateway extends Service {
    private static final String CHANNEL_DEFAULT_IMPORTANCE = "pro.ansori.sig";
    private static final String CHANNEL_PUSH = "pro.ansori.sig.push";
    private static final int ONGOING_NOTIFICATION_ID = 2003;
    private static final int CHANNEL_PUSH_ID = 2004;

    Context context;
    SharedPreferences sp;
    String authClient;
    OkHttpClient client;
    WebSocket ws;
    JSONObject dataOutgoing;
    NotificationManager notificationManager;
    NotificationManager nMPush;
    PendingIntent pendingIntent;
    PendingIntent pushPI;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String url = sp.getString("outgoing_ws", "ws://localhost/");
        Request request = new Request.Builder()
                .url(url)
                .build();
        final class OutGoingListener extends WebSocketListener {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                webSocket.send(authClient);
                Log.d("WebSockets", "WebSockets onOpen");
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                Log.d("WebSockets", "Receive Message " + text);
                try {
                    dataOutgoing = new JSONObject(text);
                    if (dataOutgoing.has("type")) {
                        if (dataOutgoing.getString("type").equals("outgoing")) {
                            JSONObject dataNya = dataOutgoing.getJSONObject("data");

                            final ArrayList<Integer> simCardList = new ArrayList<>();
                            SubscriptionManager subscriptionManager;
                            subscriptionManager = SubscriptionManager.from(context);

                            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                                // TODO: Consider calling
                                //    ActivityCompat#requestPermissions
                                // here to request the missing permissions, and then overriding
                                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                //                                          int[] grantResults)
                                // to handle the case where the user grants the permission. See the documentation
                                // for ActivityCompat#requestPermissions for more details.
                                Log.d("SMSOutgoing", "READ PHONE STATE not permitted");
                                return;
                            }

                            final List<SubscriptionInfo> subscriptionInfoList = subscriptionManager
                                    .getActiveSubscriptionInfoList();
                            for (SubscriptionInfo subscriptionInfo : subscriptionInfoList) {
                                int subscriptionId = subscriptionInfo.getSubscriptionId();
                                simCardList.add(subscriptionId);
                            }

                            SmsManager sms = SmsManager.getSmsManagerForSubscriptionId(simCardList.get(0));
                            sms.sendTextMessage(dataNya.getString("to_number"), null, dataNya.getString("text"), null, null);
                            Log.d("SMSOutgoing", "SMS to " + dataNya.getString("to_number") + " has been sent.");

                            NotificationCompat.Builder buildPush = new NotificationCompat.Builder(context, CHANNEL_PUSH)
                                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                                    .setContentTitle("SMS Outgoing sent!")
                                    .setContentText("SMS to " + dataNya.getString("to_number") + " has been sent.")
                                    .setContentIntent(pushPI);

                            nMPush.notify(CHANNEL_PUSH_ID, buildPush.build());
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {
                Log.d("WebSockets", "WebSockets onClosing");
                client.connectionPool().evictAll();
                ws = client.newWebSocket(request, this);
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                Log.d("WebSockets", "WebSockets onClosed");
                client.connectionPool().evictAll();
                ws = client.newWebSocket(request, this);
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                Log.d("WebSockets", "WebSockets onFailure at: " + t.getMessage());
                client.connectionPool().evictAll();
                ws = client.newWebSocket(request, this);
            }
        }
        OutGoingListener outGoingListener = new OutGoingListener();
        client = new OkHttpClient();
        ws = client.newWebSocket(request, outGoingListener);
        return START_STICKY;
    }

    @Override

    public void onCreate() {
        super.onCreate();
        this.context = this;
        this.sp = PreferenceManager.getDefaultSharedPreferences(this);
        this.authClient = "{\"key\": \"" + sp.getString("key_ws", "KEYKU") + "\"}";
        Toast.makeText(this, "Service onCreate Started", Toast.LENGTH_LONG).show();

        Intent notificationIntent = new Intent(this, SettingsActivity.class);
        pendingIntent =
                PendingIntent.getActivity(this, 0, notificationIntent, 0);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addNextIntent(new Intent(this, SettingsActivity.class));
        pushPI = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.app_name);
            String description = getString(R.string.notification_text);
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel(CHANNEL_DEFAULT_IMPORTANCE, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);

            NotificationChannel channel_push = new NotificationChannel(CHANNEL_PUSH, name, importance);
            channel_push.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            nMPush = getSystemService(NotificationManager.class);
            nMPush.createNotificationChannel(channel_push);
        }

        Notification notification =
                new Notification.Builder(this, CHANNEL_DEFAULT_IMPORTANCE)
                        .setContentTitle(getText(R.string.notification_message))
                        .setContentText(getText(R.string.notification_text))
                        .setSmallIcon(R.drawable.ic_launcher_foreground)
                        .setContentIntent(pendingIntent)
                        .build();

        startForeground(ONGOING_NOTIFICATION_ID, notification);

        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}