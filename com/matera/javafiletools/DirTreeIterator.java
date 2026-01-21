/*
 * DirTreeIterator
 * Author: Carlos Netto - carlos.netto@gmail.com
 *
 * An Iterator implementation that recursively traverses a directory tree.
 */
package com.matera.javafiletools;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Stack;

class DirTreeIterator implements Iterator<File> {

	//
	// Iterator for the files in the current directory level
	//
	private Iterator<File> currentLevelFiles = null;

	//
	// Stack used to save the state of previous levels when navigating into a subdirectory
	//
	private Stack<Iterator<File>> previousLevelsFiles = null;

	//
	// Buffer for the next valid file to be returned
	//
	private File nextFile = null;

	//
	// Flag to determine if Symbolic Links should be followed
	//
	boolean follow = false;

	DirTreeIterator(File file) {
		dirTreeIterator(file);
	}

	DirTreeIterator(File file, boolean followSymLink) {
		follow = followSymLink;
		dirTreeIterator(file);
	}

	//
	// Initialize the iterator with the root file/directory
	//
	void dirTreeIterator(File file) {
		ArrayList<File> fileArrayList = new ArrayList<File>();
		fileArrayList.add(file);
		currentLevelFiles = fileArrayList.iterator();
		previousLevelsFiles = new Stack<Iterator<File>>();
		nextFile = null;
	}

	//
	// Finds the next valid file or directory, skipping special files.
	//
	private void findNext() {
		while (nextFile == null) {
			if (!currentLevelFiles.hasNext()) {
				if (previousLevelsFiles.isEmpty()) {
					return;
				}
				currentLevelFiles = previousLevelsFiles.pop();
				continue;
			}

			File f = currentLevelFiles.next();

			if (f.isDirectory()) {
				System.err.println(f.toString());
				if (!f.canRead()) {
					System.err.println("Warning: could not read directory " + f.toString());
				} else if (Files.isSymbolicLink(f.toPath()) && !follow) {
					System.err.println("Warning: Not Following " + f.toString() + " Symbolic Link");
				} else {
					File[] files = f.listFiles();
					if (files != null) {
						previousLevelsFiles.push(currentLevelFiles);
						currentLevelFiles = Arrays.asList(files).iterator();
					} else {
						System.err.println("Warning: could not list files in directory " + f.toString());
					}
				}
				nextFile = f;
			} else if (Files.isRegularFile(f.toPath())) {
				nextFile = f;
			} else {
				System.err.println("Skipping special file: " + f.toString());
			}
		}
	}

	public boolean hasNext() {
		if (nextFile == null) {
			findNext();
		}
		return nextFile != null;
	}

	public File next() {
		if (!hasNext()) {
			throw new NoSuchElementException();
		}
		File result = nextFile;
		nextFile = null;
		return result;
	}

	public void remove() {
		throw new UnsupportedOperationException();
	}

	static public void main(String args[]) {
		DirTreeIterator dt = new DirTreeIterator(new File("/tmp"), true);
		while (dt.hasNext()) {
			System.out.println(dt.next().toString());
		}
	}
}
