package osiris.action;

import lombok.extern.log4j.Log4j2;
import osiris.Config;
import osiris.database.AutoFolder;
import osiris.database.BackupFile;
import osiris.database.FileVersion;
import osiris.database.Folder;
import osiris.database.Host;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.Collection;
import java.util.Stack;

import org.apache.commons.io.DirectoryWalker;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.time.StopWatch;


@Log4j2

public class Finder extends DirectoryWalker<BackupFile> {
	private Config config;

	private Stack<Folder> stack = new Stack<Folder>();
	private Folder current;
	private int scanDir; 
	private int scanFil;
	
	private boolean followLinks;
	private boolean includeHidden;
	private StringBuilder reason;
	

	public Finder(Config config) { this.config = config; }
	
	public ArrayList<BackupFile> scan(String fname) {

		
		if (fname == null) {
			log.warn("No backup directory selected to backup");
			return null;
		}
		fname = expand(fname);

		followLinks = config.isFollowLinks();
		includeHidden = config.isIncludeHidden();
		
		StopWatch st = new StopWatch();
		st.start();
		
		if (config.isReportUnmatched())
			log.info("Reporting items not matched:");
		
		File dir = new File(fname);
		config.setBackupName(dir.getName());
		scanDir = scanFil = 0;
		
		Host host = config.getCurrentHost();
		
		AutoFolder af = host.findFolder(dir.getAbsolutePath());
		af.setExcludes(config.getExcludes());
		af.setIncludes(config.getIncludes());
		af.setIncludeHidden(config.isIncludeHidden());
		af.setFollowLinks(config.isFollowLinks());
		
		current = af;
		
		current.updateLastSeen();
		
		log.info("Starting File walk...{}", dir.getAbsolutePath());
		ArrayList<BackupFile> candidates = new ArrayList<BackupFile> ();
		try {
			walk(dir, candidates);
		} catch (IOException e) {
			log.error("Error walking dir tree: ", e); //$NON-NLS-1$
		}
		
		st.stop();
		log.info("Completed file walk. Scaned Dir={} File={}. Found: {} to backup.  elapsed: {}", scanDir, scanFil, candidates.size(), st) ;
		return candidates;
	}


	private String expand(String fname) {
		String newname = fname;
		if (fname.startsWith("~" + File.separator)) {
			newname = System.getProperty("user.home") + fname.substring(1);
		}	
		// TODO - handle Environment variable 
		return newname;
	}

	public void handleDirectoryEnd(File directory, int depth,  Collection<BackupFile> results) throws IOException {
		if (depth > 0 )
			current = stack.pop();
	}
	
	private boolean isSymLink(File directory) { 
		boolean symlink = false; 
		
		try {
			symlink = FileUtils.isSymlink(directory);
		} catch (IOException e) {
			log.error("Error handling directory : ", e); //$NON-NLS-1$
		}
		return symlink;
	}

	@Override
	protected boolean handleDirectory(File directory, int depth, Collection<BackupFile> results) {
		scanDir++;
		if (depth == 0 ) return true;

		boolean process = match(directory);
		if ( match(directory)) {
			stack.push(current);
			current = current.findFolder(directory);
			current.setDepth(depth);
		}			
		return process;
	}


	@Override
	protected void handleFile(File file, int depth,  Collection<BackupFile> results) {

		if (!file.isFile()) {
			log.debug("Non-file ignored: {}", file.getAbsolutePath());
			return;
		}
		scanFil++;
		
		boolean process = false;
		reason = new StringBuilder();

		if (match(file)) {
			BackupFile buf = current.findFile(file); 
			FileVersion v = buf.getLatestVersion();
			if (v == null) {
				process = true;
			}
			else { 
				LocalDateTime vmdt = v.getModifiedDT();
				Date in = new Date(file.lastModified());
				LocalDateTime fmdt = LocalDateTime.ofInstant(in.toInstant(), ZoneId.systemDefault());
				
				if (fmdt.isAfter(vmdt))
					process = true;
				else
					reason.append("No change to file");
			}
			
			if (process) {
				results.add(buf);
			} else 	{
				if (config.isReportUnmatched()) {

					String item = "Unknown  ";
					if (file.isDirectory()) item = "Directory";
					if (file.isFile())      item = "File     ";
					
					log.info("{} {} {} ", item, String.format("%40s", file.getAbsolutePath()), reason );
				}
			}
			
			buf.updateLastSeen();
		}
	}
	

	/**
	 * Is this file a match 
	 * 
	 * @param file
	 * @return
	 */
	
	private boolean match(File file) {
		boolean process = true;
		
		// Remove links unless we want them 
		reason = new StringBuilder();
		
		boolean symlink = isSymLink(file); 
		boolean hidden = file.isHidden();
		
		if (symlink & !followLinks) {
			process = false;
			reason.append("Symbolic Link; ");
		}
		
		if (hidden & !includeHidden) {
			process = false;
			reason.append("Hidden file; ");
		}
		
		// Remove excluded files
		if (!config.getExcludes().isEmpty()) {
			String ap = file.getAbsolutePath();
			String fn = file.getName();
			for (String s : config.getExcludes()) {
				if (fn.matches(s) || ap.matches(s)) {
					process = false;
					reason.append("Excluded; ");
				}
			}
		}
		
		// Remove generic things that consume space and can be replaced. 
		String name = file.getName();
		if (name.equalsIgnoreCase("Dropbox")) {
			log.warn("Ignoring Dropbox");
			process = false;
		}
		if (name.equalsIgnoreCase("VirtualBox VMs")) {
			process = false;
			log.warn("Ignoring Virtual Box VMs");
		}
		if (name.endsWith("iso")) {
			process = false;
			log.warn("Ignoring iso {}", name);
		}

		// Finally, if we specifically include this file hen override any excludes
		String fn = file.getAbsolutePath();
		if (!config.getIncludes().isEmpty()) {
			for (String s : config.getIncludes()) {
				if (fn.matches(s))
					process = true;
			}
		}
		
		
		return process;
	}

	/**
	 * Find out if a file is hidden, or if any of the parents are hidden
	 * 
	 * @param file
	 * @return
	 */
	@SuppressWarnings("unused")
	private boolean isHidden(File file) {
		boolean hidden = file.isHidden();
		while (hidden == false & file.getParentFile() != null) {
			file = file.getParentFile();
			hidden = file.isHidden();
		}
		return hidden;
	}
}
