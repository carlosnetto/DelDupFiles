/*
 * DelDupFiles
 * Author: Carlos Netto - carlos.netto@gmail.com
 *
 * This program detects and deletes duplicate files by comparing a new directory against an official directory.
 *
 * Design Note: CRC32 is used instead of MD5 because ZIP files already contain the pre-calculated CRC32.
 * This avoids the need to read/decompress the entire file content when checking ZIP entries.
 */
package com.matera.javafiletools;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Iterator;
import java.util.LinkedList;

class DelDupFiles {
	static public void main(String args[]) {
		boolean deleteAll = false;
		boolean enterZip = false;
		boolean followSymLink = false;
		boolean dumpOfficial = false;
		File officialDir = null;
		File newFilesDir = null;

		System.err.println("DelDupFiles v0.4 - BETA - (Nov-8th, 2014)");
		System.err.println("(c) Carlos Netto");
		int i = 0;

		//
		// Parse command line arguments
		//
		while (i < args.length && args[i].toString().charAt(0) == '-') {
			switch (args[i++].toString().charAt(1)) {
			case 'y':
				deleteAll = true;
				break;
			case 'z':
				enterZip = true;
				break;
			case 'l':
				followSymLink = true;
				break;
			case 'd':
				dumpOfficial = true;
				break;
			}
		}

		//
		// Validate arguments and print usage if incorrect
		//
		if ((dumpOfficial && args.length - i != 1) || (!dumpOfficial && args.length -i != 2)) {
			System.err.println("Usage: DupFiles [-yzl] <Official Directory> <Directory with New Files>");
			System.err.println("       DupFiles -d [-zl] <Official Directory>");
			System.err.println(" -y: delete all duplicated files without asking the user");
			System.err.println(" -z: enter .zip files while navigating official directory");
			System.err.println(" -l: follow Symbolic links");
			System.err.println(" -d: dump official directory index to stdout as CSV (filename, size, crc32_64k, date)");
			System.exit(-1);
		} else {
			officialDir = new File(args[i++]);
			if (!dumpOfficial)
				newFilesDir = new File(args[i++]);
		}

		//
		// Build the index (Hash) for the <Official Directory>
		//
		System.err.println("Reading files under " + officialDir);
		FileHashMap officialHashMap = new FileHashMap(new DirTreeIterator(officialDir, followSymLink), enterZip);

		//
		// If dump mode is enabled, print the official directory index to stdout and exit
		//
		if (dumpOfficial) {
			DateFormat formatter = new SimpleDateFormat("dd/MM/yyyy-H:M:S");
			Calendar calendar = Calendar.getInstance();
			Iterator<LinkedList<FileZipEntry>> ill = officialHashMap.values().iterator();
			while (ill.hasNext()) {
				for (FileZipEntry fileZip : ill.next()) {
					calendar.setTimeInMillis(fileZip.getDate());
					System.out.println("\"" + fileZip.toString() + "\","
							+ fileZip.length() + "," + fileZip.getCrc32_64k() + ","
							+ formatter.format(calendar.getTime()));
				}
			}
			System.exit(0);
		}

		//
		// Build the index (Hash) for the "NewFiles" directory to be checked
		//
		System.err.println("Reading files under " + newFilesDir);
		FileHashMap newFilesHashMap = new FileHashMap(new DirTreeIterator(newFilesDir, followSymLink));

		//
		// Iterate through every new file to check if it already exists in the Official Directory
		//
		System.err.println("Find duplicated files in " + newFilesDir);
		byte readline[] = new byte[512];
		Iterator<LinkedList<FileZipEntry>> newFilesLinkedListIterator = newFilesHashMap.values().iterator();
		while (newFilesLinkedListIterator.hasNext()) {
			for (FileZipEntry newFileZip : newFilesLinkedListIterator.next()) {
				LinkedList<FileZipEntry> dupLinkedList = officialHashMap.get(newFileZip);
				boolean duplicated = false;
				//
				// Lazy Strategy:
				// 1. Calculate CRC32 for the first 64k bytes (and file size) for a fast check.
				// 2. If they don't match, they are not the same file.
				// 3. If they match, scan the entire file to compare the full CRC32.
				//
				// If a potential match is found based on the unique key (size + partial CRC)
				if (dupLinkedList != null) {
					System.out.println("File "	+ newFileZip.getFile().toString());
					System.out.println("Size:" + newFileZip.length()
							+ " CRC32:" + newFileZip.getCrc32()
							+ " is duplicated at:");
					for (FileZipEntry dupFileZip : dupLinkedList) {
						System.out.println(" => " + dupFileZip.getFile().toString());
						System.out.println("Size:" + dupFileZip.length() + " CRC32:" + dupFileZip.getCrc32());
						// Perform full CRC32 check to confirm duplication
						if (newFileZip.getCrc32().equals(dupFileZip.getCrc32()))
							duplicated = true;
					}
					if (!duplicated) {
						System.out.println("Uppss!!! It's not duplicated actually!");
					} else {
						// Handle deletion (either automatic or interactive)
						try {
							if (deleteAll) {
								newFileZip.getFile().delete();
							} else {
								System.out.print("Delete? (y/Y/n/N) :");
								int bytesRead = System.in.read(readline, 0, readline.length);
								if (bytesRead < 0) {
									System.err.println("End of input stream detected. Exiting.");
									System.exit(0);
								}
								if (bytesRead > 0) {
									char response = (char) readline[0];
									if (response == 'y' || response == 'Y') {
										newFileZip.getFile().delete();
										if (response == 'Y') deleteAll = true;
									}
									if (response == 'N') break;
								}
							}
						} catch (IOException e) {
							e.printStackTrace();
						}
					}

				}
			}
		}
	}
}
