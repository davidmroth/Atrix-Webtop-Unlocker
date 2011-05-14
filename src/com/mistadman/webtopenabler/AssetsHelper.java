package com.mistadman.webtopenabler;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import android.content.Context;
import android.util.Log;

public class AssetsHelper {

	static final String LOGTAG = AssetsHelper.class.getSimpleName();
	static final int BUFSIZE = 5192;
	static final String ZIP_FILTER = "assets";

	static void unzipAssets(Context context) {
		String apkPath = context.getPackageCodePath();
		String mAppRoot = context.getFilesDir().toString();


		try {
			File zipFile = new File(apkPath);
			long zipLastModified = zipFile.lastModified();
			ZipFile zip = new ZipFile(apkPath);
			Vector<ZipEntry> files = AssetsHelper.getAssets(zip);
			int zipFilterLength = ZIP_FILTER.length();

			Enumeration<?> entries = files.elements();
			while (entries.hasMoreElements()) {

				ZipEntry entry = (ZipEntry) entries.nextElement();
				String path = entry.getName().substring(zipFilterLength);
				File outputFile = new File(mAppRoot, path);
				outputFile.getParentFile().mkdirs();

				if (outputFile.exists()
						&& entry.getSize() == outputFile.length()
						&& zipLastModified < outputFile.lastModified())
					continue;

				FileOutputStream fos = new FileOutputStream(outputFile);
				AssetsHelper.copyStreams(zip.getInputStream(entry), fos);
				Runtime.getRuntime().exec(
						"chmod 755 " + outputFile.getAbsolutePath());
			}

		} catch (IOException e) {
			Log.e(LOGTAG, "Error: " + e.getMessage());
		}
	}

	static void copyStreams(InputStream is, FileOutputStream fos) {
		BufferedOutputStream os = null;

		try {
			byte data[] = new byte[BUFSIZE];
			int count;
			os = new BufferedOutputStream(fos, BUFSIZE);

			while ((count = is.read(data, 0, BUFSIZE)) != -1) {
				os.write(data, 0, count);
			}

			os.flush();

		} catch (IOException e) {
			Log.e(LOGTAG, "Exception while copying: " + e);

		} finally {

			try {

				if (os != null) {
					os.close();
				}

			} catch (IOException e2) {
				Log.e(LOGTAG, "Exception while closing the stream: " + e2);
			}
		}
	}

	public static Vector<ZipEntry> getAssets(ZipFile zip) {
		Vector<ZipEntry> list = new Vector<ZipEntry>();
		Enumeration<?> entries = zip.entries();

		while (entries.hasMoreElements()) {
			ZipEntry entry = (ZipEntry) entries.nextElement();

			if (entry.getName().startsWith(ZIP_FILTER)) {
				list.add(entry);
			}
		}

		return list;
	}
}