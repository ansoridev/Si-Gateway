package pro.ansori.sig;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.preference.PreferenceManager;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class SMSReceiver extends BroadcastReceiver {
    final SmsManager sms = SmsManager.getDefault();
    OkHttpClient client;
    WebSocket ws;
    SharedPreferences sp;
    String phoneNumber = "";
    String message = "";
    NotificationManager nmPush;
    PendingIntent pendingIntent;

    @Override
    public void onReceive(Context context, Intent intent) {
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addNextIntent(new Intent(context, SettingsActivity.class));
        pendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = context.getString(R.string.app_name);
            String description = context.getString(R.string.notification_text);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel("pro.ansori.si.coming", name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this

            NotificationChannel channel_push = new NotificationChannel("pro.ansori.si.coming", name, importance);
            channel_push.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            nmPush = context.getSystemService(NotificationManager.class);
            nmPush.createNotificationChannel(channel_push);
        }

        sp = PreferenceManager.getDefaultSharedPreferences(context);
        String url = sp.getString("incoming_ws", "ws://localhost");
        String authWS = "{\"key\": \"" + sp.getString("key_ws", "KEYKU") + "\"}";
        client = new OkHttpClient();
        Request req = new Request.Builder()
                .url(url)
                .build();

        // Retrieves a map of extended data from the intent.
        final Bundle bundle = intent.getExtras();
        final class IncomingListener extends WebSocketListener {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                webSocket.send(authWS);
                Log.d("IncomingSMS", "Incoming WebSockets onOpen");
                String msg = "{ \"type\": \"incoming\", \"data\": { \"from_number\": \"" + phoneNumber + "\", \"text\": \"" + message + "\" } }";
                webSocket.send(msg);
                Log.d("IncomingSMS", "SMS from " +  phoneNumber + " has been sent to server");

                NotificationCompat.Builder buildPush = new NotificationCompat.Builder(context, "pro.ansori.si.coming")
                        .setSmallIcon(R.drawable.ic_launcher_foreground)
                        .setContentTitle("SMS Incoming sent!")
                        .setContentText("SMS from " +  phoneNumber + " has been sent to server")
                        .setContentIntent(pendingIntent);

                nmPush.notify(2006, buildPush.build());
            }

            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {
                Log.d("IncomingSMS", "Incoming WebSockets onClosing");
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                Log.d("IncomingSMS", "Incoming WebSockets onClosed");
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                Log.d("IncomingSMS", "Incoming WebSockets onFailure at: " + t.getMessage());
            }
        }

        try {

            if (bundle != null) {

                final Object[] pdusObj = (Object[]) bundle.get("pdus");

                for (int i = 0; i < pdusObj.length; i++) {

                    SmsMessage currentMessage = SmsMessage.createFromPdu((byte[]) pdusObj[i]);
                    phoneNumber = currentMessage.getDisplayOriginatingAddress();
                    message = currentMessage.getDisplayMessageBody();

                    IncomingListener incomingListener = new IncomingListener();
                    ws = client.newWebSocket(req, incomingListener);
                    Log.d("IncomingSMS", "SMS Coming from " + phoneNumber);

                } // end for loop
            } // bundle is null

        } catch (Exception e) {
            Log.e("SmsReceiver", "Exception smsReceiver" +e);

        }
    }
}