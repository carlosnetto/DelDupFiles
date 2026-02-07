/*
 * FileZipEntry
 * Author: Carlos Netto - carlos.netto@gmail.com
 *
 * LICENSE: This software is 100% open source. You can use, modify, copy, 
 * or distribute it as you wish without limitations.
 *
 * NO WARRANTY: THIS SOFTWARE IS PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND.
 * THE AUTHOR IS NOT LIABLE FOR ANY DATA LOSS (INCLUDING VALUABLE PICTURES).
 * ALWAYS MAKE BACKUPS!
 *
 * Represents a file entity, which can be a standard file on the filesystem
 * or an entry inside a ZIP archive.
 *
 * Performance Strategy:
 * 1. Lazy Calculation: CRC32 and Size are only calculated when first requested.
 * 2. ZIP Optimization: ZIP files store the CRC32 of their entries natively. 
 *    We retrieve this value without reading/decompressing the data.
 */
package com.matera.javafiletools;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class FileZipEntry {
	/** The physical file on disk (or the ZIP container if ze != null). */
	private final File fi;
	private ZipFile zf = null;
	private ZipEntry ze = null;

	/** Cached values for performance. */
	private Long crc32 = null;
	private Long crc32_64k = null;
	private Long size = null;

	/** Constructor for a standard OS file. */
	public FileZipEntry(File f) {
		this.fi = f;
	}

	/** Constructor for an entry inside a ZIP file. */
	public FileZipEntry(ZipEntry z, ZipFile fz, File f) {
		this.fi = f;
		this.zf = fz;
		this.ze = z;
	}

	/**
	 * Calculates the full CRC32 of the file content.
	 */
	private void calcCrc32() {
		if (ze != null) { 
			// ZIP Optimization: No need to read the file! 
			// The ZIP header already contains the CRC32.
			crc32 = ze.getCrc();
			size = ze.getSize();
		} else { 
			// Standard File: Must read the entire content to calculate CRC.
			var buf = new byte[1024 * 100]; 
			var auxCrc32 = new CRC32();
			try (var fis = new FileInputStream(fi)) {
				int n;
				while ((n = fis.read(buf)) != -1) {
					auxCrc32.update(buf, 0, n);
				}
			} catch (IOException e) {
				System.err.println("Error calculating CRC32 for " + fi + ": " + e.getMessage());
			}
			crc32 = auxCrc32.getValue();
			size = fi.length();
		}
	}

	/**
	 * Calculates the CRC32 of the first 64KB (Partial CRC).
	 * Used as a "Lazy Key" for fast O(1) lookups in the Map.
	 */
	private void calcCrc32_64k() {
		var buf = new byte[1024 * 64]; 
		var auxCrc32 = new CRC32();

		if (ze != null) { 
			// For ZIP entries, we still have to read the first bytes if we want a partial CRC.
			try (var zis = zf.getInputStream(ze)) {
				size = ze.getSize();
				if (size > 0) {
					var n = zis.read(buf);
					if (n > 0) auxCrc32.update(buf, 0, n);
					crc32_64k = auxCrc32.getValue();
				} else {
					crc32_64k = 0L;
				}
			} catch (IOException e) {
				crc32_64k = 0L;
			}
		} else {
			size = fi.length();
			if (size > 0) {
				try (var fis = new FileInputStream(fi)) {
					var n = fis.read(buf);
					if (n > 0) auxCrc32.update(buf, 0, n);
					crc32_64k = auxCrc32.getValue();
				} catch (IOException e) {
					crc32_64k = 0L;
				}
			} else {
				crc32_64k = 0L;
			}
		}
	}

	public File getFile() {
		return fi;
	}

	/** Returns the full CRC32, calculating it once if needed. */
	public Long getCrc32() {
		if (crc32 == null) calcCrc32();
		return crc32;
	}

	/** Returns the partial CRC32 of the first 64KB. */
	public Long getCrc32_64k() {
		if (crc32_64k == null) calcCrc32_64k();
		return crc32_64k;
	}

	/** Returns the file size, using cached value if available. */
	public Long length() {
		if (size == null) {
			if (ze == null) calcCrc32_64k(); // Size is set during partial CRC calc
			else calcCrc32();               // Size is set during full CRC calc
		}
		return size;
	}

	/**
	 * Generates a key for hashing. 
	 * Format: "partialCRC:size"
	 */
	public String getUniqueKey() {
		if (crc32_64k == null) calcCrc32_64k();
		return crc32_64k + ":" + size;
	}
	
	public Long getDate() {
		return (ze == null) ? fi.lastModified() : ze.getTime();
	}

	@Override
	public String toString() {
		return (ze != null) ? fi + "|" + ze.getName() : fi.toString();
	}
}