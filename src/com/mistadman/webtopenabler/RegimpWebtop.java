package com.mistadman.webtopenabler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.AsyncTask;

public class RegimpWebtop extends AsyncTask<Boolean, Integer, Boolean> {

	private final String LOGTAG = this.getClass().getSimpleName();

	private DialogHelper Dialog;
	private ShellHelper Task;
	private WebtopEnabler Activity;

	private final int bufferSize = 1 * 1024;

	private String Path;
	private String AlertTitle = "Error!";
	private String AlertMessage;
	private String ProgressMessage;

	public RegimpWebtop(WebtopEnabler Activity, String Path) {
		this.Path = Path;
		this.Activity = Activity;
		this.Dialog = new DialogHelper(this.Activity);
	}

	@Override
	protected void onPreExecute() {
		Dialog.showProgress("Removing Webtop modification");
	}

	@Override
	protected void onProgressUpdate(Integer... values) {
		super.onProgressUpdate(values);
		this.Dialog.showProgress(this.ProgressMessage, values[0]);
	}

	@Override
	protected Boolean doInBackground(Boolean... params) {
		String busybox = Path + "/busybox ";
		this.Task = new ShellHelper();
		String output = null;

		this.AlertMessage = "There was an error uninstalling Webtop modifications!";
		java.io.File webtopFile = new java.io.File(WebtopEnabler.drivefile);
		java.io.File mountshFile = new java.io.File("/system/bin/mountosh.orig");

		if (webtopFile.exists() || mountshFile.exists()) {
			this.Task.RunCommand(busybox + "rm " + WebtopEnabler.drivefile);

			this.Task.RunCommand(busybox + "mount");
			if ((output = this.Task.stdout) != null) {
				WebtopEnabler.logHelper(LOGTAG, output);

				String str;
				BufferedReader reader = new BufferedReader(new StringReader(
						output), bufferSize);

				try {
					while ((str = reader.readLine()) != null) {

						if (str.contains("/dev/block/mmcblk0p12")) {
							if (str.contains("ro")) {
								this.Task.RunCommand(busybox
										+ "mount -o rw,remount /system");
								if (!this.Task.wasSuccessfull) {
									return false;
								} else {

									this.Task
											.RunCommand(busybox
													+ "md5sum /system/bin/mountosh.orig");
									if ((output = this.Task.stdout) != null) {

										String[] md5sum = output.split(" ");
										WebtopEnabler.logHelper(LOGTAG,
												md5sum[0].trim());
										if (md5sum[0].trim().equals(
												WebtopEnabler.mountosh_md5)) {

											this.Task
													.RunCommand("mv /system/bin/mountosh.orig /system/bin/mountosh");
											if (!this.Task.wasSuccessfull) {
												return false;
											}
										}
									}
								}
							}
						}
					}

				} catch (IOException e) {
					return false;
				}

			} else {
				return false;
			}

		} else {
			AlertMessage = "Sorry, Webtop modification does not seem to be installed. No changes made!";
			return false;
		}

		AlertTitle = "Successful!";
		AlertMessage = "Webtop modifications successfully uninstalled!\n\nDo you want to reboot?";
		return true;
	}

	@Override
	protected void onPostExecute(Boolean result) {
		this.cancel(true);

		if (result)
			this.Dialog.showActionConfirmation(this.AlertTitle,
					this.AlertMessage, this.RebootConfirmation());
		else
			this.Dialog.showAlert(this.AlertTitle, this.AlertMessage);
		
		this.Dialog = null;
		this.Task = null;
	}

	protected OnClickListener RebootConfirmation() {
		return new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				String[] RebootCmd = new String[] { "sync", "sync", "reboot" };
				new Reboot(Activity, Path, RebootCmd).execute();
			}
		};
	}
}