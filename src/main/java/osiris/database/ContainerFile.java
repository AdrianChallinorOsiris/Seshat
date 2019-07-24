package osiris.database;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import osiris.Config;
import osiris.S3;
import osiris.action.Encryptor;

@Getter
@Setter
@Log4j2
public class ContainerFile {
	
	private long totalSize = 0;
	private long fileCount;
	private int part = 0;

	private String containerName;
	private String containerLocation;
	private String host;
	private String threadID;

	private RandomAccessFile raf;
	private Config config; 
	private Container bc;
	private TimeStamp bts;
	private S3 s3;
	private String modifiedDate;
	
	Encryptor enc;
	
 	public void addFiletoContainer(BackupFile bf) throws IOException, IllegalBlockSizeException, BadPaddingException  { 
		long position = 0;
		
		log.debug("Adding file: {}", bf.getName());
		
		// Work out if we must switch container
		// Create a new version 
		FileVersion fv = new FileVersion(bf);
		
		long encsize = ((fv.getSize() + 16l)/16l)*16l;
		
		if (encsize > config.getMaxSize()) {
			log.error("File {}, size {} exceeds max size of container {}. Unable to backup", bf.getName(), config.getMaxSize());
			return;
		}
		
		if (totalSize + encsize >  config.getMaxSize())  
		{ 
			log.info("Container is full {} size {} files {}", containerName, totalSize, fileCount);
			try {
				close();
			} catch (IOException e) {
				log.error("Error closing container file {} ", containerLocation, e);
				System.exit(-1);
			}
			newContainer();
			totalSize = 0;
			fileCount = 0;
		}
		
		// Add the file to the container
		position = raf.getFilePointer();	
		long dataSize = enc.encryptfile(bf, raf);
		
		// Check 
		if (encsize != dataSize) 
			log.debug("Encrypted size does not match data size {} != {}", encsize, dataSize);
		
		
		// Save the file details
		fv.setSavedsize(dataSize);
		fv.setOffset(position);
		fv.setIv(enc.getIv());
		fv.setContainer(bc);
		fv.setBuf(bf);

		// Link the version 
		bf.addVersion(fv);
		bc.addVersion(fv);

		totalSize += dataSize;
		fileCount++;
		

	}
	
	public void newContainer(String host, String threadID, Config config, TimeStamp bts	) { 
		this.host = host;
		this.config = config;
		this.threadID = threadID;
		this.bts = bts;
		modifiedDate= new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());

		newContainer();
	}
	
	private void newContainer() {
		String partno = String.format("%03d", part++);
		containerName =  "SEF_" + host + "_" + config.getBackupName() + "_" + modifiedDate + "_" + partno;
		containerLocation = config.getContainerLocation() + "/" + containerName;
		try {
			log.info("Opening container file: {}", containerLocation);
			raf = new RandomAccessFile(containerLocation, "rw");
		} catch (FileNotFoundException e) {
			log.error("FNF on new container file: Impossible! {} ", containerLocation, e);
			System.exit(-1);
		}
		bc = new Container();
		bc.setContainerName(containerName);
		bts.addFileContainer(bc);
		enc = new Encryptor();
		enc.genSecretKey();
		bc.setPassword(enc.getPassword());
		bc.setSalt(enc.getSalt());
	}
	

	public void close() throws IOException {
		log.info("Closed container: {} ", containerName);
		bc.setFileCount(fileCount);
		bc.setSize(totalSize); 
		raf.close();
		
		if (config.isDelayUpload())
			config.AddDelayedUpload(bc);
		else {
			s3.upload(bc);
			File file = new File(containerLocation);
			file.delete();
		}
		log.debug("Container file deleted: {} ", containerLocation);
	}

	public void restoreFile(String containerName, FileVersion v, String pathname) throws IOException, IllegalBlockSizeException, BadPaddingException {
		log.info("Opening container for restore {}", containerName);
		String fileName = config.getContainerLocation() + "/" + containerName;

		File f = new File(fileName);
		if (!f.exists()) { 
			log.info("Restoring file from Amazon S3: {} ", containerName);
			S3 s3 = new S3(config);
			s3.download(containerName, fileName);
		}
		
		raf = new RandomAccessFile(fileName, "r");
		Encryptor enc = new Encryptor();
		enc.decryptTofile(raf, v, pathname);
	}

}
