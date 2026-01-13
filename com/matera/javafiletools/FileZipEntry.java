package com.matera.javafiletools;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class FileZipEntry {
	File fi = null;
	ZipFile zf = null;
	ZipEntry ze = null;

	Long crc32 = null;
	Long crc32_64k = null;
	Long size = null;

	public FileZipEntry(File f) {
		fi = f;
	}

	public FileZipEntry(ZipEntry z, ZipFile fz, File f) {
		fi = f;
		zf = fz;
		ze = z;
	}

	private void calcCrc32() {
		if (ze != null) { // It's a ZipEntry; everything is ready to be used
			crc32 = ze.getCrc();
			size = ze.getSize();
		} else { // Let's calculate CRC32
			int n;
			byte buf[] = new byte[1024 * 100]; // let's read blocks of 100k
			FileInputStream fis = null;

			CRC32 auxCrc32 = new CRC32();
			auxCrc32.reset();
			try {
				fis = new FileInputStream(fi);
				while ((n = fis.read(buf)) != -1) {
					auxCrc32.update(buf, 0, n);
				}
				fis.close();
			} catch (IOException e) {
				System.err.println(e);
			} finally {
				try {
					fis.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			crc32 = auxCrc32.getValue();
			size = fi.length();
		}
	}

	private void calcCrc32_64k() {
		int n;
		byte buf[] = new byte[1024 * 64]; // let's calculate only 1st 64kb of
											// the file
		CRC32 auxCrc32 = new CRC32();
		auxCrc32.reset();

		if (ze != null) { // It's a ZIP entry actually
			InputStream zis = null;
			try {
				size = ze.getSize();
				if (size > 0) {
					zis = zf.getInputStream(ze);
					n = zis.read(buf);
					auxCrc32.update(buf, 0, n);
					crc32_64k = auxCrc32.getValue();
					auxCrc32 = null;
					zis.close();
				} else
					crc32_64k = 0L;
			} catch (IOException e) {
				System.err.println(e);
			} finally {
				try {
					if (zis != null)
						zis.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		} else {
			FileInputStream fis = null;
			try {
				size = fi.length();
				if (size > 0) {
					fis = new FileInputStream(fi);
					n = fis.read(buf);
					auxCrc32.update(buf, 0, n);
					crc32_64k = auxCrc32.getValue();
					fis.close();
				} else
					crc32_64k = 0L;
			} catch (IOException e) {
				System.err.println(e);
			} finally {
				try {
					if (fis != null)
						fis.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public File getFile() {
		return fi;
	}

	public Long getCrc32() {
		if (crc32 == null) {
			calcCrc32();
		}
		return crc32;
	}

	public Long getCrc32_64k() {
		if (crc32_64k == null) {
			calcCrc32_64k();
		}
		return crc32_64k;
	}

	public Long length() {
		if (size == null)
			if (ze == null) { // It's not a ZIP; cheaper to calc Crc32_64k
				calcCrc32_64k();
			} else { // It's a ZIP; cheaper to calc CRC32
				calcCrc32();
			}
		return size;
	}

	public String getUniqueKey() {
		if (crc32_64k == null) {
			calcCrc32_64k();
		}
		return crc32_64k.toString() + ":" + size.toString();
	}
	
	public Long getDate () {
		if (ze == null) {
			return fi.lastModified();
		} else {
			return ze.getTime();
		}
	}

	public String toString() {
		if (ze != null) {
			return fi.toString() + "|" + ze.toString();
		} else {
			return fi.toString();
		}

	}
}
