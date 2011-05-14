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
import android.content.DialogInterface;
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

	static String ROM_VERSION = null;

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

		String VersionString = android.os.Build.ID;

		if (!VersionString.contains("OLYFR_U4_1.5.7")) {
			logHelper(LOGTAG, "***Motorola ROM 1.5.7 not detected ["
					+ VersionString + "] ***");
			new DialogHelper(this)
					.showActionConfirmation(
							"Version Alert",
							"You do no appear to be running ROM 1.5.7; you may experience problems! If you should have issues, uninsalling the modification via the menu option in this application should resolve your issue.\n\nAre you sure you want to continue?",
							"Yes", "No", null,
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									finish();

								}
							});
		}

		if (VersionString.contains("OLYFR_U4_1.5.7"))
			WebtopEnabler.ROM_VERSION = "1.5.7";

		if (VersionString.contains("OLYFR_U4_1.8.3"))
			WebtopEnabler.ROM_VERSION = "1.8.3";

		logHelper(LOGTAG, "\n\n\nStarting WebtopEnabler (ROM "
				+ VersionString
				+ "): "
				+ (DateFormat.format("EEEE, MMMM d, yyyy hh:mmaa ", new Date()
						.getTime())).toString());

		AssetsHelper.unzipAssets(this.getApplicationContext());

		this.Path = String.valueOf(this.getApplicationContext().getFilesDir()
				.getAbsolutePath());

		Button mod_webtop_button = (Button) this
				.findViewById(R.id.button_webtop_mod);
		Button reboot_button = (Button) this.findViewById(R.id.button_reboot);

		mod_webtop_button.setOnClickListener(new ModWebtopButtonListener());
		reboot_button.setOnClickListener(new RebootButtonListener());
	}

	// TODO: Properly handle onPause()
	/*
	 * protected void onPause() {
	 * 
	 * }
	 */

	public boolean verifyConnectivity() {
		ConnectivityManager connec = (ConnectivityManager) getSystemService(Activity.CONNECTIVITY_SERVICE);
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

					logHelper(LOGTAG,
							"Connection to http://ports.ubuntu.com successfull!");

				} else {

					logHelper(LOGTAG,
							"Connection to http://ports.ubuntu.com unsuccessfull!");

				}

			} catch (MalformedURLException e) {

				logHelper(LOGTAG, e.getMessage());
				return false;

			} catch (IOException e) {

				logHelper(LOGTAG, e.getMessage());
				return false;

			}

		} else {

			return false;
		}

		return true;
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

	class RebootButtonListener implements OnClickListener {
		public void onClick(View v) {

			new DialogHelper(WebtopEnabler.this).showActionConfirmation(
					"Reboot", "Are sure you want to reboot?",
					new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							new Reboot(WebtopEnabler.this, Path, "reboot")
									.execute();

						}
					});
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