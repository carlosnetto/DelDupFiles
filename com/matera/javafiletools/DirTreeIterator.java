package com.matera.javafiletools;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Stack;

class DirTreeIterator implements Iterator<File> {

	//
	// Iterator for the current level of the tree
	//
	private Iterator<File> currentLevelFiles = null;

	//
	// Used to save the currentLevelFiles when I navigate into a directory
	//
	private Stack<Iterator<File>> previousLevelsFiles = null;

	//
	// Follow Symbolic Link?
	//
	boolean follow = false;

	DirTreeIterator(File file) {
		dirTreeIterator(file);
	}

	DirTreeIterator(File file, boolean followSymLink) {
		follow = followSymLink;
		dirTreeIterator(file);
	}

	void dirTreeIterator(File file) {
		ArrayList<File> fileArrayList = new ArrayList<File>();
		fileArrayList.add(file);
		currentLevelFiles = fileArrayList.iterator();
		previousLevelsFiles = new Stack<Iterator<File>>();
	}

	public boolean hasNext() {
		//
		// TODO: e se o Primeiro item aqui já é um symlink inválido ou um diretório vazio sem mais nada??
		// o this.hasNext() deveria retornar false; Tem que bolar uma forma do hasNext() pular tudo que não presta.
		// Talvez o "hasNext()" possa executar o next() e guardar numa variável local o resultado. Depois, quando o
		// next() for chamado, devolve o que está na variável e a anula; se já estiver nula, faz o next() de fato.
		//
		return currentLevelFiles.hasNext();		
	}

	public File next() {
		File returnFile = currentLevelFiles.next();
		//
		// If the returnFile is a directory, push the current directory in the
		// stack
		// and places a new Iterator in this new directory (returnFile)
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
		// I have to leave "next" with a valid return for the next "next()" call
		// to this class. If the currentLevelFiles has no Next (!hasNext()), I
		// have to pop previous currentLevelFiles from the stack until I find
		// one
		// which has more elements yet.
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
