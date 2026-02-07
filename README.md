# DelDupFiles

**DelDupFiles** is a specialized command-line utility for managing large file archives. It identifies and removes duplicate files from a source directory by comparing them against an "Official" repository of truth.

## The Problem it Solves

When merging old backups, photo collections, or disparate file folders into a main repository, you often encounter duplicates. 
*   **Manual checking** is impossible for thousands of files.
*   **Simple file-name matching** fails because different files often share the same name, or identical files have different names.
*   **Full byte-by-byte comparison** of every file is extremely slow.

**DelDupFiles** solves this by providing a high-performance, content-aware comparison that is safe and intelligent.

## High-Performance Logic

To handle large volumes of data efficiently, the tool employs three core design patterns:

### 1. Lazy Comparison Strategy (Two-Tier Check)
Instead of calculating the full hash of every file immediately, it uses a tiered approach:
*   **Tier 1 (Fast):** Compares the **File Size** and the **CRC32 of the first 64KB**. This acts as a "Lazy Key". If these don't match, the files are guaranteed to be different.
*   **Tier 2 (Verification):** Only if the Tier 1 check matches does the tool perform a full CRC32 scan of the entire file. This confirms identity before any deletion occurs.

### 2. ZIP Optimization
If you use the `-z` flag, the tool indexes the contents of `.zip` files. Because the ZIP format natively stores the CRC32 of every entry, **DelDupFiles** can compare files against zipped content **without decompressing or even reading** the zipped data. This makes comparison against archives nearly instantaneous.

### 3. Iterative Traversal
The directory walker is implemented using a custom **Stack-based iterator** (`DirTreeIterator`) rather than standard recursion. This ensures the program can handle extremely deep directory structures without ever triggering a `StackOverflowError`.

## Features

*   **Content-Based**: Uses CRC32 signatures, not just filenames.
*   **ZIP Awareness (`-z`)**: Detects if a loose file is already archived inside a ZIP file.
*   **Symbolic Link Support (`-l`)**: Customizable behavior for symlinks.
*   **Automatic Mode (`-y`)**: Batch processing for large cleanups.
*   **CSV Export (`-d`)**: Generates a snapshot of your repository for auditing or external analysis.
*   **Modern Java**: Leverages **Java 25** features (var, text blocks, modern switch) for performance and readability.

## Requirements

*   **Java 25** or higher.
*   **Maven** (to build from source).

## Usage

```bash
java com.matera.javafiletools.DelDupFiles [options] <Official Directory> <Directory with New Files>
```

### Options

*   `-y`: **Yes to all**. Delete all duplicated files automatically without asking the user.
*   `-z`: **Enter Zip**. Look inside `.zip` files when indexing the Official Directory.
*   `-l`: **Links**. Follow Symbolic links during traversal.
*   `-d`: **Dump**. Exports the Official Directory index to stdout in CSV format.

## Examples

**1. Interactive Cleanup**
Compare `new_photos` against your `main_gallery`. The program will prompt for each deletion.
```bash
java com.matera.javafiletools.DelDupFiles /mnt/storage/main_gallery /media/usb/new_photos
```

**2. Automated Archive Deduplication**
Identify files in `downloads` that are already stored (even inside ZIPs) in your `archives`.
```bash
java com.matera.javafiletools.DelDupFiles -yz /mnt/archives /home/user/downloads
```

## License and No Warranty

This software is **100% open source**. You are free to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the software as you wish, with no limitations.

**DISCLAIMER: THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND.**

In no event shall the author be liable for any claim, damages, or other liability, including but not limited to the **loss of data (valuable pictures, documents, etc.)**. If you lose files using this tool, the author is not responsible. 

**CRITICAL ADVICE: ALWAYS MAKE BACKUPS before running this tool.**

## Author

**Carlos Netto** - carlos.netto@gmail.com