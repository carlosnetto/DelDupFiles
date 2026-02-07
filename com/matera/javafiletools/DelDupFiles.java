/*
 * DelDupFiles
 * Author: Carlos Netto - carlos.netto@gmail.com
 *
 * LICENSE: This software is 100% open source. You can use, modify, copy, 
 * or distribute it as you wish without limitations.
 *
 * NO WARRANTY: THIS SOFTWARE IS PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND.
 * THE AUTHOR IS NOT LIABLE FOR ANY DATA LOSS (INCLUDING VALUABLE PICTURES).
 * ALWAYS MAKE BACKUPS!
 *
 * This program detects and deletes duplicate files by comparing a new directory 
 * against an "Official" directory (the repository of truth).
 *
 * Logic Overview:
 * 1. Build an index (FileHashMap) of the Official Directory.
 * 2. If '-z' is enabled, the indexer opens ZIP files and indexes their entries as if 
 *    they were loose files.
 * 3. Iterate through the "New Files" directory.
 * 4. For each new file, query the index using a "Lazy Strategy":
 *    - Initial lookup uses a key composed of (FileSize + CRC32 of first 64KB).
 *    - If a match is found in the Map, a full CRC32 check of the entire file 
 *      is performed to confirm identity.
 * 5. Handle duplicates based on user input (Interactive) or '-y' (Automatic).
 */
package com.matera.javafiletools;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.Scanner;

class DelDupFiles {
	/**
	 * Main entry point. Parses CLI arguments and executes the comparison logic.
	 */
	static public void main(String args[]) {
		var deleteAll = false;
		var enterZip = false;
		var followSymLink = false;
		var dumpOfficial = false;
		File officialDir = null;
		File newFilesDir = null;

		System.err.println("DelDupFiles v0.5 - Java 25 - (Feb-7th, 2026)");
		System.err.println("(c) Carlos Netto");
		int i = 0;

		//
		// Parse command line arguments
		//
		while (i < args.length && args[i].charAt(0) == '-') {
			var arg = args[i++];
			for (int j = 1; j < arg.length(); j++) {
				switch (arg.charAt(j)) {
					case 'y' -> deleteAll = true;     // -y: automatic deletion
					case 'z' -> enterZip = true;      // -z: index zip entries
					case 'l' -> followSymLink = true; // -l: follow symlinks
					case 'd' -> dumpOfficial = true;  // -d: CSV export mode
				}
			}
		}

		//
		// Validate arguments and print usage text block if incorrect
		//
		if ((dumpOfficial && args.length - i != 1) || (!dumpOfficial && args.length - i != 2)) {
			System.err.println("""
					Usage: DupFiles [-yzl] <Official Directory> <Directory with New Files>
					       DupFiles -d [-zl] <Official Directory>
					 -y: delete all duplicated files without asking the user
					 -z: enter .zip files while navigating official directory
					 -l: follow Symbolic links
					 -d: dump official directory index to stdout as CSV (filename, size, crc32_64k, date)
					""");
			System.exit(-1);
		} else {
			officialDir = new File(args[i++]);
			if (!dumpOfficial)
				newFilesDir = new File(args[i++]);
		}

		//
		// STEP 1: Build the index for the <Official Directory>
		// We use DirTreeIterator to walk the tree without recursion to avoid StackOverflow.
		//
		System.err.println("Reading files under " + officialDir);
		var officialHashMap = new FileHashMap(new DirTreeIterator(officialDir, followSymLink), enterZip);

		//
		// STEP 2: Handle CSV Dump mode
		// Useful for creating a searchable text snapshot of the repository.
		//
		if (dumpOfficial) {
			var formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy-HH:mm:ss")
					.withZone(ZoneId.systemDefault());
			for (var entries : officialHashMap.values()) {
				for (var fileZip : entries) {
					var dateStr = formatter.format(Instant.ofEpochMilli(fileZip.getDate()));
					System.out.println("\"" + fileZip.toString() + "\","
							+ fileZip.length() + "," + fileZip.getCrc32_64k() + ","
							+ dateStr);
				}
			}
			System.exit(0);
		}

		//
		// STEP 3: Build the index for the "NewFiles" directory.
		//
		System.err.println("Reading files under " + newFilesDir);
		var newFilesHashMap = new FileHashMap(new DirTreeIterator(newFilesDir, followSymLink));

		//
		// STEP 4: Detect Duplicates
		// We iterate over the "New Files" and check them against the "Official" index.
		//
		System.err.println("Find duplicated files in " + newFilesDir);
		var scanner = new Scanner(System.in);
		for (var entries : newFilesHashMap.values()) {
			for (var newFileZip : entries) {
				// Query the official index using the Lazy Key (Size + Partial CRC)
				var dupLinkedList = officialHashMap.get(newFileZip);
				var duplicated = false;

				// If there's a hit on the Lazy Key, we verify the full content.
				if (dupLinkedList != null) {
					System.out.println("File " + newFileZip.getFile().toString());
					System.out.println("Size:" + newFileZip.length()
							+ " CRC32:" + newFileZip.getCrc32()
							+ " is duplicated at:");
					
					for (var dupFileZip : dupLinkedList) {
						System.out.println(" => " + dupFileZip.getFile().toString());
						System.out.println("Size:" + dupFileZip.length() + " CRC32:" + dupFileZip.getCrc32());
						
						// The ultimate proof: check the full file CRC32
						if (newFileZip.getCrc32().equals(dupFileZip.getCrc32()))
							duplicated = true;
					}

					if (!duplicated) {
						System.out.println("Uppss!!! It's not duplicated actually!");
					} else {
						// STEP 5: Deletion logic
						if (deleteAll) {
							newFileZip.getFile().delete();
						} else {
							System.out.print("Delete? (y/Y/n/N) :");
							if (scanner.hasNextLine()) {
								var responseLine = scanner.nextLine().trim();
								if (!responseLine.isEmpty()) {
									var response = responseLine.charAt(0);
									if (response == 'y' || response == 'Y') {
										newFileZip.getFile().delete();
										// 'Y' (uppercase) acts as "Yes to all remaining"
										if (response == 'Y')
											deleteAll = true;
									} else if (response == 'N' || response == 'n') {
										// Skip deletion for this file
									}
								}
							} else {
								System.err.println("End of input stream detected. Exiting.");
								System.exit(0);
							}
						}
					}
				}
			}
		}
	}
}