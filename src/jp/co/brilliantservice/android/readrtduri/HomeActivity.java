/*
 * Copyright 2012 yamashita@brilliantservice.co.jp
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jp.co.brilliantservice.android.readrtduri;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.TextUtils;
import android.view.Menu;
import android.widget.Toast;

/**
 * @author yamashita@brilliantservice.co.jp
 */
public class HomeActivity extends Activity {

    public static final String LOG_TAG = HomeActivity.class.getSimpleName();

    private NfcAdapter mNfcAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.home, menu);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();

        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (mNfcAdapter == null) {
            Toast.makeText(getApplicationContext(), "not found NFC feature", Toast.LENGTH_SHORT)
                    .show();
            finish();
            return;
        }

        if (!mNfcAdapter.isEnabled()) {
            Toast.makeText(getApplicationContext(), "NFC feature is not available",
                    Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this,
                getClass()), 0);
        IntentFilter[] intentFilter = new IntentFilter[] {
            new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED),
        };
        String[][] techList = new String[][] {
            {
                android.nfc.tech.Ndef.class.getName()
            }
        };
        mNfcAdapter.enableForegroundDispatch(this, pendingIntent, intentFilter, techList);

    }

    @Override
    public void onPause() {
        super.onPause();

        mNfcAdapter.disableForegroundDispatch(this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        String action = intent.getAction();
        if (TextUtils.isEmpty(action)) {
            Toast.makeText(getApplicationContext(), "null action", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!action.equals(NfcAdapter.ACTION_TECH_DISCOVERED))
            return;

        Parcelable[] messages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
        if (messages == null) {
            Toast.makeText(getApplicationContext(), "Null Message", Toast.LENGTH_SHORT).show();
            return;
        }

        if (messages.length == 0) {
            Toast.makeText(getApplicationContext(), "Empty Message", Toast.LENGTH_SHORT).show();
            return;
        }

        NdefRecord[] records = ((NdefMessage)messages[0]).getRecords();
        if (records == null || records.length == 0) {
            Toast.makeText(getApplicationContext(), "Empty Record", Toast.LENGTH_SHORT).show();
            return;
        }

        for (NdefRecord record : records) {
            if (isUriRecord(record)) {
                String uriString = getUri(record);

                String toastMessage = String.format("oepn '%s'", uriString);
                Toast.makeText(getApplicationContext(), toastMessage, Toast.LENGTH_LONG).show();

                Intent uriIntent = new Intent(Intent.ACTION_VIEW);
                uriIntent.addCategory(Intent.CATEGORY_DEFAULT);
                uriIntent.addCategory(Intent.CATEGORY_BROWSABLE);
                uriIntent.setData(Uri.parse(uriString));
                startActivity(uriIntent);
            } else {
                Toast.makeText(getApplicationContext(), "Not URI Record", Toast.LENGTH_SHORT)
                        .show();
            }
        }
    }

    /**
     * NdefRecordがRTDのUriか判定します
     * 
     * @param record
     * @return true:RTD Uri Recotd , false:not RTD Uri Recotd
     */
    private boolean isUriRecord(NdefRecord record) {
        return record.getTnf() == NdefRecord.TNF_WELL_KNOWN
                && Arrays.equals(record.getType(), NdefRecord.RTD_URI);
    }

    private static List<String> sProtocolList;
    static {
        sProtocolList = new ArrayList<String>();
        sProtocolList.add("");
        sProtocolList.add("http://www.");
        sProtocolList.add("https://www.");
        sProtocolList.add("http://");
        sProtocolList.add("https://");
        sProtocolList.add("tel:");
        sProtocolList.add("mailto:");
        sProtocolList.add("ftp://anonymous:anonymous@");
        sProtocolList.add("ftp://ftp.");
        sProtocolList.add("ftps://");
        sProtocolList.add("sftp://");
        sProtocolList.add("smb://");
        sProtocolList.add("nfs://");
        sProtocolList.add("ftp://");
        sProtocolList.add("dav://");
        sProtocolList.add("news:");
        sProtocolList.add("telnet://");
        sProtocolList.add("imap:");
        sProtocolList.add("rtsp://");
        sProtocolList.add("urn:");
        sProtocolList.add("pop:");
        sProtocolList.add("sip:");
        sProtocolList.add("sips:");
        sProtocolList.add("tftp:");
        sProtocolList.add("btspp://");
        sProtocolList.add("btl2cap://");
        sProtocolList.add("btgoep://");
        sProtocolList.add("tcpobex://");
        sProtocolList.add("irdaobex://");
        sProtocolList.add("file://");
        sProtocolList.add("urn:epc:id:");
        sProtocolList.add("urn:epc:tag:");
        sProtocolList.add("urn:epc:pat:");
        sProtocolList.add("urn:epc:raw:");
        sProtocolList.add("urn:epc:");
        sProtocolList.add("urn:nfc:");
    }

    /**
     * RTD URI RecordからURIを取得します
     * 
     * @param record
     * @return
     */
    private String getUri(NdefRecord record) {
        if (record == null)
            throw new IllegalArgumentException();

        byte[] payload = record.getPayload();
        int identifierCode = payload[0];
        String protocol;
        if (identifierCode < sProtocolList.size()) {
            protocol = sProtocolList.get(identifierCode);
        } else {
            protocol = "";
        }

        String uri;
        try {
            String uriField = new String(payload, 1, payload.length - 1, "UTF-8");
            uri = protocol + uriField;
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }

        return uri;
    }
}
