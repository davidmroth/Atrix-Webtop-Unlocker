package com.mistadman.webtopenabler;

import android.os.AsyncTask;

public class Reboot extends AsyncTask<Boolean, Integer, Boolean> {

	private DialogHelper Dialog;
	private ShellHelper Task;
	private WebtopEnabler Context;

	private String Path;
	private String[] Cmd;

	private String AlertTitle = "Error!";
	private String AlertMessage;
	private String ProgressMessage;

	public Reboot(WebtopEnabler Context, String Path, String Cmd) {
		this(Context, Path, new String[] {Cmd});
	}

	public Reboot(WebtopEnabler Context, String Path, String[] Cmd) {
		this.Path = Path;
		this.Context = Context;
		this.Dialog = new DialogHelper(this.Context);

		this.Task = new ShellHelper(true);
		this.Cmd = Cmd;
	}

	public Reboot(WebtopEnabler Context, String Path, String Shell, String[] Cmd) {
		this.Path = Path;
		this.Context = Context;
		this.Dialog = new DialogHelper(this.Context);

		this.Task = new ShellHelper(Shell, true);
		this.Cmd = Cmd;
	}

	@Override
	protected void onPreExecute() {
		Dialog.showProgress("Rebooting");
	}

	protected void onProgressUpdate(Integer... values) {
		super.onProgressUpdate(values);
		this.Dialog.showProgress(this.ProgressMessage, values[0]);
	}

	@Override
	protected Boolean doInBackground(Boolean... params) {
		String busybox = this.Path + "/busybox ";

		this.Task.RunCommand(this.Cmd, false);
		if (!this.Task.wasSuccessfull) {
			this.Task.RunCommand(busybox + "rm -r " + Path + "/tmp", false);
			this.AlertMessage = "There was an error rebooting the device.\n\nPlease manually reboot!";
			return false;
		}

		return true;
	}

	@Override
	protected void onPostExecute(Boolean result) {
		if (result)
			this.Dialog.showAlert(this.AlertTitle, this.AlertMessage);
		
		this.Dialog = null;
		this.Task = null;
	}
}