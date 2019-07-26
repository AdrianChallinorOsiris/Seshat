package osiris;

import java.util.ArrayList;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.log4j.Log4j2;
import osiris.action.Finder;
import osiris.action.Lister;
import osiris.action.Backup;
import osiris.action.Restore;
import osiris.database.AutoFolder;
import osiris.database.BackupFile;
import osiris.database.Host;
import osiris.stp.Callback;
import osiris.stp.Token;

@Log4j2
@Data
@EqualsAndHashCode(callSuper=false)
public class CB extends Callback {
	private Config config;
	private Lister lister;
	private Restore restore;
	
	public CB(Config config) {
		super();
		this.config = config;
		lister = new Lister(config);
		restore = new Restore(config);
	}
	
	public void exit(Token arg1) { }
	public void listLimit(Token arg1) {	config.setListLimit(arg1.ival);	}
	
	public void dbLoad(Token arg1) { config.autoLoadDB(); }
	public void dbSave(Token arg1) { config.autoSaveDB(); }
	public void dbReset(Token arg1) { config.resetDB(); }
	public void dbSize(Token arg1) {config.sizeDB(); }
	public void dbLock(Token arg1) {config.lockDB(); }
	public void dbUnlock(Token arg1) {config.unlockDB(); }

	public void setDryRunYes(Token arg1) { config.setDryRun(true); }
	public void setDryRunNo(Token arg1) { config.setDryRun(false); }
	public void setDelayYes(Token arg1) { config.setDelayUpload(true); }
	public void setDelayNo(Token arg1) { config.setDelayUpload(false); }
	public void setWaitYes(Token arg1) { config.setWaitOnLock(true); }
	public void setWaitNo(Token arg1) {	config.setWaitOnLock(false); }
	public void setDetailYes(Token arg1) { config.setReportUnmatched(true); }
	public void setDetailNo(Token arg1) { config.setReportUnmatched(false); }
	public void setHiddenYes(Token arg1) { config.setIncludeHidden(true);	}
	public void setHiddenNo(Token arg1) { config.setIncludeHidden(false);	}
	public void setLinksYes(Token arg1) { config.setFollowLinks(true);	}
	public void setLinksNo(Token arg1) { config.setFollowLinks(false);}
	public void includeReset(Token arg1) { config.includeReset();	}
	public void excludeReset(Token arg1) { config.excludeReset();	}
	public void include(Token arg1) { config.includeAdd(arg1.sval);	}
	public void exclude(Token arg1) { config.excludeAdd(arg1.sval);	}
	public void showConfig(Token arg1) { config.show();}
	
	public void backup(Token arg) {
		if (!config.lockDB()) 
			return;
		
		Finder finder = new Finder(config);
		ArrayList<BackupFile> c = finder.scan(arg.sval);
		Backup backup = new Backup(config);
		backup.backup(c);
		config.autoSaveDB();
		config.ProcessDelayed();
	}

	public void automatic(Token arg) {
		config.setWaitOnLock(true);
		if (!config.lockDB()) 
			return;
		
		Host host = config.getCurrentHost();
		ArrayList<AutoFolder> paths = host.getFolders();
				
		Finder finder = new Finder(config);
		Backup backup = new Backup(config);
		if (paths != null) {
			for (AutoFolder bp : paths) {
				
				config.setExcludes(bp.getExcludes());
				config.setIncludes(bp.getIncludes());
				config.setFollowLinks(bp.isFollowLinks());
				config.setIncludeHidden(bp.isIncludeHidden());
				
				ArrayList<BackupFile> c = finder.scan(bp.getName());
				backup.backup(c);
			}
			config.autoSaveDB();
			config.ProcessDelayed();
		} else {
			log.warn("No backup paths found to process for host: {}", host.getName());
			config.unlockDB(); 
		}
	}
	
	public void s3Bucket(Token arg1) {	config.setS3BucketName(arg1.sval);	}

	public void s3Region(Token arg1) {
		S3 s3 = new S3(config);
		if (s3.isValidRegion(arg1.sval))
			config.setS3Region(arg1.sval);
		else 
			log.warn("Invalid Amazon S3 region: {} ", arg1.sval);
		}
	
	public void init(Token arg1) { config.init(true); }

	public void listSelected(Token arg1) { lister.listSelected(); }
	public void listAuto(Token arg1) { lister.listAuto(); }
	
	public void listHosts(Token arg1) { lister.listHosts(); }
	public void listFolders(Token arg1) { lister.listFolders(); }
	public void listBackups(Token arg1) { lister.listBackups(); }
	public void listFound(Token arg1) { lister.listFound(); }
	public void listFiles(Token arg1) { lister.listFiles(false); }
	public void listTree(Token arg1) { lister.listFiles(true); }
	public void listVersions(Token arg1) { lister.listVersions(); }
	public void selectReset(Token arg1) { lister.selectReset(); }
	public void selectHostNumber(Token arg1) { lister.selectHostNumber(arg1.ival); }
	public void selectFolderNumber(Token arg1) { lister.selectFolderNumber(arg1.ival); }
	public void selectHostName(Token arg1) { lister.selectHostName(arg1.sval); }
	public void selectFolderName(Token arg1) { lister.selectFolderName(arg1.sval); }
	public void selectBackupNumber(Token arg1) { lister.selectBackupNumber(arg1.ival); }
	public void selectBackupDate(Token arg1) { lister.selectBackupDate(arg1.sval); }
	public void fileReset(Token arg1) { lister.fileReset(); }
	public void fileAll(Token arg1) { lister.fileAll(); } 
	public void fileContains(Token arg1) { lister.fileContains(arg1.sval); }
	public void fileRegex(Token arg1) { lister.fileRegex(arg1.sval); }
	public void selectFileNumber(Token arg1) { lister.selectFileNumber(arg1.ival); }
	
	
	public void addFolder(Token arg1)		{ 
		lister.getFilesInFolder(false);
		}
	
	public void addTree(Token arg1)		{ 
		lister.getFilesInFolder(true);
		}
	
	public void restoreToDirectory(Token arg1) 	{ restore.restoreToDirectory(arg1.sval);		}
	public void restoreToOriginal(Token arg1) 	{ restore.restoreToOriginal();		}
	public void restoreGo(Token arg1) 			{ restore.doRestore();							}
	public void restoreTimeStamp(Token arg1)	{ lister.setRestorePoint(arg1.ldtval);			}
	public void restoreBareMetal(Token arg1)	{ restore.restoreBareMetal(); 		}
	
	public void setSize(Token arg1) { config.setMaxSize(arg1.ival); }
	
	public void setSizeMultiplier(Token arg1) {  config.setMaxSize(arg1.sval); }
	
	public void grammarCheck(Token arg1) { config.getP().check(); }
	public void grammarPrint(Token arg1) { config.getP().print(); }
	public void grammarPrune(Token arg1) { config.getP().prune(); }
	public void help(Token arg1) { config.getP().help(); }
	public void memstats(Token arg1) { Util.memstats(); }
//	public void password(Token arg1) { config.setPassword(arg1.sval); }


}
