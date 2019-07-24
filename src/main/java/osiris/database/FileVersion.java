package osiris.database;


import lombok.*;
import lombok.extern.log4j.Log4j2;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.UserPrincipal;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.security.Principal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Set;

@Data
@Log4j2
public class FileVersion implements Serializable {
	private static final long serialVersionUID = 1L;
    private LocalDateTime backupDT;
    private LocalDateTime modifiedDT;
    private LocalDateTime createdDT;
    private LocalDateTime lastAccessDT;

    private Long size;
    private BackupFile buf;
    private long savedsize;
    private long offset;
    private byte[] iv;
	private Set<PosixFilePermission> perms;
    private String ownerName;
    private String groupName;

    private Container container; 
    
    public FileVersion() { } 
    public FileVersion(BackupFile buf) { 
 		File f = new File(buf.getName());
		Path path = f.toPath();
   	
		
		try {
			Principal group = Files.readAttributes(path, PosixFileAttributes.class, LinkOption.NOFOLLOW_LINKS).group();
			Principal owner = Files.readAttributes(path, PosixFileAttributes.class, LinkOption.NOFOLLOW_LINKS).owner();
			perms =  Files.getPosixFilePermissions(path, LinkOption.NOFOLLOW_LINKS);

			groupName = group.getName();
			ownerName = owner.getName();

	    	backupDT = LocalDateTime.now();
	    	
	    	BasicFileAttributes attr = Files.readAttributes(path, BasicFileAttributes.class);
	    	modifiedDT = ldt(attr.lastModifiedTime());
	    	createdDT = ldt(attr.creationTime());
	    	lastAccessDT = ldt(attr.lastAccessTime());
	     	size = attr.size();
		} catch (IOException e) {
			log.error("Error getting file attributes on {} - {}", buf.getName(), e.getMessage());
		}
		
    }
    
	public void setDetails() {
		File f = new File(buf.getName());
		Path path = f.toPath();
		try {
			UserPrincipalLookupService lookupService = FileSystems.getDefault().getUserPrincipalLookupService();
			UserPrincipal op = lookupService.lookupPrincipalByName(ownerName);
			GroupPrincipal  gp = lookupService.lookupPrincipalByGroupName(groupName);
			Files.setOwner(path, op);
			Files.setPosixFilePermissions(path, perms);
			Files.getFileAttributeView(path, PosixFileAttributeView.class, LinkOption.NOFOLLOW_LINKS).setGroup(gp);
			Files.setAttribute(path, "lastAccessTime", ft(lastAccessDT));
			Files.setAttribute(path, "lastModifiedTime", ft(modifiedDT));
			Files.setAttribute(path, "creationTime", ft(createdDT));
			
		} catch (IOException e) {
			log.error("Error setting file attributes on {} - {}", buf.getName(), e.getMessage());
		}
	}

    private  LocalDateTime ldt(FileTime ft) { 
    	long millis = ft.toMillis();
    	LocalDateTime ldt =
    		    LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault());
    	return ldt;
    }
    
    private FileTime ft(LocalDateTime ldt) {
    	long millis = ldt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    	return FileTime.fromMillis(millis);
    }
    
    public String toString() { return backupDT.toString(); }
        
	public VersionQuery getVersionQuery() {
		return new VersionQuery(buf, this);
	}
	
    

}
