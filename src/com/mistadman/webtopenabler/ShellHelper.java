package com.mistadman.webtopenabler;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.util.Log;

import com.mistadman.webtopenabler.WebtopEnabler.ShellHelperStdoutCallback;

public class ShellHelper {
	private static final String LOGTAG = ShellHelper.class.getSimpleName();

	private boolean DEBUG = true;
	private int bufferSize = 1 * 32;
	private ShellHelperStdoutCallback callBack;
	private SH shell;
	private Boolean can_su;
	private boolean MultiCommandMode = true;

	public boolean wasSuccessfull = false;
	public String stdout = null;
	public String stderr = null;
	public int exitCode = 1;

	public ShellHelper() {
		this("su", false, null);
	}

	public ShellHelper(String Shell) {
		this(Shell, false, null);
	}

	public ShellHelper(boolean RunMultipuleCommands) {
		this("su", RunMultipuleCommands, null);
	}

	public ShellHelper(ShellHelperStdoutCallback callBack) {
		this("su", false, callBack);
	}

	public ShellHelper(String Shell, boolean RunMultipuleCommands) {
		this(Shell, RunMultipuleCommands, null);
	}

	public ShellHelper(String Shell, boolean RunMultipuleCommands,
			ShellHelperStdoutCallback callBack) {
		canSU();
		shell = new SH(Shell);
		this.callBack = callBack;
	}

	public boolean canSU() {

		Integer exitValue = null;
		Process process = null;

		if (this.can_su == null) {

			try {
				process = Runtime.getRuntime().exec("su");

				DataOutputStream toProcess = new DataOutputStream(process
						.getOutputStream());
				toProcess.writeBytes("exec id\n");
				toProcess.flush();

				exitValue = process.waitFor();

			} catch (IOException e) {
				Log.e(LOGTAG, "Exception while trying to run su: "
						+ e.getMessage());

			} catch (InterruptedException e) {
				Log.e(LOGTAG, "Exception while trying to run su: "
						+ e.getMessage());
				process = null;
			}

			if (DEBUG)
				WebtopEnabler
						.logHelper(LOGTAG, "su installed: [" + exitValue + "]");

		}

		this.can_su = (exitValue != null && exitValue == 0);

		return this.can_su;
	}

	public class CommandResult {
		public final String stdout;
		public final String stderr;
		public final Integer exit_value;

		CommandResult(Integer exit_value_in, String stdout_in, String stderr_in) {
			exit_value = exit_value_in;
			stdout = stdout_in;
			stderr = stderr_in;
		}

		CommandResult(Integer exit_value_in) {
			this(exit_value_in, null, null);
		}

		public boolean success() {
			return exit_value != null && exit_value == 0;
		}
	}

	private class SH {
		private String SHELL;

		public SH(String SHELL_in) {
			SHELL = SHELL_in;
		}

		public Process run(String cmd, boolean streamStdout) {

			Process process = null;

			System.out.println(cmd);

			try {
				process = Runtime.getRuntime().exec(SHELL);

				DataOutputStream toProcess = new DataOutputStream(process
						.getOutputStream());

				if (MultiCommandMode)
					toProcess.writeBytes(cmd + " exit\n");
				else
					toProcess.writeBytes("exec " + cmd + "\n");

				toProcess.flush();

				if (streamStdout) {
					BufferedReader output = new BufferedReader(
							new InputStreamReader(process.getInputStream()),
							bufferSize);
					String line;
					while ((line = output.readLine()) != null) {
						try {
							callBack.StdoutCallback(line);

						} catch (Exception e) {
							Log.e(LOGTAG,
									"Callback requested, but not enabled for cmd: '"
											+ cmd + "'");
							break;

						}
					}
				}

			} catch (Exception e) {
				Log.e(LOGTAG, "Exception while trying to run: '" + cmd + "' "
						+ e.getMessage());
				process = null;
			}

			return process;
		}

		public CommandResult runWaitFor(String cmd, boolean streamStdout) {
			Process process = null;

			process = run(cmd, streamStdout);

			Integer exit_value = null;
			String stdout = null;
			String stderr = null;

			if (process != null) {
				try {
					exit_value = process.waitFor();

					if (streamStdout)
						stdout = null;
					else
						stdout = getStreamLines(process.getInputStream());

					stderr = getStreamLines(process.getErrorStream());

				} catch (InterruptedException e) {

					Log.e(LOGTAG, "runWaitFor " + e.toString());

				} catch (NullPointerException e) {

					Log.e(LOGTAG, "runWaitFor " + e.toString());
				}
			}

			return new CommandResult(exit_value, stdout, stderr);
		}
	}

	private String getStreamLines(InputStream is) {
		String out = null;
		StringBuffer buffer = null;
		DataInputStream dis = new DataInputStream(is);

		try {
			if (dis.available() > 0) {
				buffer = new StringBuffer(dis.readLine());
				while (dis.available() > 0)
					buffer.append("\n").append(dis.readLine());
			}

			dis.close();

		} catch (Exception ex) {
			Log.e(LOGTAG, "Error: " + ex.getMessage());

		}

		if (buffer != null)
			out = buffer.toString();

		return out;
	}

	public String RunCommand(String[] Commands, boolean streamStdout) {

		String Output = "";
		CommandResult r = null;
		StringBuilder Command = new StringBuilder();

		for (int i = 0; i < Commands.length; i++) {
			Command.append(Commands[i]);
			if ((Commands.length > 1) && MultiCommandMode)
				Command.append("; ");
		}

		WebtopEnabler.logHelper(LOGTAG, "\nStarting (" + shell.SHELL + ") '"
				+ Command.toString() + "'");

		if (can_su) {
			r = shell.runWaitFor(Command.toString(), streamStdout);

		} else {
			WebtopEnabler.logHelper(LOGTAG,
					"Error, su doesn't appear to be installed (su found?: "
							+ can_su + ")!");
		}

		if (r != null) {

			this.wasSuccessfull = r.success();
			this.stdout = r.stdout;
			this.stderr = r.stderr;

			if (r.exit_value != null)
				this.exitCode = r.exit_value;

			if (r.success()) {
				Output = r.stdout;

				if (Output != null) {
					if (DEBUG)
						WebtopEnabler.logHelper(LOGTAG,
								"Successfully executed command '" + Command
										+ "' - Exit code: " + r.exit_value
										+ " Stdout: " + Output);
				} else {
					if (streamStdout) {

						if (DEBUG)
							WebtopEnabler.logHelper(LOGTAG,
									"Successfully executed command '" + Command
											+ "' - Exit code: " + r.exit_value);
					} else {

						if (DEBUG)
							WebtopEnabler.logHelper(LOGTAG,
									"Successfully executed command '" + Command
											+ "' - Exit code: " + r.exit_value
											+ " - No stdout");
					}
				}

			} else {
				Output = r.stderr;

				if (DEBUG)
					WebtopEnabler.logHelper(LOGTAG, "Error executing command '"
							+ Command + "' - Exit code: [" + r.exit_value
							+ "] Result is: " + Output);
			}
		}

		return Output;
	}

	public String RunCommand(String Command, boolean streamStdout) {
		MultiCommandMode = false;
		return RunCommand(new String[] { Command }, streamStdout);

	}

	public String RunCommand(String Command) {
		MultiCommandMode = false;
		return RunCommand(new String[] { Command }, false);
	}
}