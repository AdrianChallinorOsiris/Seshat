package osiris.database;

import java.util.ArrayList;
import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.Data;


@Data
public class TimeStamp implements Serializable {
	private static final long serialVersionUID = 1L;

    private LocalDateTime backupDate;
    private ArrayList<Container> containers;

    
    public TimeStamp () { 
    	backupDate = LocalDateTime.now();
    }

 	public void addFileContainer(Container bc) {
    	if (containers == null)
    		containers = new ArrayList<Container>();
    	containers.add(bc);		
	}

	public ArrayList<VersionQuery> getVersionList() {
		ArrayList<VersionQuery> aq = new ArrayList<VersionQuery>();
		for (Container c: containers) {
			aq.addAll(c.getVersionList());
		}
		return aq;
	}

}
