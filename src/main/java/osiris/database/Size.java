package osiris.database;

import lombok.Data;

@Data
public class Size {
	private int files = 0;
	private int folders = 0;
	
	public void addFiles(int i) { 
		files += i;
		folders++;
	}
	
}
