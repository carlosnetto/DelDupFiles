/*
 * DirTreeIterator
 * Author: Carlos Netto - carlos.netto@gmail.com
 *
 * LICENSE: This software is 100% open source. You can use, modify, copy, 
 * or distribute it as you wish without limitations.
 *
 * NO WARRANTY: THIS SOFTWARE IS PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND.
 * THE AUTHOR IS NOT LIABLE FOR ANY DATA LOSS (INCLUDING VALUABLE PICTURES).
 * ALWAYS MAKE BACKUPS!
 *
 * An Iterator implementation that recursively traverses a directory tree.
 * 
 * Design Note: This implementation is iterative (using a Deque as a stack) 
 * rather than recursive. This avoids StackOverflowErrors when processing 
 * extremely deep directory structures.
 */
package com.matera.javafiletools;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.Iterator;
import java.util.NoSuchElementException;

class DirTreeIterator implements Iterator<File> {

	/** Iterator for the files in the current directory level. */
	private Iterator<File> currentLevelFiles = null;

	/** Deque used as a stack to save the state of parent directories. */
	private final Deque<Iterator<File>> previousLevelsFiles = new ArrayDeque<>();

	/** Look-ahead buffer for the next file to be returned. */
	private File nextFile = null;

	/** Whether to follow Symbolic Links. */
	private boolean follow = false;

	DirTreeIterator(File file) {
		initialize(file);
	}

	DirTreeIterator(File file, boolean followSymLink) {
		this.follow = followSymLink;
		initialize(file);
	}

	/**
	 * Prepares the iterator starting from a root file or directory.
	 */
	private void initialize(File file) {
		var fileArrayList = new ArrayList<File>();
		fileArrayList.add(file);
		currentLevelFiles = fileArrayList.iterator();
		nextFile = null;
	}

	/**
	 * Scans the filesystem to find the next available file or directory.
	 * It descends into directories and pushes the current state onto the stack.
	 */
	private void findNext() {
		while (nextFile == null) {
			// If current directory is exhausted, pop the parent directory's iterator
			if (!currentLevelFiles.hasNext()) {
				if (previousLevelsFiles.isEmpty()) {
					return; // No more files anywhere
				}
				currentLevelFiles = previousLevelsFiles.pop();
				continue;
			}

			var f = currentLevelFiles.next();

			if (f.isDirectory()) {
				System.err.println(f.toString());
				if (!f.canRead()) {
					System.err.println("Warning: could not read directory " + f);
				} else if (Files.isSymbolicLink(f.toPath()) && !follow) {
					System.err.println("Warning: Not Following " + f + " Symbolic Link");
				} else {
					var files = f.listFiles();
					if (files != null) {
						// Save current level and dive into the subdirectory
						previousLevelsFiles.push(currentLevelFiles);
						currentLevelFiles = Arrays.asList(files).iterator();
					} else {
						System.err.println("Warning: could not list files in directory " + f);
					}
				}
				nextFile = f; // Return the directory itself as an entry
			} else if (Files.isRegularFile(f.toPath())) {
				nextFile = f;
			} else {
				System.err.println("Skipping special file: " + f);
			}
		}
	}

	@Override
	public boolean hasNext() {
		if (nextFile == null) {
			findNext();
		}
		return nextFile != null;
	}

	@Override
	public File next() {
		if (!hasNext()) {
			throw new NoSuchElementException();
		}
		var result = nextFile;
		nextFile = null;
		return result;
	}

	/**
	 * Simple test utility for local debugging.
	 */
	public static void main(String[] args) {
		var dt = new DirTreeIterator(new File("/tmp"), true);
		while (dt.hasNext()) {
			System.out.println(dt.next().toString());
		}
	}
}