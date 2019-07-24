package osiris.database;

import java.util.ArrayList;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AutoFolder extends Folder {
	private static final long serialVersionUID = 1L;
	private boolean isFollowLinks = false;
	private boolean isIncludeHidden = false;
	private ArrayList<String> includes = new  ArrayList<String>() ;
	private ArrayList<String> excludes  = new  ArrayList<String>();

	public AutoFolder(String name) { 
		super(name);
	}
}
