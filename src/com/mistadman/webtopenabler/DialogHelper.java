package com.mistadman.webtopenabler;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;

public class DialogHelper extends Dialog {

	private AlertDialog.Builder Alertbuilder;
	private ProgressDialog progressDialog;

	public boolean PBAR_ISHORIZPNTAL = false;

	protected DialogHelper(Context context) {
		super(context, false, null);

		this.Alertbuilder = new AlertDialog.Builder(context);
		this.progressDialog = new ProgressDialog(context);
		this.progressDialog.setCancelable(false);
	}

	public void showProgress(String Message) {
		if (!this.progressDialog.isShowing())
			this.progressDialog.show();

		this.progressDialog.setMessage(Message + "...");
	}

	public void showProgress(String Message, int Value) {

		if (Value != -1 && !this.PBAR_ISHORIZPNTAL) {

			if (this.progressDialog.isShowing())
				this.progressDialog.dismiss();

			this.progressDialog.setProgress(0);
			this.progressDialog
					.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			this.progressDialog.show();
			this.PBAR_ISHORIZPNTAL = true;

		} else if (Value == -1 && this.PBAR_ISHORIZPNTAL == true) {

			if (this.progressDialog.isShowing())
				this.progressDialog.dismiss();

			this.progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			this.progressDialog.show();
			this.PBAR_ISHORIZPNTAL = false;
		}

		//if (Value != -1)
			//this.progressDialog.setProgress(Value);

		if (!this.progressDialog.isShowing())
			this.progressDialog.show();

		this.progressDialog.setMessage(Message + "...");
	}

	public void setMaxValue(int Value) {
		this.progressDialog.setMax(Value);
	}

	void dismissProgressDialog() {
		if (this.progressDialog.isShowing())
			this.progressDialog.dismiss();
	}

	public void showAlert(String Title, String Message) {

		if (this.progressDialog.isShowing())
			this.progressDialog.dismiss();

		this.Alertbuilder.setTitle(Title);
		this.Alertbuilder.setMessage(Message);
		this.Alertbuilder.setPositiveButton("Ok",
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				});
		this.Alertbuilder.create().show();
	}

	public void showActionConfirmation(String Title, String Message,
			DialogInterface.OnClickListener clickListener) {

		if (this.progressDialog.isShowing())
			this.progressDialog.dismiss();

		this.Alertbuilder.setCancelable(false);
		this.Alertbuilder.setTitle(Title);
		this.Alertbuilder.setMessage(Message);
		this.Alertbuilder.setPositiveButton("Yes", clickListener);
		this.Alertbuilder.setNegativeButton("No",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.dismiss();
					}
				});

		this.Alertbuilder.create().show();
	}
}