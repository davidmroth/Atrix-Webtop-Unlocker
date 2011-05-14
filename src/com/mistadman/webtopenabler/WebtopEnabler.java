package com.mistadman.webtopenabler;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class WebtopEnabler extends Activity {

	private static final String LOGTAG = "Main";

	Handler progressHandler = new Handler();

	private static int bufferSize = 1 * 32;
	private static File SDcard = Environment.getExternalStorageDirectory();

	private String Path = null;

	static boolean NETWORK_ACTIVE = false;

	static final String mountosh_md5 = "12deaf61043441bd9656b1d47d5ded61";
	static final String aptsourcesnew_md5 = "27354e668917d7378eb178ae3d5eca62";
	static final int drivefilesize = 1024;
	static final String drivefile = "/data/ubuntu.disk";

	interface ShellHelperStdoutCallback {
		void StdoutCallback(String text);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		logHelper(LOGTAG, "\n\n\nStarting WebtopEnabler: "
				+ (DateFormat.format("EEEE, MMMM d, yyyy hh:mmaa ", new Date()
						.getTime())).toString());

		AssetsHelper.unzipAssets(this.getApplicationContext());

		this.Path = String.valueOf(this.getApplicationContext().getFilesDir()
				.getAbsolutePath());

		Button mod_webtop_button = (Button) this
				.findViewById(R.id.button_webtop_mod);

		mod_webtop_button.setOnClickListener(new ModWebtopButtonListener());
	}

	// TODO: Properly handle onPause()
	/*
	 * protected void onPause() {
	 * 
	 * }
	 */

	public boolean verifyConnectivity() {

		ConnectivityManager connec = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

		if (connec.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState() == NetworkInfo.State.CONNECTED
				|| connec.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
						.getState() == NetworkInfo.State.CONNECTED) {
			try {

				URL url = new URL("http://ports.ubuntu.com");

				HttpURLConnection urlc = (HttpURLConnection) url
						.openConnection();
				urlc.setRequestProperty("User-Agent", "Android Application");
				urlc.setRequestProperty("Connection", "close");
				urlc.setConnectTimeout(1000 * 30); // mTimeout is in seconds
				urlc.connect();

				if (urlc.getResponseCode() == 200) {

					NETWORK_ACTIVE = true;
					logHelper(LOGTAG,
							"Connection to http://ports.ubuntu.com successfull (Network Acitve: "
									+ NETWORK_ACTIVE + ")");

				} else {

					NETWORK_ACTIVE = false;
					logHelper(LOGTAG,
							"Connection to http://ports.ubuntu.com unsuccessfull (Network Acitve: "
									+ NETWORK_ACTIVE + ")");

				}

			} catch (MalformedURLException e) {

				NETWORK_ACTIVE = false;
				logHelper(LOGTAG, e.getMessage());

			} catch (IOException e) {

				NETWORK_ACTIVE = false;
				logHelper(LOGTAG, e.getMessage());

			}

		} else {

			NETWORK_ACTIVE = false;
		}

		return NETWORK_ACTIVE;
	}

	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {

		case R.id.menu_uninstall_webtop_mod:
			new RegimpWebtop(WebtopEnabler.this, Path).execute();
			return true;

		default:
			return super.onOptionsItemSelected(item);
		}
	}

	class ModWebtopButtonListener implements OnClickListener {

		public void onClick(View v) {
			new UngimpWebtop(WebtopEnabler.this, Path).execute();
		}
	}

	public static void logHelper(final String logtag, final String msg) {

		if (SDcard.canWrite()) {

			File logfile = new File(SDcard, "WebtopEnabler.log");

			try {
				FileWriter logwriter = new FileWriter(logfile, true);
				BufferedWriter out = new BufferedWriter(logwriter, bufferSize);
				out.write(msg + "\n");
				out.close();
				Log.d("WebtopEnabler", logtag + ": " + msg);

			} catch (IOException e) {

				Log.d("WebtopEnabler", e.getMessage());
			}
		}
	}
}