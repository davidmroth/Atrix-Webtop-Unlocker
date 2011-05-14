package com.mistadman.webtopenabler;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;

import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.AsyncTask;
import android.os.StatFs;

import com.mistadman.webtopenabler.WebtopEnabler.ShellHelperStdoutCallback;

public class UngimpWebtop extends AsyncTask<Boolean, Integer, Boolean>
		implements ShellHelperStdoutCallback {

	private final String LOGTAG = this.getClass().getSimpleName();

	private DialogHelper Dialog;
	private ShellHelper Task;
	private WebtopEnabler Activity;

	private final int bufferSize = 1 * 1024;
	private final String Path;

	private String AlertTitle = "Error!";
	private String AlertMessage;
	private String ProgressMessage;

	private int RebootType = 0;

	public UngimpWebtop(WebtopEnabler Activity, String Path) {
		this.Path = Path;
		this.Activity = Activity;
		this.Dialog = new DialogHelper(this.Activity);
	}

	@Override
	protected void onPreExecute() {
		Dialog.showProgress("Initializing");
	}

	protected void onProgressUpdate(Integer... values) {
		super.onProgressUpdate(values);
		this.Dialog.showProgress(this.ProgressMessage, values[0]);
	}

	@Override
	protected Boolean doInBackground(Boolean... params) {
		this.Task = new ShellHelper(this);
		String busybox = this.Path + "/busybox ";

		this.ProgressMessage = "Verifying network connectivity";
		this.AlertMessage = "No active network connection detected. Please enable 3G or WiFi!";
		publishProgress(-1);

		if (!Activity.verifyConnectivity()) {
			return false;
		}

		if (!isAlreadyModded(busybox)) {

			// TODO: Add better cleanup function
			cleanup(busybox);

			// Create and modify filesystem
			if (setupFileSystem(busybox)) {

				// Install wget, rync, and the replicate the /osh directory
				if (!InstallWget())
					return false;
				if (!InstallRysnc())
					return false;
				if (!ReplicateOsh())
					return false;

				// Modify sudoers and tomoyo
				if (!setupSudoers(busybox))
					return false;
				if (!ReplaceExceptionPolicy(busybox))
					return false;

				// Install apps
				if (!InstallLXTerminal(busybox))
					return false;
				if (!InstallCoreUtils(busybox))
					return false;
				if (!InstallCPIO(busybox))
					return false;
				if (!InstallDbus(busybox))
					return false;
				if (!InstallDbusX11(busybox))
					return false;
				if (!InstallDhcp3Client(busybox))
					return false;
				if (!InstallFindUtils(busybox))
					return false;
				if (!InstallGpgv(busybox))
					return false;
				if (!InstallLogrotate(busybox))
					return false;
				if (!InstallPulseAudio(busybox))
					return false;
				if (!InstallUdev(busybox))
					return false;
				if (!InstallXorgCore(busybox))
					return false;

				// Check and modify system packages
				if (!SetupAPTSourcesList(busybox))
					return false;
				if (!UpdateAptitudeConfiguration(busybox))
					return false;

				if (CheckBuiltinPackages(busybox)) {
					// Install Logrotate and GKSU
					if (!ReinstallLogrotate(busybox))
						return false;
					if (!InstallGksu(busybox))
						return false;
					if (!InstallAWN(busybox))
						return false;

				} else {

					return false;

				}

				if (!InstallMountosh(busybox))
					return false;

				this.AlertTitle = "Successful!";
				this.AlertMessage = "Webtop modification successfully installed!\n\n Do you want to reboot?";
				return true;
			}

		} else {

			return true;
		}

		return false;
	}

	/*
	 * 
	 * 
	 * 
	 * 
	 * Main Functions
	 */

	/**
	 * @param busybox
	 * @param mountpoint
	 * @return
	 */
	private boolean verifyMount(String busybox, String mountpoint) {
		String output = null;

		this.AlertMessage = "Unable to verify mount" + mountpoint;

		String cmd = busybox + "mount";
		this.Task.RunCommand(cmd);
		if ((output = this.Task.stdout) != null) {

			String str;
			BufferedReader reader = new BufferedReader(
					new StringReader(output), this.bufferSize);

			try {
				while ((str = reader.readLine()) != null) {

					if (str.contains(mountpoint)) {
						WebtopEnabler.logHelper(LOGTAG, "Mountpoint found: "
								+ str);
						return true;
					}
				}

			} catch (IOException e) {
				return false;
			}
		}

		return false;
	}

	/**
	 * @param busybox
	 * @return
	 */
	private boolean isAlreadyModded(String busybox) {
		String output = null;
		File mountosh_orig = new File("/system/bin/mountosh.orig");

		this.AlertMessage = "Unable to verify previous modification!";

		if (mountosh_orig.exists()) {
			String cmd = busybox + "mount";
			this.Task.RunCommand(cmd);
			if ((output = this.Task.stdout) != null) {

				String str;
				BufferedReader reader = new BufferedReader(new StringReader(
						output), this.bufferSize);

				try {
					while ((str = reader.readLine()) != null) {

						if (str.contains("/dev/block/loop7 on /osh")) {
							WebtopEnabler.logHelper(LOGTAG,
									"Mountpoint found: " + str);
							RebootType = 1;
							this.AlertTitle = "Uninstall?";
							this.AlertMessage = "Webtop modification already installed. You will need to uninstall before any modifications can be done.\n\nDo you want to uninstall?";
							return true;
						}
					}

				} catch (IOException e) {
					return false;
				}
			}
		}

		return false;
	}

	/**
	 * @param busybox
	 * @return
	 */
	private boolean cleanup(String busybox) {
		String cmd = null;
		int giveUp = 0;
		int retryLimit = 2;

		if (verifyMount(busybox, "/dev/block/loop7")) {

			ProgressMessage = "Performing clean up";
			publishProgress(-1);

			cmd = busybox + "umount -f /dev/block/loop7";
			this.Task.RunCommand(cmd);
			while (!this.Task.wasSuccessfull) {
				if (giveUp > retryLimit)
					return false;

				this.Task.RunCommand(cmd);
				this.sleep(1);
				giveUp++;
			}

			giveUp = 0;
			cmd = busybox + "losetup -d /dev/block/loop7";
			this.Task.RunCommand(cmd);
			while (!this.Task.wasSuccessfull) {
				if (giveUp > retryLimit)
					return false;

				this.Task.RunCommand(cmd);
				this.sleep(1);
				giveUp++;
			}
		}

		File file = new File("/tmp/osh/");
		if (file.exists()) {
			this.Task.RunCommand(busybox + "rm -r /tmp/osh");
			if (!this.Task.wasSuccessfull) {
				return false;
			}
		}

		return true;
	}

	/**
	 * @param busybox
	 * @return
	 */
	private boolean setupFileSystem(String busybox) {
		File file = new File(WebtopEnabler.drivefile);

		// Get free space on data
		StatFs stat = new StatFs("/data");
		int FreeSpace = stat.getAvailableBlocks() * stat.getBlockSize() / 1024;

		sleep(1);

		// Checking free space
		this.AlertMessage = "Error creating " + WebtopEnabler.drivefile + "!";
		this.ProgressMessage = "Checking free space";
		publishProgress(-1);

		this.sleep(1);

		WebtopEnabler.logHelper(LOGTAG, String.valueOf("Free space on /data: "
				+ FreeSpace));
		if ((file.exists())
				|| (FreeSpace > (WebtopEnabler.drivefilesize * 1024))) {

			WebtopEnabler.logHelper(LOGTAG, String.valueOf("Found "
					+ WebtopEnabler.drivefile));
			if (!file.exists()) {

				ProgressMessage = "Creating filesystem (Please be patient)";
				publishProgress(-1);

				this.Task.RunCommand(busybox + "dd if=/dev/zero of="
						+ WebtopEnabler.drivefile + " bs=1024 count="
						+ (WebtopEnabler.drivefilesize * 1024));
				if (!this.Task.wasSuccessfull) {
					return false;
				}

			} else {
				this.ProgressMessage = "Filesystem found. Reusing";
				publishProgress(-1);
				sleep(2);
			}

			// Setting permissions
			ProgressMessage = "Setting perimission on "
					+ WebtopEnabler.drivefile;
			publishProgress(-1);

			this.Task.RunCommand(busybox + "chmod 644 "
					+ WebtopEnabler.drivefile);
			if (!this.Task.wasSuccessfull) {
				return false;
			}

			// Formatting filesystem
			this.AlertMessage = "Unable to format the filesystem!";
			this.ProgressMessage = "Formatting: " + WebtopEnabler.drivefile;
			publishProgress(-1);

			this.Task.RunCommand(busybox + "losetup /dev/block/loop7 "
					+ WebtopEnabler.drivefile);
			if (!this.Task.wasSuccessfull) {
				return false;
			}

			this.Task
					.RunCommand("/sbin/mkfs -t ext3 -m 1 -b 2048 /dev/block/loop7");
			if (!this.Task.wasSuccessfull) {
				return false;
			}

			this.Task.RunCommand(busybox + "losetup -d /dev/block/loop7");
			if (!this.Task.wasSuccessfull) {
				return false;
			}

			// Mounting filesystem
			this.AlertMessage = "Unable to mount filesystem!";
			this.ProgressMessage = "Mounting filesystem: "
					+ WebtopEnabler.drivefile;
			publishProgress(-1);

			this.Task.RunCommand(busybox + "losetup /dev/block/loop7 "
					+ WebtopEnabler.drivefile);
			if (!this.Task.wasSuccessfull) {
				return false;
			}

			this.Task.RunCommand(busybox + "mkdir -p /tmp/osh");
			if (!this.Task.wasSuccessfull) {
				return false;
			}

			this.Task.RunCommand(busybox
					+ "mount -t ext3 /dev/block/loop7 /tmp/osh");
			if (!this.Task.wasSuccessfull) {
				return false;
			}

			this.Task.RunCommand(busybox + "mkdir -p /tmp/deb");
			if (!this.Task.wasSuccessfull) {
				return false;
			}

		} else {

			return false;
		}

		return true;
	}

	/**
	 * @return
	 */
	private boolean InstallWget() {

		// Installing wget
		this.AlertMessage = "Error installing the wget package!";
		this.ProgressMessage = "Installing the wget package";
		publishProgress(-1);

		String cmd = "/usr/bin/dpkg-deb -x " + Path
				+ "/wget_1.11.4-2ubuntu1_armel.deb /tmp/deb";
		this.Task.RunCommand(cmd);
		if (!this.Task.wasSuccessfull) {
			return false;
		}

		return true;
	}

	/**
	 * @return
	 */
	private boolean InstallRysnc() {

		// Installing rsync
		this.AlertMessage = "Unknown error installing the rsync package!";
		this.ProgressMessage = "Installing the rsync package";
		publishProgress(-1);

		String cmd = "/tmp/deb/usr/bin/wget --waitretry=10 http://ports.ubuntu.com/pool/main/r/rsync/rsync_3.0.5-1ubuntu2_armel.deb -O /tmp/rsync.deb";
		this.Task.RunCommand(cmd);
		if (!this.Task.wasSuccessfull) {
			return false;
		}

		this.Task.RunCommand("/usr/bin/dpkg-deb -x /tmp/rsync.deb /tmp/deb");
		if (!this.Task.wasSuccessfull) {
			return false;
		}

		return true;
	}

	/**
	 * @return
	 */
	private boolean ReplicateOsh() {

		// Replicating /osh
		this.AlertMessage = "Unable to replicate /osh!";
		this.ProgressMessage = "Calculating rsync progress";
		publishProgress(-1);

		String cmd = "/tmp/deb/usr/bin/rsync -ax --delete --stats --progress --dry-run /osh/ /tmp/osh/";
		this.Task.RunCommand(cmd, true);
		if (!this.Task.wasSuccessfull) {
			return false;
		}

		this.ProgressMessage = "Replicating /osh \n(This is going to take awhile)";
		publishProgress(-1);

		cmd = "/tmp/deb/usr/bin/rsync -ax --delete --progress /osh/ /tmp/osh/";
		this.Task.RunCommand(cmd, true);
		if (!this.Task.wasSuccessfull) {
			return false;
		}

		return true;
	}

	/**
	 * @param busybox
	 * @return
	 */
	private boolean setupSudoers(String busybox) {

		// Setting up sudoers
		this.AlertMessage = "Unable to modify sudoers!";
		this.ProgressMessage = "Setting up sudoers";
		publishProgress(-1);

		this.Task.RunCommand(busybox + "cp " + Path
				+ "/fullwebtop/sudoers.new /tmp/osh/etc/sudoers.new");
		if (!this.Task.wasSuccessfull) {
			return false;
		}

		this.Task.RunCommand(busybox + "chmod 0440 /tmp/osh/etc/sudoers.new");
		if (!this.Task.wasSuccessfull) {
			return false;
		}

		this.Task.RunCommand(busybox + "chown 0 /tmp/osh/etc/sudoers.new");
		if (!this.Task.wasSuccessfull) {
			return false;
		}

		this.Task.RunCommand(busybox + "chgrp 0 /tmp/osh/etc/sudoers.new");
		if (!this.Task.wasSuccessfull) {
			return false;
		}

		this.Task.RunCommand(busybox
				+ "mv /tmp/osh/etc/sudoers.new /tmp/osh/etc/sudoers");
		if (!this.Task.wasSuccessfull) {
			return false;
		}

		return true;
	}

	/**
	 * @param busybox
	 * @return
	 */
	private boolean ReplaceExceptionPolicy(String busybox) {

		// Replacing exception_policy.conf
		this.AlertMessage = "Unable delete /tmp/osh/etc/tomoyo/exception_policy.conf!";
		this.ProgressMessage = "Replacing exception_policy.conf";
		publishProgress(-1);

		this.Task.RunCommand(busybox
				+ "rm /tmp/osh/etc/tomoyo/exception_policy.conf");
		if (!this.Task.wasSuccessfull) {
			return false;
		}

		this.Task.RunCommand(busybox
				+ "touch /tmp/osh/etc/tomoyo/exception_policy.conf");
		if (!this.Task.wasSuccessfull) {
			return false;
		}

		this.Task.RunCommand(busybox
				+ "rm /tmp/osh/etc/tomoyo/domain_policy.conf");
		if (!this.Task.wasSuccessfull) {
			return false;
		}

		this.Task.RunCommand(busybox
				+ "touch /tmp/osh/etc/tomoyo/domain_policy.conf");
		if (!this.Task.wasSuccessfull) {
			return false;
		}

		return true;
	}

	/**
	 * @param busybox
	 * @return
	 */
	private boolean InstallLXTerminal(String busybox) {
		String cmd = null;

		// Checking LXTerminal
		this.AlertMessage = "Unable to install lxterminal package!";
		this.ProgressMessage = "Checking the LXTerminal package";
		publishProgress(-1);

		File file = new File("/tmp/osh/usr/bin/lxterminal");
		if (!file.exists()) {

			file = new java.io.File("/tmp/osh/usr/bin/.lxterminal");
			if (!file.exists()) {
				cmd = busybox
						+ "mv /tmp/osh/usr/bin/.lxterminal /tmp/osh/usr/bin/lxterminal";
				this.Task.RunCommand(cmd);
				if (!this.Task.wasSuccessfull) {
					return false;
				}
			}

			this.ProgressMessage = "LXTerminal appears to have been deleted. Re-installing";
			publishProgress(-1);

			cmd = "/tmp/deb/usr/bin/wget --waitretry=10 http://ports.ubuntu.com/pool/universe/l/lxterminal/lxterminal_0.1.3-2_armel.deb -O /tmp/lxterminal.deb";
			this.Task.RunCommand(cmd);
			if (!this.Task.wasSuccessfull) {
				return false;
			}

			cmd = busybox
					+ "env PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin /usr/bin/dpkg -i --root=/tmp/osh /tmp/lxterminal.deb";
			this.Task.RunCommand(cmd);
			if (!this.Task.wasSuccessfull) {
				return false;
			}

			cmd = "/bin/mv /tmp/osh/usr/bin/.lxterminal /tmp/osh/usr/bin/lxterminal";
			this.Task.RunCommand(cmd);
			if (!this.Task.wasSuccessfull) {
				return false;
			}
		}

		return true;
	}

	/**
	 * @param busybox
	 * @return
	 */
	private boolean InstallCoreUtils(String busybox) {

		// Check for coreutils
		this.ProgressMessage = "Checking for coreutils";
		publishProgress(-1);

		String cmd = "/usr/bin/dpkg-query -W coreutils";
		this.Task.RunCommand(cmd);
		if (!this.Task.stdout.contains("6.10-6ubuntu1")) {

			// Installing coreutils
			this.AlertMessage = "Unable to install coreutils!";
			this.ProgressMessage = "Installing coreutils";
			publishProgress(-1);

			cmd = "/tmp/deb/usr/bin/wget --waitretry=10 http://ports.ubuntu.com/pool/main/c/coreutils/coreutils_6.10-6ubuntu1_armel.deb -O /tmp/coreutils.deb";
			this.Task.RunCommand(cmd);
			if (!this.Task.wasSuccessfull) {
				return false;
			}

			cmd = busybox
					+ "env PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin /usr/bin/dpkg -i --root=/tmp/osh --force-overwrite /tmp/coreutils.deb";
			this.Task.RunCommand(cmd);
			if (!this.Task.wasSuccessfull) {
				return false;
			}
		}

		return true;
	}

	/**
	 * @param busybox
	 * @return
	 */
	private boolean InstallCPIO(String busybox) {

		// Check for cpio
		this.AlertMessage = "Unable to install cpio!";
		this.ProgressMessage = "Checking for cpio package";
		publishProgress(-1);

		this.Task.RunCommand("/usr/bin/dpkg-query -W cpio");
		if (!this.Task.stdout.contains("2.9-15ubuntu1")) {

			// Installing cpio
			this.ProgressMessage = "Installing cpio";
			publishProgress(-1);

			String cmd = "/tmp/deb/usr/bin/wget --waitretry=10 http://ports.ubuntu.com/pool/main/c/cpio/cpio_2.9-15ubuntu1_armel.deb -O /tmp/cpio.deb";
			this.Task.RunCommand(cmd);
			if (!this.Task.wasSuccessfull) {
				return false;
			}

			cmd = busybox
					+ "env PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin /usr/bin/dpkg -i --root=/tmp/osh --force-overwrite /tmp/cpio.deb";
			this.Task.RunCommand(cmd);
			if (!this.Task.wasSuccessfull) {
				return false;
			}
		}

		return true;
	}

	/**
	 * @param busybox
	 * @return
	 */
	private boolean InstallDbus(String busybox) {
		String output = null;

		// Check for dbus
		this.ProgressMessage = "Checking for dbus package";
		publishProgress(-1);

		this.Task.RunCommand("/usr/bin/dpkg-query -W dbus");
		if (!this.Task.stdout.contains("1.2.12-0ubuntu2")) {

			// Installing dbus
			this.ProgressMessage = "Installing dbus";
			publishProgress(-1);

			String cmd = busybox
					+ "cp -p /tmp/osh/etc/init.d/dbus /tmp/osh/etc/init.d/dbus.orig";
			this.Task.RunCommand(cmd);
			if (!this.Task.wasSuccessfull) {
				return false;
			}

			cmd = "/tmp/deb/usr/bin/wget --waitretry=10 http://ports.ubuntu.com/pool/main/d/dbus/dbus_1.2.12-0ubuntu2_armel.deb -O /tmp/dbus.deb";
			this.Task.RunCommand(cmd);
			if (!this.Task.wasSuccessfull) {
				return false;
			}

			cmd = busybox + "chmod 0000 /tmp/osh/usr/sbin/invoke-rc.d";
			this.Task.RunCommand(cmd);
			if (!this.Task.wasSuccessfull) {
				return false;
			}

			this.Task.RunCommand(busybox + "mount --bind /proc /tmp/osh/proc");
			if (!this.Task.wasSuccessfull) {
				return false;
			}

			cmd = busybox
					+ "env PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin /usr/bin/dpkg -i --root=/tmp/osh --force-overwrite /tmp/dbus.deb";
			this.Task.RunCommand(cmd);
			if (!this.Task.wasSuccessfull) {
				return false;
			}

			// TODO: Kill the dbus-daemon that got kicked off by this
			// install so that we can unmount /tmp/osh later.
			this.Task.RunCommand(busybox + "pidof dbus");
			if ((output = this.Task.stdout) != null) {

				String pid = output.trim();
				if (pid != null) {
					WebtopEnabler.logHelper(LOGTAG, "dbus PID: " + pid);

					this.ProgressMessage = "Killing dbus";
					publishProgress(-1);

					this.Task.RunCommand(busybox + "kill -9 " + pid);
					if (!this.Task.wasSuccessfull) {
						return false;
					}
				}
			}

			this.Task.RunCommand(busybox
					+ "chmod 0755 /tmp/osh/usr/sbin/invoke-rc.d");
			if (!this.Task.wasSuccessfull) {
				return false;
			}

			cmd = busybox
					+ "mv /tmp/osh/etc/init.d/dbus.orig /tmp/osh/etc/init.d/dbus";
			this.Task.RunCommand(cmd);
			if (!this.Task.wasSuccessfull) {
				return false;
			}

			this.Task.RunCommand(busybox + "umount /tmp/osh/proc");
			if (!this.Task.wasSuccessfull) {
				return false;
			}
		}

		return true;
	}

	/**
	 * @param busybox
	 * @return
	 */
	private boolean InstallDbusX11(String busybox) {

		// Check for dbus-x11
		this.AlertMessage = "Unable to install package dbus-x11";
		this.ProgressMessage = "Checking for dbus-x11";
		publishProgress(-1);

		this.Task.RunCommand("/usr/bin/dpkg-query -W dbus-x11");
		if (Task.stdout.contains("1.2.12-0ubuntu2")) {

			// Installing dbus-x11
			this.ProgressMessage = "Installing dbus-x11";
			publishProgress(-1);

			String cmd = busybox
					+ "cp -p /tmp/osh/etc/X11/Xsession.d/75dbus_dbus-launch /tmp/osh/etc/X11/Xsession.d/75dbus_dbus-launch.orig";
			this.Task.RunCommand(cmd);
			if (!this.Task.wasSuccessfull) {
				return false;
			}

			cmd = "/tmp/deb/usr/bin/wget --waitretry=10 http://ports.ubuntu.com/pool/main/d/dbus/dbus-x11_1.2.12-0ubuntu2_armel.deb -O /tmp/dbus-x11.deb";
			this.Task.RunCommand(cmd);
			if (!this.Task.wasSuccessfull) {
				return false;
			}

			cmd = busybox
					+ "env PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin /usr/bin/dpkg -i --root=/tmp/osh --force-overwrite /tmp/dbus-x11.deb";
			this.Task.RunCommand(cmd);
			if (!this.Task.wasSuccessfull) {
				return false;
			}

			cmd = busybox
					+ "mv /tmp/osh/etc/X11/Xsession.d/75dbus_dbus-launch.orig /tmp/osh/etc/X11/Xsession.d/75dbus_dbus-launch";
			this.Task.RunCommand(cmd);
			if (!this.Task.wasSuccessfull) {
				return false;
			}
		}

		return true;
	}

	private boolean InstallDhcp3Client(String busybox) {

		// Check for dhcp3-client
		this.AlertMessage = "Unable to install dhcp3-client!";
		this.ProgressMessage = "Checking for dhcp3-client";
		publishProgress(-1);

		this.Task.RunCommand("/usr/bin/dpkg-query -W dhcp3-client");
		if (!Task.stdout.contains("3.1.1-5ubuntu8")) {

			// Installing Installing dhcp3-client
			this.ProgressMessage = "Installing dhcp3-client";
			publishProgress(-1);

			String cmd = "/tmp/deb/usr/bin/wget --waitretry=10 http://ports.ubuntu.com/pool/main/d/dhcp3/dhcp3-client_3.1.1-5ubuntu8_armel.deb -O /tmp/dhcp3-client.deb";
			this.Task.RunCommand(cmd);
			if (!this.Task.wasSuccessfull) {
				return false;
			}

			cmd = busybox
					+ "env PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin /usr/bin/dpkg -i --root=/tmp/osh --force-overwrite /tmp/dhcp3-client.deb";
			this.Task.RunCommand(cmd);
			if (!this.Task.wasSuccessfull) {
				return false;
			}
		}

		return true;
	}

	/**
	 * @param busybox
	 * @return
	 */
	private boolean InstallFindUtils(String busybox) {

		// Check for findutils
		this.AlertMessage = "Unable to install package findutils!";
		this.ProgressMessage = "Checking for findutils";
		publishProgress(-1);

		this.Task.RunCommand("/usr/bin/dpkg-query -W findutils");
		if (!Task.stdout.contains("4.4.0-2ubuntu3")) {

			// Installing findutils
			this.ProgressMessage = "Installing findutils";
			publishProgress(-1);

			String cmd = "/tmp/deb/usr/bin/wget --waitretry=10 http://ports.ubuntu.com/pool/main/f/findutils/findutils_4.4.0-2ubuntu3_armel.deb -O /tmp/findutils.deb";
			this.Task.RunCommand(cmd);
			if (!this.Task.wasSuccessfull) {
				return false;
			}

			cmd = busybox
					+ "env PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin /usr/bin/dpkg -i --root=/tmp/osh --force-overwrite /tmp/findutils.deb";
			this.Task.RunCommand(cmd);
			if (!this.Task.wasSuccessfull) {
				return false;
			}
		}

		return true;
	}

	/**
	 * @param busybox
	 * @return
	 */
	private boolean InstallLogrotate(String busybox) {

		// Check for logrotate
		this.ProgressMessage = "Checking for the logrotate package";
		this.AlertMessage = "Unable to install logrotate!";
		publishProgress(-1);

		this.Task.RunCommand("/usr/bin/dpkg-query -W logrotate");
		if (Task.stdout.contains("3.7.7-3ubuntu1")) {

			// Installing logrotate
			this.ProgressMessage = "Installing logrotate";
			publishProgress(-1);

			String cmd = busybox
					+ "cp -p /tmp/osh/etc/logrotate.conf /tmp/osh/etc/logrotate.conf.orig";
			this.Task.RunCommand(cmd);
			if (!this.Task.wasSuccessfull) {
				return false;
			}

			cmd = "/tmp/deb/usr/bin/wget --waitretry=10 http://ports.ubuntu.com/pool/main/l/logrotate/logrotate_3.7.7-3ubuntu1_armel.deb -O /tmp/logrotate.deb";
			this.Task.RunCommand(cmd);
			if (!this.Task.wasSuccessfull) {
				return false;
			}

			cmd = busybox
					+ "env PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin /usr/bin/dpkg -i --root=/tmp/osh --force-depends --force-overwrite /tmp/logrotate.deb";
			this.Task.RunCommand(cmd);
			if (!this.Task.wasSuccessfull) {
				return false;
			}

			cmd = busybox
					+ "mv /tmp/osh/etc/logrotate.conf.orig /tmp/osh/etc/logrotate.conf";
			this.Task.RunCommand(cmd);
			if (!this.Task.wasSuccessfull) {
				return false;
			}
		}

		return true;
	}

	/**
	 * @param busybox
	 * @return
	 */
	private boolean InstallPulseAudio(String busybox) {

		// Check for pulseaudio
		this.ProgressMessage = "Checking for the pulseaudio package";
		this.AlertMessage = "Unable to install the pulseaudio package";
		publishProgress(-1);

		this.Task.RunCommand("/usr/bin/dpkg-query -W pulseaudio");
		if (Task.stdout.contains("1:0.9.14-0ubuntu20")) {

			// Installing pulseaudio
			this.ProgressMessage = "Installing pulseaudio";
			publishProgress(-1);

			String cmd = busybox
					+ "cp -p /tmp/osh/etc/pulse/daemon.conf /tmp/osh/etc/pulse/daemon.conf.orig";
			this.Task.RunCommand(cmd);
			if (!this.Task.wasSuccessfull) {
				return false;
			}

			cmd = busybox
					+ "cp -p /tmp/osh/etc/pulse/default.pa /tmp/osh/etc/pulse/default.pa.orig";
			this.Task.RunCommand(cmd);
			if (!this.Task.wasSuccessfull) {
				return false;
			}

			cmd = "/tmp/deb/usr/bin/wget --waitretry=10 http://ports.ubuntu.com/pool/main/p/pulseaudio/pulseaudio_0.9.14-0ubuntu20_armel.deb -O /tmp/pulseaudio.deb";
			this.Task.RunCommand(cmd);
			if (!this.Task.wasSuccessfull) {
				return false;
			}

			this.Task.RunCommand(busybox
					+ "chmod 0755 /tmp/osh/usr/sbin/invoke-rc.d");
			if (!this.Task.wasSuccessfull) {
				return false;
			}

			this.Task.RunCommand(busybox + "mount --bind /proc /tmp/osh/proc");
			if (!this.Task.wasSuccessfull) {
				return false;
			}

			cmd = busybox
					+ "env PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin /usr/bin/dpkg -i --root=/tmp/osh --force-overwrite /tmp/pulseaudio.deb";
			this.Task.RunCommand(cmd);
			if (!this.Task.wasSuccessfull) {
				return false;
			}

			this.Task.RunCommand(busybox
					+ "chmod 0755 /tmp/osh/usr/sbin/invoke-rc.d");
			if (!this.Task.wasSuccessfull) {
				return false;
			}

			cmd = busybox
					+ "mv /tmp/osh/etc/pulse/daemon.conf.orig /tmp/osh/etc/pulse/daemon.conf";
			this.Task.RunCommand(cmd);
			if (!this.Task.wasSuccessfull) {
				return false;
			}

			cmd = busybox
					+ "mv /tmp/osh/etc/pulse/default.pa.orig /tmp/osh/etc/pulse/default.pa";
			this.Task.RunCommand(cmd);
			if (!this.Task.wasSuccessfull) {
				return false;
			}

			this.Task.RunCommand(busybox + "umount /tmp/osh/proc");
			if (!this.Task.wasSuccessfull) {
				return false;
			}
		}

		return true;
	}

	/**
	 * @param busybox
	 * @return
	 */
	private boolean InstallUdev(String busybox) {

		// Check for udev
		this.AlertMessage = "Unable to install the udev package";
		this.AlertMessage = "Checking for the udev package";
		publishProgress(-1);

		this.Task.RunCommand("/usr/bin/dpkg-query -W udev");
		if (Task.stdout.contains("141-1")) {

			// Installing udev
			this.ProgressMessage = "Installing udev";
			publishProgress(-1);

			String cmd = busybox
					+ "cp -p /tmp/osh/etc/init.d/udev /tmp/osh/etc/init.d/udev.orig";
			this.Task.RunCommand(cmd);
			if (!this.Task.wasSuccessfull) {
				return false;
			}

			cmd = "/tmp/deb/usr/bin/wget --waitretry=10 http://ports.ubuntu.com/pool/main/u/udev/udev_141-1_armel.deb -O /tmp/udev.deb";
			this.Task.RunCommand(cmd);
			if (!this.Task.wasSuccessfull) {
				return false;
			}

			cmd = busybox
					+ "env PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin /usr/bin/dpkg -i --root=/tmp/osh --force-overwrite /tmp/udev.deb";
			this.Task.RunCommand(cmd);
			if (!this.Task.wasSuccessfull) {
				return false;
			}

			cmd = busybox
					+ "mv /tmp/osh/etc/init.d/udev.orig /tmp/osh/etc/init.d/udev";
			this.Task.RunCommand(cmd);
			if (!this.Task.wasSuccessfull) {
				return false;
			}
		}

		return true;
	}

	/**
	 * @param busybox
	 * @return
	 */
	private boolean InstallXorgCore(String busybox) {

		// Check for xserver-xorg-core
		this.ProgressMessage = "Checking for xserver-xorg-core";
		this.AlertMessage = "Unable to install the xserver-xorg-core package";
		publishProgress(-1);

		this.Task.RunCommand("/usr/bin/dpkg-query -W xserver-xorg-core");
		if (!Task.stdout.contains("2:1.6.0-0ubuntu14")) {

			// Installing xserver-xorg-core
			this.ProgressMessage = "Installing xserver-xorg-core";
			publishProgress(-1);

			String cmd = busybox
					+ "cp -p /tmp/osh/usr/bin/Xorg /tmp/osh/usr/bin/Xorg.orig";
			this.Task.RunCommand(cmd);
			if (!this.Task.wasSuccessfull) {
				return false;
			}

			cmd = "/tmp/deb/usr/bin/wget --waitretry=10 http://ports.ubuntu.com/pool/main/x/xorg-server/xserver-xorg-core_1.6.0-0ubuntu14_armel.deb -O /tmp/xserver-xorg-core.deb";
			this.Task.RunCommand(cmd);
			if (!this.Task.wasSuccessfull) {
				return false;
			}

			cmd = busybox
					+ "env PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin /usr/bin/dpkg -i --root=/tmp/osh --force-overwrite /tmp/xserver-xorg-core.deb";
			this.Task.RunCommand(cmd);
			if (!this.Task.wasSuccessfull) {
				return false;
			}

			cmd = busybox
					+ "mv /tmp/osh/usr/bin/Xorg.orig /tmp/osh/usr/bin/Xorg";
			this.Task.RunCommand(cmd);
			if (!this.Task.wasSuccessfull) {
				return false;
			}
		}

		return true;
	}

	/**
	 * @param busybox
	 * @return
	 */
	private boolean InstallGpgv(String busybox) {
		// Check for gpgv
		this.ProgressMessage = "Checking for the gpgv package";
		this.AlertMessage = "Unable to install the gpgv package!";
		publishProgress(-1);

		this.Task.RunCommand("/usr/bin/dpkg-query -W gpgv");
		if (!Task.stdout.contains("1.4.9-3ubuntu1")) {

			// Installing gpgv
			this.ProgressMessage = "Installing the gpgv package";
			publishProgress(-1);

			String cmd = "/tmp/deb/usr/bin/wget --waitretry=10 http://ports.ubuntu.com/pool/main/g/gnupg/gpgv_1.4.9-3ubuntu1_armel.deb -O /tmp/gpgv.deb";
			this.Task.RunCommand(cmd);
			if (!this.Task.wasSuccessfull) {
				return false;
			}

			cmd = busybox
					+ "env PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin /usr/bin/dpkg -i --root=/tmp/osh /tmp/gpgv.deb";
			this.Task.RunCommand(cmd);
			if (!this.Task.wasSuccessfull) {
				return false;
			}
		}

		return true;
	}

	/**
	 * @param busybox
	 * @return
	 */
	private boolean SetupAPTSourcesList(String busybox) {
		String output = null;

		// Setting up APT's sources.list
		this.ProgressMessage = "Setting up APT sources.list";
		this.AlertMessage = "Unable to setup APT sources.list";
		publishProgress(-1);

		this.Task.RunCommand(busybox + "md5sum /tmp/osh/etc/apt/sources.list");
		if ((output = Task.stdout) != null) {

			String[] md5sum = output.split(" ");
			WebtopEnabler.logHelper(LOGTAG, "-" + md5sum[0].trim() + "-");
			if (md5sum[0].trim().equals(WebtopEnabler.aptsourcesnew_md5)) {

				// Setting up APT's sources.list
				this.ProgressMessage = "Updating sources.list";
				String cmd = busybox
						+ "cp "
						+ Path
						+ "/fullwebtop/sources.list.new /tmp/osh/etc/apt/sources.list.new";
				this.Task.RunCommand(cmd);
				if (!this.Task.wasSuccessfull) {
					return false;
				}

				this.Task.RunCommand(busybox
						+ "chmod 0644 /tmp/osh/etc/apt/sources.list.new");
				if (!this.Task.wasSuccessfull) {
					return false;
				}

				this.Task.RunCommand(busybox
						+ "chown 0 /tmp/osh/etc/apt/sources.list.new");
				if (!this.Task.wasSuccessfull) {
					return false;
				}

				this.Task.RunCommand(busybox
						+ "chgrp 0 /tmp/osh/etc/apt/sources.list.new");
				if (!this.Task.wasSuccessfull) {
					return false;
				}

				cmd = busybox
						+ "mv /tmp/osh/etc/apt/sources.list.new /tmp/osh/etc/apt/sources.list";
				this.Task.RunCommand(cmd);
				if (!this.Task.wasSuccessfull) {
					return false;
				}
			}
		}

		return true;
	}

	/**
	 * @param busybox
	 * @return
	 */
	private boolean UpdateAptitudeConfiguration(String busybox) {
		String cmd = null;

		// Checking Aptitude configuration
		this.AlertMessage = "Unable to update Aptitude configuration!";
		this.ProgressMessage = "Checking Aptitude configuration";
		publishProgress(-1);

		File file = new java.io.File("/tmp/osh/etc/apt/apt.conf.d/06aptitude");
		if (!file.exists()) {

			cmd = busybox
					+ "cp "
					+ Path
					+ "/fullwebtop/06aptitude.new /tmp/osh/etc/apt/apt.conf.d/06aptitude";
			this.Task.RunCommand(cmd);
			if (!this.Task.wasSuccessfull) {
				return false;
			}

			cmd = busybox + "chmod 0644 /tmp/osh/etc/apt/apt.conf.d/06aptitude";
			this.Task.RunCommand(cmd);
			if (!this.Task.wasSuccessfull) {
				return false;
			}

			this.Task.RunCommand(busybox
					+ "chown 0 /tmp/osh/etc/apt/apt.conf.d/06aptitude");
			if (!this.Task.wasSuccessfull) {
				return false;
			}

			this.Task.RunCommand(busybox
					+ "chgrp 0 /tmp/osh/etc/apt/apt.conf.d/06aptitude");
			if (!this.Task.wasSuccessfull) {
				return false;
			}
		}

		// Updating APT's package lists (this may take a while)
		this.ProgressMessage = "Updating APT's package lists (this may take a while)";
		publishProgress(-1);

		cmd = busybox
				+ "chroot /tmp/osh /usr/bin/env PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin /usr/bin/apt-get update";
		this.Task.RunCommand(cmd);
		if (!this.Task.wasSuccessfull) {
			return false;
		}

		return true;
	}

	/**
	 * @param busybox
	 * @return
	 */
	private boolean CheckBuiltinPackages(String busybox) {

		// Checking built-in packages
		this.ProgressMessage = "Checking built-in packages";
		this.AlertMessage = "Unable to update built-in packages!";
		publishProgress(-1);

		// TODO: FIX
		/*
		 * String cmd = busybox + "cp " + Path +
		 * "/fullwebtop/aptitude_parse.pl /tmp/aptitude_parse.pl";
		 * this.Task.RunCommand(cmd); if (!this.Task.wasSuccessfull) { return
		 * false; }
		 * 
		 * String cmd =
		 * "/usr/sbin/chroot /tmp/osh /usr/bin/env PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin /usr/bin/aptitude hold -f -q -y -s | /usr/bin/perl /tmp/aptitude_parse.pl"
		 * ; this.Task.RunCommand(cmd); if (!this.Task.wasSuccessfull) { return
		 * false; }
		 */

		// Fixing built-in packages (this may take a while)
		this.ProgressMessage = "Fixing built-in packages (this may take a while)";
		publishProgress(-1);

		this.Task.RunCommand(busybox + "mkdir -p /tmp/osh/data");
		if (!this.Task.wasSuccessfull) {
			return false;
		}

		this.Task.RunCommand(busybox + "mount --bind /data /tmp/osh/data");
		if (!this.Task.wasSuccessfull) {
			return false;
		}

		this.Task.RunCommand(busybox
				+ "mount -t devpts devpts /tmp/osh/dev/pts");
		if (!this.Task.wasSuccessfull) {
			return false;
		}

		String cmd = busybox
				+ "chroot /tmp/osh /usr/bin/env PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin /usr/bin/aptitude hold -f -q -y -o Aptitude::Delete-Unused=false -o Dpkg::options::=--root=/";
		this.Task.RunCommand(cmd);
		if (!this.Task.wasSuccessfull) {
			return false;
		}

		return true;
	}

	/**
	 * @param busybox
	 * @return
	 */
	private boolean ReinstallLogrotate(String busybox) {
		String output = null;

		// Re-installing logrotate
		this.ProgressMessage = "Re-installing logrotate";
		this.AlertMessage = "Unable to re-install logrotate!";
		publishProgress(-1);

		String cmd = busybox
				+ "chroot /tmp/osh /usr/bin/env PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin /usr/bin/aptitude install logrotate -q -y -o Aptitude::Delete-Unused=false -o Dpkg::options::=--root=/";
		this.Task.RunCommand(cmd);
		if (!this.Task.wasSuccessfull) {
			return false;
		}

		this.sleep(3);

		// Killing cron
		this.Task.RunCommand(busybox + "pidof cron");
		if ((output = Task.stdout) != null) {

			String pid = output.trim();
			if (pid != null) {
				WebtopEnabler.logHelper(LOGTAG, "cron PID: " + pid);

				this.ProgressMessage = "Killing cron";
				publishProgress(-1);

				this.Task.RunCommand(busybox + "kill -9 " + pid);
				if (!this.Task.wasSuccessfull) {
					return false;
				}
			}
		}

		return true;
	}

	/**
	 * @param busybox
	 * @return
	 */
	private boolean InstallGksu(String busybox) {
		// Installing gksu
		this.AlertMessage = "Unable to install the gksu package!";
		this.ProgressMessage = "Checking the gksu package";
		publishProgress(-1);

		String cmd = busybox
				+ "chroot /tmp/osh /usr/bin/env PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin /usr/bin/aptitude install gksu -q -y -o Aptitude::Delete-Unused=false -o Dpkg::options::=--root=/";
		this.Task.RunCommand(cmd);
		if (!this.Task.wasSuccessfull) {
			return false;
		}

		// Installing synaptic
		this.ProgressMessage = "Installing the gksu package";
		publishProgress(-1);

		cmd = busybox
				+ "chroot /tmp/osh /usr/bin/env PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin /usr/bin/aptitude install synaptic -q -y -o Aptitude::Delete-Unused=false -o Dpkg::options::=--root=/";
		this.Task.RunCommand(cmd);
		if (!this.Task.wasSuccessfull) {
			return false;
		}

		this.Task.RunCommand(busybox + "umount /tmp/osh/dev/pts");
		if (!this.Task.wasSuccessfull) {
			return false;
		}

		this.Task.RunCommand(busybox + "umount /tmp/osh/data");
		if (!this.Task.wasSuccessfull) {
			return false;
		}

		this.Task.RunCommand(busybox + "rmdir /tmp/osh/data");
		if (!this.Task.wasSuccessfull) {
			return false;
		}

		return true;
	}

	private boolean InstallAWN(String busybox) {

		File windowManager = new File(
				"/data/home/adas/.gconf/apps/avant-window-navigator/window_manager/%gconf.xml");

		this.AlertMessage = "Unable to modify avant window navigator (AWN)!";
		this.ProgressMessage = "Modifying avant window navigator (AWN)";
		publishProgress(-1);

		if (windowManager.exists()) {

			this.Task
					.RunCommand(busybox
							+ "mkdir -p /data/home/adas/.gconf/apps/avant-window-navigator/window_manager");
			if (!this.Task.wasSuccessfull) {
				return false;
			}

			this.Task.RunCommand(busybox
					+ "chmod -R 0700 /data/home/adas/.gconf");
			if (!this.Task.wasSuccessfull) {
				return false;
			}

			this.Task.RunCommand(busybox
					+ "touch /data/home/adas/.gconf/apps/%gconf.xml");
			if (!this.Task.wasSuccessfull) {
				return false;
			}

			this.Task.RunCommand(busybox
					+ "chmod 0600 /data/home/adas/.gconf/apps/%gconf.xml");
			if (!this.Task.wasSuccessfull) {
				return false;
			}

			this.Task
					.RunCommand(busybox
							+ "cp "
							+ Path
							+ "fullwebtop/%gconf.xml.new /data/home/adas/.gconf/apps/avant-window-navigator/window_manager/%gconf.xml");
			if (!this.Task.wasSuccessfull) {
				return false;
			}

			this.Task
					.RunCommand(busybox
							+ "chmod 0600 /data/home/adas/.gconf/apps/avant-window-navigator/window_manager/%gconf.xml");
			if (!this.Task.wasSuccessfull) {
				return false;
			}

			this.Task.RunCommand(busybox
					+ "chown -R 5000:5000 /data/home/adas/.gconf");
			if (!this.Task.wasSuccessfull) {
				return false;
			}
		}

		return true;
	}

	/**
	 * @param busybox
	 * @return
	 */
	private boolean InstallMountosh(String busybox) {
		String output = null;

		this.AlertMessage = "Unable to install the modified /system/bin/mountosh";
		this.ProgressMessage = "Modifying mountosh boot script";
		publishProgress(-1);

		this.Task.RunCommand(busybox + "mount");
		if ((output = this.Task.stdout) != null) {
			WebtopEnabler.logHelper(LOGTAG, output);

			String str;
			BufferedReader reader = new BufferedReader(
					new StringReader(output), bufferSize);

			try {
				while ((str = reader.readLine()) != null) {

					if (str.contains("/dev/block/mmcblk0p12")) {
						if (str.contains("ro")) {
							this.Task.RunCommand(busybox
									+ "mount -o rw,remount /system");
							if (!this.Task.wasSuccessfull) {
								return false;
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

		// Updating mountosh
		this.ProgressMessage = "Installing the updated /system/bin/mountosh";
		publishProgress(-1);

		this.Task.RunCommand(busybox + "md5sum /system/bin/mountosh");
		if ((output = this.Task.stdout) != null) {

			String[] md5sum = output.split(" ");
			WebtopEnabler.logHelper(LOGTAG, md5sum[0].trim());
			if (md5sum[0].trim().equals(WebtopEnabler.mountosh_md5)) {

				String cmd = busybox + "cp " + Path
						+ "/fullwebtop/mountosh.new /system/bin/mountosh.new";
				this.Task.RunCommand(cmd);
				if (!this.Task.wasSuccessfull) {
					return false;
				}

				this.Task.RunCommand(busybox
						+ "chmod 0755 /system/bin/mountosh.new");
				if (!this.Task.wasSuccessfull) {
					return false;
				}

				this.Task.RunCommand(busybox
						+ "chown 0 /system/bin/mountosh.new");
				if (!this.Task.wasSuccessfull) {
					return false;
				}

				this.Task.RunCommand(busybox
						+ "chgrp 2000 /system/bin/mountosh.new");
				if (!this.Task.wasSuccessfull) {
					return false;
				}

				cmd = busybox
						+ "mv /system/bin/mountosh /system/bin/mountosh.orig";
				this.Task.RunCommand(cmd);
				if (!this.Task.wasSuccessfull) {
					return false;
				}

				cmd = busybox
						+ "mv /system/bin/mountosh.new /system/bin/mountosh";
				this.Task.RunCommand(cmd);
				if (!this.Task.wasSuccessfull) {
					return false;

				} else {

					cmd = busybox + "umount -f /dev/block/loop7";
					this.Task.RunCommand(cmd);
					if (!this.Task.wasSuccessfull) {
						return false;
					}
				}
			}
		}

		return true;
	}

	@Override
	protected void onPostExecute(Boolean result) {

		// TODO: Why isn't the progressbox being dismissed?
		this.cancel(true);

		if (result) {
			switch (RebootType) {
			case 0:
				this.Dialog.showActionConfirmation(this.AlertTitle,
						this.AlertMessage, this.Reboot());
				break;

			case 1:
				this.Dialog.showActionConfirmation(this.AlertTitle,
						this.AlertMessage, this.Regimp());
				break;
			}

		} else
			this.Dialog.showAlert(this.AlertTitle, this.AlertMessage);

		this.Task = null;
		this.Dialog = null;
	}

	protected OnClickListener Regimp() {
		return new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				new RegimpWebtop(Activity, Path).execute();
			}
		};
	}

	protected OnClickListener Reboot() {
		return new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				String[] RebootCmd = new String[] { "sync", "sync", "reboot" };
				new Reboot(Activity, Path, RebootCmd).execute();
			}
		};
	}

	public void StdoutCallback(String line) {

		if (line.contains("to-check=")) {
			String[] split = line.split("=");
			String split2 = split[1].substring(0, (split[1].length() - 1));
			String[] numOfFiles = split2.split("/");

			try {
				publishProgress((Integer.valueOf(numOfFiles[1]) - Integer
						.valueOf(numOfFiles[0])));
				WebtopEnabler.logHelper(LOGTAG, "Updating progress bar: "
						+ (Integer.valueOf(numOfFiles[1]) - Integer
								.valueOf(numOfFiles[0])));
			} catch (Exception e) {

				e.printStackTrace();
			}
		} else if (line.contains("Number of files:")) {
			String[] split = line.split(":");
			String currentCopied = split[1].trim();

			this.Dialog.setMaxValue(Integer.valueOf(currentCopied));
			WebtopEnabler.logHelper(LOGTAG,
					"Total number of files left to sync: " + currentCopied);
		}
	}

	public void sleep(final int time) {
		try {

			Thread.sleep(time * 1000);

		} catch (InterruptedException e) {

			e.printStackTrace();
		}
	}
}