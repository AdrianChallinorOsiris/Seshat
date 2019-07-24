package osiris.database;

import lombok.Data;
import lombok.extern.log4j.Log4j2;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;


@Data
@Log4j2
public class Host implements Serializable {
	private static final long serialVersionUID = 1L;
    private String name;
    private LocalDateTime createDT;
        
    private ArrayList<TimeStamp> backupTimeStamps;
    private ArrayList<AutoFolder> folders;

    public Host() { 
    	this.createDT = LocalDateTime.now();
    }
    
    public Host(String name) { 
    	this.name = name;
    	this.createDT = LocalDateTime.now();
     }
 
    public AutoFolder findFolder(String name) {
    	AutoFolder f = null;
    	if (folders == null) {
    		folders = new ArrayList<AutoFolder>();
    	} else {
    		f = folders.stream().filter(o -> o.getName().equals(name)).findFirst().orElse(null);
    	}
    	
    	if (f == null) { 
    		f = new AutoFolder(name);
    		folders.add(f);
    		log.info("Created top level folder {}", name);
    	}
    	return f;
    }

    
    public BackupFile findFile(String name) { 
    	BackupFile bf = null; 
    	
    	return bf;
    }
    
    public void addTimeStamp(TimeStamp ts){ 
    	if (backupTimeStamps == null) {
    		backupTimeStamps = new ArrayList<TimeStamp>();
    	}
    	backupTimeStamps.add(ts);
    }
    	

	public ArrayList<VersionQuery> getFileContains(String s1) {
		ArrayList<VersionQuery> aq = new ArrayList<VersionQuery>();
		
		// For each folder, check if the files are found 
		for (Folder f : folders) {
			aq.addAll(f.findFileNameContains(s1));
		}
		return aq;
	}

	public ArrayList<VersionQuery> getFileRegex(String s1) {
		ArrayList<VersionQuery> aq = new ArrayList<VersionQuery>();
		
		// For each folder, check if the files are found 
		for (Folder f : folders) {
			aq.addAll(f.findFileNameRegex(s1));
		}
		return aq;
	}

	public void reportSize(Size size) {
		if (folders != null) {
			for (Folder f : folders) {
				f.reportSize(size);
			}
		}
		
	}
}
 

