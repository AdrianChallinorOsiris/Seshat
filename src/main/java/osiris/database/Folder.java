package osiris.database;

import java.io.File;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.regex.Pattern;

import lombok.Data;
import lombok.extern.log4j.Log4j2;

@Data
@Log4j2
public class Folder implements Serializable {
	private static final long serialVersionUID = 1L;
	protected String name;
	protected LocalDateTime lastSeen;
	protected int depth;

	private ArrayList<Folder> folders;
	private ArrayList<BackupFile> files;

	public Folder(String name) {
		this.name = name;
		lastSeen = LocalDateTime.now();
	}

	public void add(Folder f) {
		if (folders == null)
			folders = new ArrayList<Folder>();
		folders.add(f);
	}

	public void add(BackupFile f) {
		if (files == null)
			files = new ArrayList<BackupFile>();
		files.add(f);
	}

	public Folder findFolder(File file) {
		String fname = file.getAbsolutePath();
		Folder f = null;
		if (folders == null) {
			folders = new ArrayList<Folder>();
		} else {
			f = folders.stream().filter(o -> o.getName().equals(fname)).findFirst().orElse(null);
		}
		if (f == null) {
			f = new Folder(fname);
			folders.add(f);
			log.debug("Created folder  {} in {}", fname, name);
		}
		return f;
	}

	public BackupFile findFile(File file) {
		BackupFile f = null;
		if (files == null) {
			files = new ArrayList<BackupFile>();
		} else {
			f = files.stream().filter(o -> o.getName().equals(file.getAbsolutePath())).findFirst().orElse(null);
		}
		if (f == null) {
			f = new BackupFile(file);
			files.add(f);
			log.debug("Created file  {} in {}", file.getAbsoluteFile(), name);
		}
		return f;
	}

	public void updateLastSeen() {
		lastSeen = LocalDateTime.now();
	}

	public ArrayList<VersionQuery> getContents(boolean tree) {
		ArrayList<VersionQuery> contents = new ArrayList<VersionQuery>();

		// Add the files in this folder
		if (files != null) {
			for (BackupFile f : files) {
				contents.addAll(f.getFileVersions());
			}
		}

		// If requested, recurse to add the folders in this folder
		if (tree) {
			for (Folder f : folders) {
				contents.addAll(f.getContents(true));
			}
		}
		return contents;
	}

	public ArrayList<VersionQuery> findFileNameContains(String s1) {
		ArrayList<VersionQuery> contents = new ArrayList<VersionQuery>();

		// Add the files in this folder

		for (BackupFile f : files) {
			contents.addAll(f.findFileNameContains(s1));
		}

		for (Folder f : folders) {
			contents.addAll(f.findFileNameContains(s1));
		}

		return contents;
	}

	public ArrayList<VersionQuery> findFileNameRegex(String s) {
		Pattern p = Pattern.compile(s);
		return findFileNameRegex(p);
	}

	private ArrayList<VersionQuery> findFileNameRegex(Pattern p) {
		ArrayList<VersionQuery> contents = new ArrayList<VersionQuery>();

		// Add the files in this folder

		for (BackupFile f : files) {
			contents.addAll(f.findFileNameRegex(p));
		}

		for (Folder f : folders) {
			contents.addAll(f.findFileNameRegex(p));
		}

		return contents;
	}

	public void reportSize(Size size) {
		if (files != null) {
			size.addFiles(files.size());
		}
		
		if (folders != null) {
			for (Folder f: folders) {
				f.reportSize(size);
			}
		}
	}

}
