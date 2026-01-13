# DelDupFiles

**DelDupFiles** is a command-line utility designed to help you organize your file archives by detecting and deleting duplicate files from a source directory based on the contents of an "Official" repository.

## The Problem it Solves

Imagine you find a folder on an old computer, a flash drive, or a CD full of pictures and documents. You want to merge these files into your main storage (your "Official Repository"), but you suspect many of them are already there.

If you simply copy them over, you'll end up with duplicates taking up space. If you try to check them manually, it takes forever.

**DelDupFiles** solves this by comparing your "found" folder (New Files) against your Official Directory.
*   If a file in the "New Files" folder is a duplicate of something in the "Official Directory", it is **deleted**.
*   If a file remains in the "New Files" folder after the process, it means it is **unique** (new stuff) and can be safely moved to your repository.

## How It Works

To ensure high performance, even with large files, the tool uses a **Lazy Comparison Strategy**:

1.  **Indexing**: It first scans the "Official Directory" and builds an in-memory index.
2.  **Fast Check**: When checking a "New File", it compares the file size and the CRC32 checksum of the **first 64KB**.
3.  **Deep Verification**: Only if the fast check matches does it read the entire file to calculate the full CRC32 checksum. This confirms identity before any deletion occurs.

## Features

*   **Recursive Scanning**: Traverses deep directory structures.
*   **ZIP Support (`-z`)**: Can look *inside* `.zip` files in the Official Directory to find duplicates (e.g., if your official repo has archived zips, it can still detect if a loose file matches an entry inside a zip).
*   **Symbolic Links (`-l`)**: Option to follow symbolic links.
*   **Batch Mode (`-y`)**: Can delete duplicates automatically without prompting.
*   **Safety**: It **never** modifies the Official Directory. It only deletes files from the "New Files" directory provided as the second argument.

## Usage

```bash
java com.matera.javafiletools.DelDupFiles [options] <Official Directory> <Directory with New Files>
```

### Options

*   `-y`: **Yes to all**. Delete all duplicated files automatically without asking the user.
*   `-z`: **Enter Zip**. Look inside `.zip` files when indexing the Official Directory.
*   `-l`: **Links**. Follow Symbolic links during traversal.
*   `-d`: **Dump**. Dump the index of the Official Directory to stdout in CSV format (Filename, Size, Partial CRC, Date). *Note: When using -d, do not provide a second directory argument.*

### Examples

**1. Interactive Mode (Safest)**
Compare `found_photos` against `my_backup`. The program will ask `Delete? (y/Y/n/N)` for every match.
```bash
java com.matera.javafiletools.DelDupFiles /mnt/data/my_backup /media/usb/found_photos
```

**2. Batch Mode**
Automatically delete anything in `temp_download` that already exists in `work_archive`.
```bash
java com.matera.javafiletools.DelDupFiles -y /home/user/work_archive /home/user/temp_download
```

**3. Scan Zips**
Your official backup contains zip files. You want to check if loose files in `new_stuff` are already inside those zips.
```bash
java com.matera.javafiletools.DelDupFiles -z /mnt/backup /home/user/new_stuff
```

**4. Dump Index**
Generate a CSV list of files in your official directory.
```bash
java com.matera.javafiletools.DelDupFiles -d /mnt/data/my_backup > index.csv
```

## Author

**Carlos Netto** - carlos.netto@gmail.com
