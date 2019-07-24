package osiris.database;

import java.io.Serializable;
import java.util.ArrayList;

import lombok.Data;
import lombok.extern.log4j.Log4j2;

@Data
@Log4j2
public class Database implements Serializable {
	private static final long serialVersionUID = 1L;
	ArrayList<Host> hosts = null;
	
	public Host findHost(String name) { 
		Host h = findExistingHost(name);
		
		if (h == null ) { 
			log.info("Adding new host: {}", name);
			h = new Host(name);
			hosts.add(h);
		}
		
		return h;
	}
	
	public Host findExistingHost(String name) { 
		Host h = null; 
		
		if (hosts == null) {
			log.warn("No database loaded - creating new DB");
			hosts = new ArrayList<Host>();
		}
		else {
			h = hosts.stream().filter(o -> o.getName().equals(name)).findFirst().orElse(null);
		}
		
		
		return h;
	}

	public void reportSize() {
		if (hosts == null) { 
			log.warn("Database is empty");
		}
		else {
			log.info("DB Size: hosts = {}", hosts.size());

			String s = String.format("\t%-20s %10s %10s %10s", "Host", "folders", "files", "Backups");
			System.out.println(s);

			for (Host h : hosts) {
				Size size = new Size();
				h.reportSize(size);
				
				s = String.format("\t%-20s %,10d %,10d %,10d", h.getName(), size.getFolders(), size.getFiles(), h.getBackupTimeStamps().size());
				System.out.println(s);
			}
		}
		
		
	}
}
