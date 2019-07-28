package osiris.action;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import osiris.Config;

import osiris.database.*;



@Log4j2
@Getter
@Setter
public class Lister {

	private Host host;
	private TimeStamp timeStamp;
	private Config config;
	private Folder folder;
	private LocalDateTime restorePoint = LocalDateTime.now();


	private ArrayList<Folder> folderList;
	private ArrayList<TimeStamp> backupList;
	private ArrayList<VersionQuery> versionList;
	private ArrayList<VersionQuery> restoreList;
	

	public Lister(Config config) {
		this.config = config;
		selectReset();
	}

	public void listBackups() {
		if (host == null) {
			log.warn("No host selected");
			return;
		}
		getBackupList();
		for (int i = 0; i < backupList.size(); i++) {
			TimeStamp t = backupList.get(i);
			String s = String.format("\t%2d\t%30s", i + 1, t.getBackupDate());
			System.out.println(s);
		}
	}

	private void getBackupList() {
		if (backupList == null) {
			backupList = new ArrayList<TimeStamp>();
			for (TimeStamp b : host.getBackupTimeStamps()) {
				backupList.add(b);
			}

			// Sorted so that he newest is on top
			Collections.sort(backupList, new Comparator<TimeStamp>() {
				@Override
				public int compare(TimeStamp bts1, TimeStamp bts2) {

					return bts2.getBackupDate().compareTo(bts1.getBackupDate());
				}
			});
			log.debug("Refreshed backup list. Size={}", backupList.size());
		}
	}

	private int min(int l1) {
		int l2 = config.getListLimit();
		return l1 < l2 ? l1 : l2;
	}

	public void listFolders() {
		if (folderList == null) {
			log.warn("No host selected or no further folders");
			return;
		}
		
		// Sorting
		Collections.sort(folderList, new Comparator<Folder>() {
		        @Override
		        public int compare(Folder f2, Folder f1)
		        {
		            return  f1.getName().compareTo(f2.getName());
		        }
		    });

		// Print 
		int i = 1;
		for (Folder f : folderList) {
			String s = String.format("\t%2d\t%-40s\t%s", i++, f.getName(), f.getLastSeen().toString());
			System.out.println(s);
		}
	}
	
	public void disable(boolean state) { 
		if (folder == null)  
			log.warn("No folder selected");
		else {
			if (folder instanceof AutoFolder) {
				AutoFolder af = (AutoFolder)folder;
				af.setDisabled(state);
				log.info("Autobackup of {} set to {}.  Remember to save the DB", af.getName(), state);
			}
			else
				log.warn("Can only process top level folders");
		}
	}
	
	public void listFound() { 
		if (versionList == null) { 
			log.warn("No search completed");
			return;
		}
		versionLister(versionList);
	}
	
	public void selectAll() { 
		if (checkSelectList()) { 
			restoreList.addAll(versionList);
		}
	}

	// Called to list contents of a folder 
	public void listFiles(boolean tree) {
		if (folder == null) { 
			log.warn("No folder selected");
			return;
		}
		versionList = folder.getContents(tree);
		versionLister(versionList);
	}
	
	
	// Called to list file versions on a backup
	public void listVersions() {
		if (timeStamp == null) {
			log.warn("No backup timestamp selected");
			return;
		}
		
		versionList = timeStamp.getVersionList();
		
		getVersionList();
		versionLister(versionList);
	}

	public void fileContains(String s1) {
		versionList = host.getFileContains(s1);

		log.info("Refreshed version list based on file name. Size {} ", versionList.size());
	}

	public void fileRegex(String s1) {
		versionList = host.getFileRegex(s1);

		log.info("Refreshed version list. Size {} ", versionList.size());
	}

	public void listSelected() {
		if (restoreList == null)
			log.warn("No files selected");
		else
			versionLister(restoreList);
	}

	private void versionLister(ArrayList<VersionQuery> s) {
		if (s == null) { 
			log.warn("No versions selected. Did you select a backup?");
			return;
		}
		
		if (s.isEmpty()) { 
			log.warn("No files associated with backup - run DB Cleanup");
			return;
		}
		
		// Sort the files by name ASC and then by date DESC
		Collections.sort(s, new Comparator<VersionQuery>() {
			@Override
			public int compare(VersionQuery v1, VersionQuery v2) {
				int sort = v1.getFilePath().compareTo(v2.getFilePath());
				if (sort == 0) 
					sort = v2.getBackupDate().compareTo(v1.getBackupDate());
				return sort;
			}
		});
		
		
		int maxLen = 0; 
		for (VersionQuery q : s) {
			maxLen = (maxLen > q.getFilePath().length()) ? maxLen : q.getFilePath().length();
		}
		String fmt = "\t %2s \t %-" + String.valueOf(maxLen) + "s \t %-19s \t %-19s \t %10s \n";
		log.warn("Listing first {} Files", min(s.size()));
		
		System.out.printf(fmt, "id", "Path", "Modified", "Backup Date", "Size");
		System.out.printf(fmt, "--", "----", "--------", "-----------", "----");
		for (int i = 0; i < min(s.size()); i++) {
			VersionQuery v = s.get(i);
			String id = String.valueOf(i+1);
			String mDate = StringUtils.left(v.getBackupDT(), 19).replace('T', ' ');
			String bDate = StringUtils.left(v.getBackupDT(), 19).replace('T', ' ');
			
			System.out.printf(fmt, id, v.getFilePath(), mDate, bDate,  String.valueOf(v.getSize()));
		}
	}

	public void fileReset() {
		restoreList = null;
	}

	public void fileAll() {
		if (checkSelectList())
			restoreList.addAll(versionList);
	}

	private boolean checkSelectList() {
		boolean process = true;
		if (versionList == null) {
			log.warn("No file version list to select from");
			process = false;
		} else if (restoreList == null)
			restoreList = new ArrayList<VersionQuery>();

		return process;
	}

	public void selectFileNumber(int id) {
		if (checkSelectList())
			if (id - 1 < 0 || id > versionList.size()) {
				log.warn("version ID {} - out of range", id);
			} else {
				restoreList.add(versionList.get(id - 1));
			}
	}

	public void selectReset() {
		host = null;
		folder = null;
	}

	public void selectFolderNumber(int id) {
		if (folderList == null) {
			log.warn("No host or no more subfolders selected");
			return;
		}
		if (id - 1 < 0 || id > folderList.size()) {
			log.warn("folder ID {} - out of range", id);
		} else {
			folder = folderList.get(id - 1);
			if (folder.getFolders() != null)
					folderList = new ArrayList<Folder> (folder.getFolders());
			else 
				folderList = null;
			System.out.println("Selected folder : " + folder.getName());
			versionList = null;
		}
	}

	public void selectFolderName(String foldername) {
		if (folderList == null) {
			log.warn("No host or no more subfolders selected");
			return;
		}
		folder = null;
		for (Folder f : folderList) {
			if (f.getName().equalsIgnoreCase(foldername)) {
				if (folder.getFolders() != null)
					folderList = new ArrayList<Folder> (folder.getFolders());
			else 
				folderList = null;
				break;
			}
		}

		if (folder == null)
			log.info("folder {} not found. ", foldername);
		else
			System.out.println("Selected folder : " + folder.getName());
	}


	public void selectHostNumber(int id) {
		selectReset();
		Database db = config.getDb();
		ArrayList<Host> hostList = db.getHosts();
		if (id - 1 < 0 || id > hostList.size()) {
			log.warn("host ID {} - out of range", id);
		} else {
			host = hostList.get(id - 1);
			folderList = new ArrayList<Folder>(host.getFolders());
			System.out.println("Selected host : " + host.getName());
		}
	}

	public void selectHostName(String hostname) {
		Database db = config.getDb();
		ArrayList<Host> hostList = db.getHosts();
		for (Host h : hostList) {
			if (h.getName().equalsIgnoreCase(hostname)) {
				host = h;
				folderList = new ArrayList<Folder>(host.getFolders());
				break;
			}
		}

		if (host == null)
			log.info("Host {} not found. ", hostname);
		else
			System.out.println("Selected host : " + host.getName());
	}


	public void listHosts() {
		Database db = config.getDb();
		ArrayList<Host> hostList = db.getHosts();
		for (int i = 0; i < min(hostList.size()); i++) {
			Host h = hostList.get(i);
			String s = String.format("\t%2d\t%20s", i + 1, h.getName());
			System.out.println(s);
		}
	}

	public void selectBackupDate(String sdfval) {
		log.warn("Unimplemented = select backup by date");
	}

	public void selectBackupNumber(int id) {
		if (host == null) {
			log.warn("No host selected");
			return;
		}
		getBackupList();
		if (id - 1 < 0 || id > backupList.size()) {
			log.warn("Backup ID {} - out of range", id);
		} else {
			timeStamp = backupList.get(id - 1);
			System.out.println("Selected Backup  : " + timeStamp.getBackupDate());
		}
	}

	public void listAuto() {
		ArrayList<Host> hosts = config.getDb().getHosts() ;
		String fmt = "\t%20s \t%30s \t%10s";
		System.out.println(String.format(fmt, "HostName", "Folder", "Disabled"));
		System.out.println(String.format(fmt, "========", "======", "========"));
		if (hosts != null) {
			for (Host h : hosts) {
				String hn = h.getName();
				ArrayList<AutoFolder> folders = h.getFolders();
				if (folders != null) {
					for (AutoFolder f : folders) {
						String fn = f.getName();
						System.out.println(String.format(fmt, hn, fn, f.isDisabled() ? "Yes":"No"));
					}
				}
			}
		} 
	}
	
	public void getFilesInFolder( boolean recurse ) { 
		getFilesInFolder(folder, recurse);
	}
	
	private void getFilesInFolder(Folder f, boolean recurse) { 
		log.debug("Getting files in folder: {} ", f.getName());
		
		getVersionsByDate(f);
		
		if (recurse) { 
			for (Folder sub : f.getFolders()) { 
				getFilesInFolder(sub, recurse);
			}
		}
	}

	private void getVersionsByDate(Folder f) {
		for (BackupFile buf : f.getFiles()) {
			if (buf.getLastSeen().isAfter(restorePoint)) {
				FileVersion version = null;
				for (FileVersion fv : buf.getVersions()) {
					if (fv.getModifiedDT().isBefore(restorePoint)) {
						if (version == null) 
							version = fv;
						else
							if (fv.getModifiedDT().isAfter(version.getModifiedDT()))
								version = fv;
					}
				}
				
				// Add version to the restore list 
				
				restoreList.add(version.getVersionQuery());
			}
			
		}
	}
	
}
