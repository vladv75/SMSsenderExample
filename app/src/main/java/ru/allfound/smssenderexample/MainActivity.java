package ru.allfound.smssenderexample;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private static final Uri SMS_INBOX = Uri.parse("content://sms/inbox");

    private EditText etPhone;
    private EditText etMessage;
    private ListView lvSMS;

    private SmsSent smsSent;

    final String DIR_SD = "SMSFiles";
    final String FILENAME_SD = "SMSfile.txt";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initUI();
    }

    private void initUI() {
        etPhone = (EditText) findViewById(R.id.etPhone);
        etMessage = (EditText) findViewById(R.id.etMessage);
        Button buttonSendSMS = (Button) findViewById(R.id.buttonSendSMS);
        assert buttonSendSMS != null;
        buttonSendSMS.setOnClickListener(this);
        Button buttonLoadSMS = (Button) findViewById(R.id.buttonLoadSMS);
        assert buttonLoadSMS != null;
        buttonLoadSMS.setOnClickListener(this);
        Button buttonSaveSMS = (Button) findViewById(R.id.buttonSaveSMS);
        assert buttonSaveSMS != null;
        buttonSaveSMS.setOnClickListener(this);
        lvSMS = (ListView) findViewById(R.id.lvSMS);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.buttonSendSMS:
                sendSMS();
                break;
            case R.id.buttonLoadSMS:
                loadSMS();
                break;
            case R.id.buttonSaveSMS:
                loadSMS();
                writeFileSD();
                break;
        }
    }

    void writeFileSD() {
        if (!Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED)) {
            Toast.makeText(this, "SD not available: " + Environment.getExternalStorageState(),
                    Toast.LENGTH_SHORT).show();
            return;
        }
        File sdPath = Environment.getExternalStorageDirectory();
        sdPath = new File(sdPath.getAbsolutePath() + "/" + DIR_SD);
        sdPath.mkdirs();
        File sdFile = new File(sdPath, FILENAME_SD);
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(sdFile));

            Uri uri = Uri.parse("content://sms/");
            Cursor c = getContentResolver().query(uri, null, null, null, null);
            assert c != null;
            int count = 0;
            StringBuilder stringBuilder = new StringBuilder();
            while (c.moveToNext()){
                stringBuilder.append("SMS: ").append(count).append(", Number: ")
                        .append(c.getString(c.getColumnIndex("address")))
                        .append("\n").append(c.getString(c.getColumnIndex("body"))).append("\n\n");
                count++;
            }
            bw.write(stringBuilder.toString());

            bw.close();
            Toast.makeText(this, "File is recorded on the SD: " + sdFile.getAbsolutePath(),
                    Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadSMS() {
        Cursor c = this.getContentResolver().query(SMS_INBOX,
                null, null, null, null);
        String[] from = new String[] {"address", "body"};
        int[] to = new int[] {R.id.tvAddress, R.id.tvBody};
        SimpleCursorAdapter lvAdapter = new SimpleCursorAdapter(this, R.layout.list_item, c, from,
                to, 0);
        lvSMS.setAdapter(lvAdapter);
    }

    private void sendSMS() {
        String phone = etPhone.getText().toString();
        String message = etMessage.getText().toString();

        if (phone.isEmpty() || message.isEmpty()) {
            Toast.makeText(this, "Please fill data...", Toast.LENGTH_SHORT).show();
            return;
        }

        SmsManager smsManager = SmsManager.getDefault();
        PendingIntent p = PendingIntent.getBroadcast(this, 0, new Intent(SmsSent.ACTION),
                PendingIntent.FLAG_ONE_SHOT);
        smsManager.sendTextMessage(phone, null, message, p, null);
    }

    @Override
    protected void onResume() {
        super.onResume();
        smsSent = new SmsSent();
        registerReceiver(smsSent, new IntentFilter(SmsSent.ACTION));
    }

    @Override
    protected void onPause() {
        if (smsSent != null) {
            unregisterReceiver(smsSent);
        }
        super.onPause();
    }

    private class SmsSent extends BroadcastReceiver {

        public final static String ACTION = "com.example.SMS_SENT";

        @Override
        public void onReceive(Context context, Intent intent) {

            switch (getResultCode()) {

                case Activity.RESULT_OK:
                    Toast.makeText(context, "SMS sent successfully",
                            Toast.LENGTH_SHORT).show();
                    break;
                case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                    Toast.makeText(context, "Generic failure cause",
                            Toast.LENGTH_SHORT).show();
                    break;
                case SmsManager.RESULT_ERROR_NO_SERVICE:
                    Toast.makeText(context, "Service is currently unavailable",
                            Toast.LENGTH_SHORT).show();
                    break;
                case SmsManager.RESULT_ERROR_NULL_PDU:
                    Toast.makeText(context, "No pdu provided",
                            Toast.LENGTH_SHORT).show();
                    break;
                case SmsManager.RESULT_ERROR_RADIO_OFF:
                    Toast.makeText(context, "Radio was explicitly turned off",
                            Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    }
}
