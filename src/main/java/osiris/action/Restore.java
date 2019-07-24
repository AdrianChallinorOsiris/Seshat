package osiris.action;

import java.io.File;
import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import lombok.Data;
import lombok.extern.log4j.Log4j2;
import osiris.Config;
import osiris.S3;
import osiris.Util;
import osiris.database.Container;
import osiris.database.ContainerFile;
import osiris.database.Database;
import osiris.database.FileVersion;
import osiris.database.Folder;
import osiris.database.VersionQuery;
import org.apache.commons.lang3.time.StopWatch;

@Data
@Log4j2
public class Restore {

	Config config;

	private String restorePrefix;
	private ArrayList<VersionQuery> restoreList;
	private Folder restoreFolder;
	private Database db;
	
	
	public Restore(Config config) {
		this.config = config;
	}

	public void restoreToDirectory(String sval) {
		/*
		 * Check that the directory exists and is actually a directory. If it
		 * does not exist, attempt to create it
		 */

		if (!Util.createDir(sval)) return;
		
		File prefixDir = new File(sval);

		/*
		 * If we get to here we have a valid directory tree to restore in to.
		 */
		restorePrefix = prefixDir.getAbsolutePath() + "/";

		doRestore();
	}

	public void restoreToOriginal() {
		restorePrefix = null;
		doRestore();
	}

	public void doRestore() {
		if (restoreList == null) {
			log.warn("Nothing selected to restore");
			return;
		}
		
		// Get a unique list of containers
		Set<Container> containers = restoreList.stream().map(VersionQuery::getContainer).collect(Collectors.toCollection(TreeSet::new));
		
		// restore container files from S3

		log.info("Restoring {} container files from S3", containers.size());
		StopWatch st = new StopWatch();
		st.start();
		S3 s3 = config.getS3();
		for (Container c : containers) {
			String containerName = c.getContainerName();
			String containerLocation = config.getContainerLocation() + "/" +  containerName;
			log.debug("Restoring container {} to {}", containerName, containerLocation);
			if (config.isDryRun()) {
				log.info("Dry Run: Would restore: {} ", containerLocation);
			}
			else {
				boolean ok = s3.download(containerName, containerLocation);
				if (!ok) 
					log.error("Unable to download required container file: {} - Aborting retore", containerName);
			}
		}
		st.stop();
		log.info("S3 Restore complete. Elapsed time: {}", st);
			
		
		
		log.debug("Restoring {} files", restoreList.size());
		st.reset();
		st.start();
		for (VersionQuery v : restoreList)
			restoreVersion(v);
		
		st.stop();
		log.info("File Restore complete. Elapsed time: {}", st);
		
		// Remove used version files 
		
		log.info("Removing used version files");
		st.start();
		for (Container c : containers) {
			String containerLocation = config.getContainerLocation() + "/" + c.getContainerName();
			File f = new File(containerLocation);
			org.apache.commons.io.FileUtils.deleteQuietly(f);
		}
		log.info("Container file removal complete. Elapsed time: {}", st);

	}
		
	
	private void restoreVersion(VersionQuery v) {
		File f = new File(v.getFilePath());
		String suffix = "";
		if (restorePrefix == null)
			suffix = v.getFilePath();
		else
			suffix = f.getName();

		String pathname = (restorePrefix == null ? "" : restorePrefix) + suffix;
		log.debug("Restoring {} to {} ", v.getFilePath(), pathname);

		FileVersion ver = v.getVersion();
		Container c = ver.getContainer();
		ContainerFile cf = new ContainerFile();
		cf.setConfig(config);
		String containerName = c.getContainerName();
		if (config.isDryRun())
			log.info("Dry Run: would restore file: {} to: {} ", suffix, pathname);
		else {
			try {
				cf.restoreFile(containerName, ver, pathname);
			} catch (Exception e) {
				log.error("Error restoring file from container {}", containerName, e);
			}
			ver.setDetails();
		}
	}

	public void restoreBareMetal() {
		if (restoreList != null) {
			log.warn("Restoring to a bare metal machine. Any selected files will be ignored");
			log.error("Sorry - can't restore bare metal yet");
		}
		restoreList = new ArrayList<VersionQuery>();
		// TODO populate restore list with everything to restore on this system, as at the restore date. 
		
		doRestore();
	}
	
}
