package osiris;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.nustaq.serialization.FSTConfiguration;
import org.nustaq.serialization.FSTObjectInput;
import org.nustaq.serialization.FSTObjectOutput;

import lombok.Data;
import lombok.extern.log4j.Log4j2;
import osiris.action.Encryptor;
import osiris.database.BackupFile;
import osiris.database.Container;
import osiris.database.Database;
import osiris.database.Host;
import osiris.stp.Parser;

@Data
@Log4j2
public class Config {
	private boolean isFollowLinks = false;
	private boolean isIncludeHidden = false;
	private boolean isInit = false;
	private ArrayList<String> includes = new  ArrayList<String>() ;
	private ArrayList<String> excludes  = new  ArrayList<String>();
	private ArrayList<BackupFile> candidates = new ArrayList<BackupFile>();
	private ArrayList<Container> delayed;

	private String s3BucketName = "osiris-seshat";
	private String s3Region = "eu-west-2";
	private String s3DBKey = "seshat-encrypted-db";
	private String backupName;
	
	private String SESHAT_HOME;

	private String containerLocation = "/tmp/";
	private int listLimit = 10;
	private int threadLowerLimit = 100;
	private String password;
	
	
	
	// Total size of a container must not exceed 40 GB. 
	public static final long KB = 1024;
	public static final long MB = KB * 1024;
	public static final long GB = MB * 1024;
	
	private long maxSize = GB * 8;
	
	private boolean DBlocked = false;
	private boolean reportUnmatched = false;
	private boolean waitOnLock = false;
	private boolean delayUpload = false;
	private boolean dryRun = true;
	
	private Parser p;
	private Database db; 
	private S3 s3;
	
	static FSTConfiguration FSTconf = FSTConfiguration.createDefaultConfiguration();

	/*
	 * Constructor
	 */
	public Config() { 
		excludes.add("Dropbox");
		excludes.add("VirtualBox VMs");
		excludes.add(".*\\.?(i|I)(s|S)(o|O)");	
	}
	
	
	public void init(boolean warn) { 
		if (isInit) {
			if (warn)
				log.warn("Configuration already initialized");
		}
		else {
			log.info("Initializing the configuration");
			db = new Database();
			s3 = new S3(this);
			autoLoadDB();
			
			SESHAT_HOME = System.getenv("SESHAT_HOME");
			if (SESHAT_HOME != null) {
				containerLocation = SESHAT_HOME;
			} else {
				File seshat = new File("/seshat");
				if (seshat.exists() && seshat.isDirectory()) { 
					containerLocation = seshat.getAbsolutePath();
				}
				else containerLocation = "/tmp";
			} 
			log.info("Staging area is: {}", containerLocation);
			
			log.info("Initialisation is compete");
			isInit = true;
		}
	}
	
	public Database getDb() {
		init(false);
		return db;
	}
	
	public boolean lockDB() { 
		init(false);
		if (!(DBlocked = s3.lockDB())) {
			if (!waitOnLock) {
				log.error("Database not locked - backup is not possible");
				return false;
			} else {
				int wait = 1;
				while ((DBlocked = s3.lockDB())) {
					log.info("DB Locked - Waiting {} minute", wait);
					try {
						TimeUnit.MINUTES.sleep(wait);
					} catch (InterruptedException e) {
					}
					switch (wait) {
					case (1):
						wait = 2;
						break;
					case (2):
						wait = 3;
						break;
					case (3):
						wait = 5;
						break;
					case (5):
						wait = 10;
						break;
					}
				}
				log.info("DB Lock acquired - backup proceeding");
			}
		}
		
		if (!DBlocked) 
			log.warn("DB not locked - unable to perform backup");
		else {
			log.debug("DB locked");
			autoLoadDB();
		}
		return DBlocked;
	}
	
	public void unlockDB() {
		s3.unlockDB();
		DBlocked = false;
		log.debug("DB unlocked");
	}
	
	public void autoLoadDB() {
		byte[] crypt = s3.readDB();
		if (crypt != null) {
			try {
				Encryptor enc = new Encryptor();
				byte[] plain = enc.decryptDB(crypt);
				ByteArrayInputStream bais = new ByteArrayInputStream(plain);
				FSTObjectInput in = new FSTObjectInput(bais);
				db = (Database) in.readObject();
				in.close();
			} catch (Exception e) {
				log.error("Unable to load database  {}", s3DBKey, e);
			} 
			log.info("Database loaded from: S3");
		}
		else { 
			log.info("No DB to load from S3 - empty DB created");
			resetDB();
		}
	}

	public void autoSaveDB() {
		if (!DBlocked) {
			log.warn("No autosave possible: DB not locked");
			return;
		}
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			FSTObjectOutput out = new FSTObjectOutput(baos);
		    out.writeObject( db );
		    out.close(); // required !
		    baos.close();
		    byte[] plain = baos.toByteArray();
			Encryptor enc = new Encryptor();
			byte[] crypt = enc.encryptDB(plain);
			s3.writeDB(crypt);
		    unlockDB();
		    log.info("DB Saved. Lock released");
		} catch (Exception e) {
			log.error("Error autosaving the DB",  e);
		}
	}
	
	
	public void sizeDB() { 
		db.reportSize();
	}
	
	public void resetDB() { 
		log.debug("Reset DB");
		db = new Database();
		}
	
	
	public void addCandidate(BackupFile c) { candidates.add(c); }
	public void resetCandidates() { candidates.clear(); }
	
	public void setMaxSize(String  unit) { 
		long m = 0;
		unit = unit.toUpperCase();
		switch (unit) {
			case "MB" : m = 1024l * 1024l; break;
			case "GB" : m = 1024l * 1024l * 1024l; break;
		}
		maxSize *= m ;
		
		if (maxSize > GB * 40 ){
			log.warn("Maximim file size is  40GB");
			maxSize = GB * 40;
		}
	}
	
	public void setMaxSize(int i) { 
		maxSize = i;
	}
		
	public void show() {
		System.out.println("Include Hidden Files  : " + isIncludeHidden);
		System.out.println("Follow symbolic links : " + isFollowLinks);
		System.out.println("Wait on lock          : " + isWaitOnLock());
		System.out.println("Restore Dry Run       : " + isDryRun());
		System.out.println("Max container size    : " + FileUtils.byteCountToDisplaySize(maxSize));
		System.out.println("Staging folder        : " + containerLocation);
		System.out.println("S3 Region             : " + (s3Region == null ? "Not set" : s3Region));
		System.out.println("S3 Bucket             : " + (s3BucketName == null ? "Not set" : s3BucketName));

		if (includes.isEmpty())
			System.out.println("Included files        : *.*");
		else
			for (String include : includes)
				System.out.println("Include             : " + include);

		if (excludes.isEmpty())
			System.out.println("Exclude            : Nothing");
		else
			for (String exclude : excludes)
				System.out.println("Exclude               : " + exclude);
		
	}
	
	public void includeReset() { includes.clear(); }
	public void excludeReset() { excludes.clear(); }
	public void includeAdd(String s) { includes.add(s); }
	public void excludeAdd(String s) { excludes.add(s); }
	public void setIncludeHidden(boolean b) { isIncludeHidden = b; }
	public void setFollowLinks(boolean b) { isFollowLinks = b; }
	
	/**
	 * Get the current host name
	 * 
	 * @return String
	 * @throws UnknownHostException
	 */
	public static String getHostname(InetAddress ip) throws UnknownHostException {
		return ip.getHostName();
	}
	
	public static InetAddress getIP() throws UnknownHostException {
		return InetAddress.getLocalHost();
	}
	public Host getCurrentHost() {
		Host  host  = null;
		if (!isInit) init(true);

		InetAddress ip;
			try {
			ip = InetAddress.getLocalHost();
			String hostName = ip.getHostName();
			host = db.findHost(hostName);
		} catch (UnknownHostException e) {
			log.fatal("Unable to get current host name", e);
		}
		return host;
	}

	public void AddDelayedUpload(Container bc) {
		if (delayed == null)
			delayed = new ArrayList<Container>();
		delayed.add(bc);
		
		log.info("Delaying upload of container file until backup completed: {}", bc.getContainerName());
	}

	public void ProcessDelayed() {
		if (!delayUpload) return;
		if (delayed == null) return;
		if (delayed.size() == 0) return;
		
		StopWatch st = new StopWatch();
		st.start();
		log.info("Processing {} delayed uploads to S3", delayed.size());
		Iterator<Container> it = delayed.iterator();
		while (it.hasNext()) {
			Container c = it.next();
			s3.upload(c);
			it.remove();
			File file = new File(c.getContainerName());
			file.delete();
		}
		st.stop();
		log.info("Container files uploaded. Elapsed: {}", st);
		
	}

	public static String perms2string(Set<PosixFilePermission> pfp) {
		StringBuilder perms = new StringBuilder("---------");
		Iterator<PosixFilePermission> p = pfp.iterator();
		while (p.hasNext()) {
			switch (p.next()) {
			case OWNER_READ:
				perms.setCharAt(0, 'r');
				break;
			case OWNER_WRITE:
				perms.setCharAt(1, 'w');
				break;
			case OWNER_EXECUTE:
				perms.setCharAt(2, 'x');
				break;

			case GROUP_READ:
				perms.setCharAt(3, 'r');
				break;
			case GROUP_WRITE:
				perms.setCharAt(4, 'w');
				break;
			case GROUP_EXECUTE:
				perms.setCharAt(5, 'x');
				break;

			case OTHERS_READ:
				perms.setCharAt(6, 'r');
				break;
			case OTHERS_WRITE:
				perms.setCharAt(7, 'w');
				break;
			case OTHERS_EXECUTE:
				perms.setCharAt(8, 'x');
				break;
			}
		}

		return perms.toString();
	}

    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
    public String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
}
