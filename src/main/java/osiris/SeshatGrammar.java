package osiris;


import lombok.Data;
import lombok.extern.log4j.Log4j2;
import osiris.stp.*;

@Data
@Log4j2
public class SeshatGrammar {

	public static Grammar generate() {
		log.debug("Starting grammar generation");

		Grammar STP = new Grammar("SESHAT", "SESHAT");
		
		STP.state("SESHAT")
			.add(new Keyword("EXIT").Complete().callback("exit"))
			.add(new Keyword("QUIT").Complete().callback("exit"))
			.add(new Keyword("BYE").Complete().callback("exit"))
			.add(new Keyword("SET").setSuccess(STP.state("SET")))
			.add(new Keyword("BACKUP").setSuccess(STP.state("BACKUP")))
			.add(new Keyword("SHOW").setSuccess(STP.state("SHOW")))
			.add(new Keyword("INCLUDE").setSuccess(STP.state("INCLUDE")))
			.add(new Keyword("EXCLUDE").setSuccess(STP.state("EXCLUDE")))
			.add(new Keyword("S3").setSuccess(STP.state("S3")))
			.add(new Keyword("DB").setSuccess(STP.state("DB")))
			.add(new Keyword("LIST").setSuccess(STP.state("LIST")))
			.add(new Keyword("SELECT").setSuccess(STP.state("SELECT")))
			.add(new Keyword("SEARCH").setSuccess(STP.state("SEARCH")))
			.add(new Keyword("RESTORE").setSuccess(STP.state("RESTORE")))
			.add(new Keyword("AUTOMATIC").callback("automatic"))
			.add(new Keyword("CD").setSuccess(STP.state("FOLDER")))
			.add(new Keyword("RESET").setSuccess(STP.state("RESET")))
			.add(new Keyword("INITIALIZE").callback("init"))
			.add(new Keyword("MEMSTATS").callback("memstats"))
			.add(new Keyword("HELP").callback("help"))
			.add(new Keyword("GRAMMAR").setSuccess(STP.state("GRAMMAR")))
			.add(new Keyword("LOGGER").setSuccess(STP.state("LOG")))
			;
		
		STP.state("LOG")
			.add(new Keyword("LIST").callback("loggerList"))
			.add(new Keyword("SET").setSuccess(STP.state("LOGSET")))
			;
		
		STP.state("LOGSET")
			.add(new Word().callback("loggerName").setSuccess(STP.state("LOGLEVEL")));

		STP.state("LOGLEVEL")
			.add(new Keyword("DEBUG").callback("loggerLevel"))
			.add(new Keyword("TRACE").callback("loggerLevel"))
			.add(new Keyword("INFO").callback("loggerLevel"))
			.add(new Keyword("WARN").callback("loggerLevel"))
			.add(new Keyword("ERROR").callback("loggerLevel"))
			;
		
		STP.state("RESTORE")
			.add(new Keyword("ADD").setSuccess(STP.state("ADD")))
			.add(new Keyword("GO").callback("restoreGo"))
			.add(new Keyword("BAREMETAL").callback("restoreBareMetal"))
			.add(new Keyword("TIMESTAMP").setSuccess(STP.state("RESTORETIME")))
			.add(new Keyword("TARGET").setSuccess(STP.state("RESTORETARGET")))					
		;
		
		STP.state("RESTORETIME")
			.add(new DateTime().callback("restoreTimeStamp"))
		;
		
		STP.state("RESTORETARGET")
			.add(new Keyword("ORIGINAL").callback("restoreToOriginal"))
			.add(new Word().callback("restoreToDirectory"))
		;
		
		STP.state("GRAMMAR")
			.add(new Keyword("CHECK").callback("grammarCheck"))
			.add(new Keyword("PRUNE").callback("grammarPrune"))
			.add(new Keyword("PRINT").callback("grammarPrint"));
		
		STP.state("ADD")
			.add(new Keyword("FILE").setSuccess(STP.state("FILE")))
			.add(new Keyword("TREE").callback("addTree"))
			.add(new Keyword("FOLDER").callback("addFolder"))
		;		
		
		STP.state("FILE")
			.add(new Keyword("RESET").callback("fileReset"))
			.add(new Keyword("ALL").callback("fileAll"))
			.add(new IntNumber().callback("selectFileNumber"))
		;
			
		STP.state("BACKUP")
			.add(new Word().callback("backup"));
		
		STP.state("SET")
			.add(new Keyword("HIDDEN").setSuccess(STP.state("HIDDEN")))
			.add(new Keyword("SYMLINKS").setSuccess(STP.state("SYMLINKS")))
			.add(new Keyword("MAX").setSuccess(STP.state("MAX")))
			.add(new Keyword("DETAIL").setSuccess(STP.state("DETAIL")))
			.add(new Keyword("WAIT").setSuccess(STP.state("WAIT")))
			.add(new Keyword("DELAY").setSuccess(STP.state("DELAY")))
			.add(new Keyword("DRYRUN").setSuccess(STP.state("DRYRUN")))
			.add(new Keyword("AUTOBACKUP").setSuccess(STP.state("SETAUTO")))
			;
	

		STP.state("MAX") 
			.add(new Keyword("SIZE").setSuccess(STP.state("MAX1"))) 
			.add(new Lambda().setSuccess(STP.state("MAX1")))
			;

		STP.state("MAX1") 
			.add(new IntNumber().callback("setSize").setSuccess(STP.state("SIZE")))
			;

		STP.state("SETAUTO")
			.add(new Keyword("YES").callback("enable"))
			.add(new Keyword("NO").callback("disable"));

		STP.state("SIZE")
			.add(new Keyword("MB").callback("setSizeMultiplier"))
			.add(new Keyword("GB").callback("setSizeMultiplier"))
			;

		STP.state("WAIT")
			.add(new Keyword("YES").callback("setWaitYes"))
			.add(new Keyword("NO").callback("setWaitNo"));
		;

		STP.state("HIDDEN")
			.add(new Keyword("YES").callback("setHiddenYes"))
			.add(new Keyword("NO").callback("setHiddenNo"));
		
		STP.state("DRYRUN")
			.add(new Keyword("YES").callback("setDryRunYes"))
			.add(new Keyword("NO").callback("setDryRunNo"));
		
		STP.state("DELAY")
		.add(new Keyword("YES").callback("setDelayYes"))
		.add(new Keyword("NO").callback("setDelayNo"));
	
		STP.state("DETAIL")
			.add(new Keyword("YES").callback("setDetailYes"))
			.add(new Keyword("NO").callback("setDetailNo"));
	
		STP.state("SYMLINKS")
			.add(new Keyword("YES").callback("setLinksYes"))
			.add(new Keyword("NO").callback("setLinksNo"));

		STP.state("SHOW")
			.add(new Keyword("CONFIG").callback("showConfig"))
		;
		
		STP.state("INCLUDE")
			.add(new Keyword("RESET").callback("includeReset"))
			.add(new Word().callback("include"));

		STP.state("EXCLUDE")
			.add(new Keyword("RESET").callback("excludeReset"))
			.add(new Word().callback("exclude"));

		STP.state("S3")
			.add(new Keyword("BUCKET").setSuccess(STP.state("S3BUCKET")))
			.add(new Keyword("REGION").setSuccess(STP.state("S3REGION")))
			;

		STP.state("S3BUCKET")
			.add(new Word().callback("s3Bucket"));

		STP.state("S3REGION")
			.add(new Word().callback("s3Region"));

		STP.state("LIST")
			.add(new IntNumber().callback("listLimit").setSuccess(STP.state("LISTACTIONS")))
			.add(new Lambda().setSuccess(STP.state("LISTACTIONS")))
			;
		
		STP.state("LISTACTIONS")
			.add(new Keyword("HOSTS").callback("listHosts"))
			.add(new Keyword("AUTO").callback("listAuto"))
			.add(new Keyword("DIRS").callback("listFolders"))
			.add(new Keyword("DIRECTORIES").callback("listFolders"))
			.add(new Keyword("FOLDERS").callback("listFolders"))
			.add(new Keyword("BACKUPS").callback("listBackups"))
			.add(new Keyword("FILES").callback("listFiles"))
			.add(new Keyword("TREE").callback("listTree"))
			.add(new Keyword("VERSIONS").callback("listVersions"))
			.add(new Keyword("FOUND").callback("listFound"))
			.add(new Keyword("SELECTED").callback("listSelected"))
			;

		STP.state("SELECT")
			.add(new Keyword("RESET").callback("selectReset"))
			.add(new Keyword("HOST").setSuccess(STP.state("HOST")))
			.add(new Keyword("FOLDER").setSuccess(STP.state("FOLDER")))
			.add(new Keyword("BACKUP").setSuccess(STP.state("BACKUP1")))
			;
				
		STP.state("BACKUP1")
			.add(new IntNumber().callback("selectBackupNumber"))
			.add(new Date().callback("selectBackupDate"));

		STP.state("HOST")
			.add(new IntNumber().callback("selectHostNumber"))
			.add(new Word().callback("selectHostName"));

		STP.state("FOLDER")
			.add(new IntNumber().callback("selectFolderNumber"))
			.add(new Word().callback("selectFolderName"));
		

		STP.state("DB")
			.add(new Keyword("RESET").callback("dbReset"))
			.add(new Keyword("LOAD").callback("dbLoad"))
			.add(new Keyword("SAVE").callback("dbSave"))
			.add(new Keyword("SIZE").callback("dbSize"))
			.add(new Keyword("UNLOCK").callback("dbUnlock"))		
			.add(new Keyword("LOCK").callback("dbLock"))
			.add(new Keyword("STATUS").callback("dbStatus"))
			;
		
		STP.state("RESET")
			.add(new Keyword("DB").callback("dbReset"))
			.add(new Keyword("RESTORE").callback("fileReset"))
			;
		
		STP.state("SEARCH")
			.add(new Keyword("CONTAINS").setSuccess(STP.state("CONTAINS")))
			.add(new Keyword("REGEX").setSuccess(STP.state("REGEX")))
			;
		
		STP.state("CONTAINS")
			.add(new Word().callback("fileContains"));
		
		STP.state("REGEX")
			.add(new Word().callback("fileRegex"));
		
		
		STP.prune();
		STP.check();
		
		return STP;
	}
}
