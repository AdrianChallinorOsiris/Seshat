package osiris.database;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.time.LocalDateTime;

import lombok.Data;

@Data
public class BackupFile  implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String name;
	private LocalDateTime lastSeen;
    private LocalDateTime createDT;
   
    private ArrayList<FileVersion> versions;
     
    public BackupFile() {}
    public BackupFile(File file) { 
    	this.name =  file.getAbsolutePath();
    	this.lastSeen = LocalDateTime.now();
    }
        
    public FileVersion getLatestVersion() { 
    	FileVersion fv = null; 
    	if (versions != null ) {
	    	for (FileVersion v : versions) {
	    		if (fv == null) 
	    			fv = v; 
	    		else {
	    			fv =  v.getBackupDT().isAfter( fv.getBackupDT() )  ? v : fv;
	    		}
	    	}
    	}
		return fv;
    }
    
    public void addVersion(FileVersion fv) { 
    	if (versions == null)
    		versions = new ArrayList<FileVersion>();
    	versions.add(fv);
    }
    
	public void updateLastSeen() {
		lastSeen = LocalDateTime.now();
	}
	public ArrayList<VersionQuery> getFileVersions() {
		ArrayList<VersionQuery> aq = new ArrayList<VersionQuery>();
		if (versions == null) return aq; 
		
		for (FileVersion v : versions) {
			VersionQuery vq = new VersionQuery(this, v);
			aq.add(vq);
		}
		return aq;
	}
	
	public ArrayList<VersionQuery> findFileNameContains(String s) {
		ArrayList<VersionQuery> aq = new ArrayList<VersionQuery>();
		if (versions == null) return aq; 
		if (name.contains(s)) {
			for (FileVersion v : versions) {
				VersionQuery vq = new VersionQuery(this, v);
				aq.add(vq);
			}
		}
		return aq;
	}

	
	
	public ArrayList<VersionQuery> findFileNameRegex(Pattern p) {
		ArrayList<VersionQuery> aq = new ArrayList<VersionQuery>();
		if (versions == null) return aq; 
		Matcher m = p.matcher(name);
		if (m.matches()) {
			for (FileVersion v : versions) {
				VersionQuery vq = new VersionQuery(this, v);
				aq.add(vq);
			}
		}
		return aq;
	}
	
}
