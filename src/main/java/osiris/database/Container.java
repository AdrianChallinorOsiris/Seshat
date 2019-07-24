package osiris.database;

import java.io.Serializable;
import java.util.ArrayList;

import lombok.Data;

@Data
public class Container implements Serializable {
	private static final long serialVersionUID = 1L;


	private long size;
    private long fileCount;
    private String containerName;
    private boolean uploaded = false;
    private byte[] salt;
    private char[] password;
 
    private ArrayList<FileVersion> versions;
    
    public Container() {
    	size = 0;
    }
     
    public void addVersion(FileVersion fv) { 
    	if (versions == null)
    		versions = new ArrayList<FileVersion>();
    	versions.add(fv);
    }

	public ArrayList<VersionQuery> getVersionList() {
		ArrayList<VersionQuery> contents = new ArrayList<VersionQuery>();
		for(FileVersion v : versions) {
			VersionQuery vq = v.getVersionQuery();
			contents.add(vq);
		}
		return contents;
	}
}
