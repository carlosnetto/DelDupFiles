/*
 * FileHashMap
 * Author: Carlos Netto - carlos.netto@gmail.com
 *
 * A specialized HashMap wrapper that indexes files by a unique key (Size + partial CRC).
 * Handles collisions by storing a list of files for each key.
 */
package com.matera.javafiletools;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class FileHashMap {
	private static final long serialVersionUID = -1502437921656142170L;
	// Map storing the unique key and a list of files sharing that key
	HashMap<String, LinkedList<FileZipEntry>> map = new HashMap<String, LinkedList<FileZipEntry>>();

	public FileHashMap(Iterator<File> iterator) {
		fileHashMap(iterator, false);
	}

	public FileHashMap(Iterator<File> iterator, boolean enterZip) {
		fileHashMap(iterator, enterZip);
	}

	//
	// Populates the map by iterating over the provided file iterator
	//
	private void fileHashMap(Iterator<File> iterator, boolean enterZip) {
		File file;

		while (iterator.hasNext()) {
			file = iterator.next();
			if (file.isDirectory())
				continue;
			if (!file.canRead())
				continue;
			if (!Files.isRegularFile(file.toPath(),
					java.nio.file.LinkOption.NOFOLLOW_LINKS))
				continue;
			// Add the regular file to the map
			put(new FileZipEntry(file));
			if (enterZip && file.getName().toUpperCase().endsWith(".ZIP")) {
				try (ZipFile zf = new ZipFile(file, ZipFile.OPEN_READ)) {
					for (Enumeration<? extends ZipEntry> zipEntries = zf.entries(); zipEntries.hasMoreElements();) {
						ZipEntry ze = (ZipEntry) zipEntries.nextElement();
						if (!ze.isDirectory()) {
							put(new FileZipEntry(ze, zf, file));
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public LinkedList<FileZipEntry> get(FileZipEntry file) {
		LinkedList<FileZipEntry> fileLinkedList = map.get(file.getUniqueKey());
		return fileLinkedList;
	}

	public LinkedList<FileZipEntry> get(String uniqueKey) {
		LinkedList<FileZipEntry> fileLinkedList = map.get(uniqueKey);
		return fileLinkedList;
	}

	//
	// Adds a file to the map. If the key exists, appends to the list (collision handling).
	//
	public LinkedList<FileZipEntry> put(FileZipEntry file) {
		LinkedList<FileZipEntry> list = map.get(file.getUniqueKey());
		if (list == null) {
			list = new LinkedList<FileZipEntry>();
			list.add(file);
			map.put(file.getUniqueKey(), list);
		} else {
			list.add(file);
		}
		return list;
	}

	public int size() {
		return map.size();
	}

	public Collection<LinkedList<FileZipEntry>> values() {
		return map.values();
	}

	public static void main(String[] args) {
		FileHashMap fhm = new FileHashMap(new DirTreeIterator(new File(
				"/home/tcarlos/Temp")));
		System.out.println("fhm is ready");
		Iterator<LinkedList<FileZipEntry>> iterator = fhm.values().iterator();
		System.out.println("dtm has " + fhm.size() + " elements");
		while (iterator.hasNext()) {
			LinkedList<FileZipEntry> fileArray = iterator.next();
			for (FileZipEntry file : fileArray) {
				System.out.println(file.toString());
				System.out.println(file.getUniqueKey());
			}
		}
	}
}
