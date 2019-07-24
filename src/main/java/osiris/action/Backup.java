package osiris.action;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.time.StopWatch;

import lombok.Data;
import lombok.extern.log4j.Log4j2;
import osiris.Config;
import osiris.S3;
import osiris.database.TimeStamp;
import osiris.database.BackupFile;
import osiris.database.ContainerFile;
import osiris.database.Host;

@Data
@Log4j2

public class Backup {
	private Config config;

	private TimeStamp bts;
	private int fileCount;
	private int failCount;
	private int containerFiles;

	public Backup(Config config) {
		this.config = config;
	}

	public void backup(ArrayList<BackupFile> backuplist) {
		Host host = config.getCurrentHost();
		String hostname = host.getName();

		if (!backuplist.isEmpty()) {
			
			StopWatch st = new StopWatch();
			st.start();
			bts = new TimeStamp();
			host.addTimeStamp(bts);

			log.info("Creating backup timestamp: {}", bts.getBackupDate());

			fileCount = 0;
			failCount = 0;
			backupFileList(backuplist, hostname);

			st.stop();
			log.info("Completed backup of {} files, failed {},  elapsed: {}", fileCount, failCount, st);
		}
	}

	private void backupFileList(List<BackupFile> backuplist, String hostname) {
		S3 s3 = new S3(config);
		ContainerFile cf = new ContainerFile();
		cf.setS3(s3);

		cf.newContainer(hostname, "MAIN", config, bts);

		int todo = backuplist.size();
		int done = 0;
		int report = todo / 10;
		for (BackupFile c : backuplist) {
			try {
				cf.addFiletoContainer(c);
				fileCount += done;
			} 
			catch (FileNotFoundException e) {
				failCount++;
				log.warn("File not backed up: {}",  e.getMessage());
			}
			catch (Exception e) {
				failCount++;
				log.error("Error during file backup : {}", e.getMessage());
			}

			done++;

			if (report > 0) {
				if (done % report == 0) {
					int pct = (done / report) * 10;
					log.info("Progress  {} - {}%", done, pct);
				}
			}
		}

		try {
			cf.close();
		} catch (IOException e) {
			log.error("Error closing container file", e);
		}
	}

}