package osiris.database;

import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class VersionQuery implements Serializable {
	private static final long serialVersionUID = 1L;
	private BackupFile buf;
	private FileVersion version;
	
	public String getFilePath() {
		return buf.getName();
	}

	public String getContainerName() {
		return version.getContainer().getContainerName();
	}

	public long getSavedSize() { 
		return version.getSavedsize();
	}

	public LocalDateTime getBackupDate() {
		return version.getBackupDT();
	}

	public String getBackupDT() {
		return getBackupDate().toString();
	}

	public long getSize() {
		return version.getSize();
	}
	
	public Container getContainer() { 
		return version.getContainer();
	}
}
