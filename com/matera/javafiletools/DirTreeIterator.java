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
	}

	public boolean hasNext() {
		//
		// TODO: What if the first item here is already an invalid symlink or an empty directory with nothing else??
		// this.hasNext() should return false; Must figure out a way for hasNext() to skip everything that is useless.
		// Maybe "hasNext()" could execute next() and store the result in a local variable. Then, when
		// next() is called, it returns what is in the variable and clears it; if it is already null, it performs the actual next().
		//
		return currentLevelFiles.hasNext();		
	}

	public File next() {
		File returnFile = currentLevelFiles.next();
		//
		// If the current file is a directory, we need to dive into it.
		// Push the current iterator to the stack and create a new iterator
		// for the subdirectory.
		//
		if (returnFile.isDirectory()) {
			System.err.println(returnFile.toString());
			if (!returnFile.canRead()) {
				System.err.println("Warning: could not read directory " + returnFile.toString());
			} else if (Files.isSymbolicLink(returnFile.toPath()) && !follow) {
				System.err.println("Warning: Not Following " + returnFile.toString() + " Symbolic Link");
			} else {
				previousLevelsFiles.push(currentLevelFiles);
				currentLevelFiles = Arrays.asList((File[]) returnFile.listFiles()).iterator();
			}
		}

		//
		// Ensure the iterator is ready for the subsequent call.
		// If the current level is exhausted, pop from the stack to return to
		// the previous directory level until we find one with more files.
		//
		while (!currentLevelFiles.hasNext() && !previousLevelsFiles.isEmpty()) {
			currentLevelFiles = previousLevelsFiles.pop();
		}

		//
		// Now Return the result
		//
		return returnFile;
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
