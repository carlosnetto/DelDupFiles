/*
 * FileHashMap
 * Author: Carlos Netto - carlos.netto@gmail.com
 *
 * LICENSE: This software is 100% open source. You can use, modify, copy, 
 * or distribute it as you wish without limitations.
 *
 * NO WARRANTY: THIS SOFTWARE IS PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND.
 * THE AUTHOR IS NOT LIABLE FOR ANY DATA LOSS (INCLUDING VALUABLE PICTURES).
 * ALWAYS MAKE BACKUPS!
 *
 * A specialized HashMap wrapper that indexes files by a "Lazy Key" (Size + partial CRC).
 * 
 * Design Note: Because different files can occasionally have the same size and 
 * starting bytes (collisions), each key maps to a LinkedList of FileZipEntry objects.
 */
package com.matera.javafiletools;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.zip.ZipFile;

public class FileHashMap {
	/** Internal map: Key = "partialCRC:size", Value = List of matching files. */
	private final Map<String, LinkedList<FileZipEntry>> map = new HashMap<>();

	public FileHashMap(Iterator<File> iterator) {
		this(iterator, false);
	}

	public FileHashMap(Iterator<File> iterator, boolean enterZip) {
		initialize(iterator, enterZip);
	}

	/**
	 * Populates the map by iterating over files.
	 * If enterZip is true, it treats ZIP files as virtual directories.
	 */
	private void initialize(Iterator<File> iterator, boolean enterZip) {
		while (iterator.hasNext()) {
			var file = iterator.next();
			if (file.isDirectory() || !file.canRead())
				continue;
			
			// Only index regular files to avoid pipes, devices, etc.
			if (!Files.isRegularFile(file.toPath(), LinkOption.NOFOLLOW_LINKS))
				continue;

			// Index the file itself
			put(new FileZipEntry(file));

			// If it's a ZIP and we are in "enterZip" mode, index its internal entries
			if (enterZip && file.getName().toUpperCase().endsWith(".ZIP")) {
				try (var zf = new ZipFile(file, ZipFile.OPEN_READ)) {
					var zipEntries = zf.entries();
					while (zipEntries.hasMoreElements()) {
						var ze = zipEntries.nextElement();
						if (!ze.isDirectory()) {
							put(new FileZipEntry(ze, zf, file));
						}
					}
				} catch (IOException e) {
					System.err.println("Error reading ZIP: " + file + " -> " + e.getMessage());
				}
			}
		}
	}

	/** Returns the list of files matching the entry's Lazy Key. */
	public LinkedList<FileZipEntry> get(FileZipEntry file) {
		return map.get(file.getUniqueKey());
	}

	/** Returns the list of files matching a specific Lazy Key. */
	public LinkedList<FileZipEntry> get(String uniqueKey) {
		return map.get(uniqueKey);
	}

	/**
	 * Adds a file to the map. 
	 * Uses computeIfAbsent to handle collisions by appending to a LinkedList.
	 */
	public LinkedList<FileZipEntry> put(FileZipEntry file) {
		var list = map.computeIfAbsent(file.getUniqueKey(), k -> new LinkedList<>());
		list.add(file);
		return list;
	}

	public int size() {
		return map.size();
	}

	public Collection<LinkedList<FileZipEntry>> values() {
		return map.values();
	}

	/**
	 * Local test utility.
	 */
	public static void main(String[] args) {
		var fhm = new FileHashMap(new DirTreeIterator(new File("/tmp")));
		System.out.println("Map ready with " + fhm.size() + " unique keys.");
		for (var fileArray : fhm.values()) {
			for (var file : fileArray) {
				System.out.println(file + " [Key: " + file.getUniqueKey() + "]");
			}
		}
	}
}