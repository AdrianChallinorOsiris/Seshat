package osiris;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;

import org.apache.commons.lang3.time.StopWatch;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.RegionUtils;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.S3ClientOptions;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.CreateBucketRequest;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.Upload;
import com.amazonaws.util.IOUtils;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import osiris.database.Container;



@Log4j2
@Getter
@Setter
public class S3 {
	private Config config;
	private AmazonS3 s3client;

	public S3(Config config) {
		this.config = config;
		connect();
	}

	public byte[] readDB() {
		byte[] encDB = null;
		String bucket = config.getS3BucketName();
		String key = config.getS3DBKey();
		
		try {
			boolean exists = s3client.doesObjectExist(bucket, key);
			if (exists) {
				S3Object s3object = s3client.getObject(new GetObjectRequest(bucket, key));
				if (s3object != null) {
					InputStream is = s3object.getObjectContent();
					encDB = IOUtils.toByteArray(is);
				}
			} 
		} catch (IOException e) {
			log.error("Failed to load DB from S3: {} {}", bucket, key, e);
		}
		return encDB;
	}

	public void writeDB(byte[] contents){
		String bucket = config.getS3BucketName();
		String key = config.getS3DBKey();
		try {
			InputStream stream = new ByteArrayInputStream(contents);
            ObjectMetadata meta = new ObjectMetadata();
            meta.setContentLength(contents.length);
            s3client.putObject(bucket, key, stream, meta);
		} catch (Exception e) {
			log.error("Failed to save DB to S3: {} {}", bucket, key, e);
		}
	}
	
	public boolean lockDB() { 
		// FIXME Not waiting for locks on db
		
		boolean lock = false;
		
		String bucket = config.getS3BucketName();
		String key = config.getS3DBKey() + "_LOCK";
		
		boolean alreadyLocked = s3client.doesObjectExist(bucket, key);
		if (alreadyLocked) { 
			log.warn("Lock file already exists: {}", key);
			lock = false;
		}
		else {
			try {
				File fout = new File("/tmp/" + key);
				FileWriter fw = new FileWriter(fout);
				fw.write("S3 locked " + config.getCurrentHost().getName());
				fw.close();
				s3client.putObject(new PutObjectRequest(bucket, key, fout));
				lock = true;
				log.info("DB Lock acquired");
			} catch (IOException e) {
				log.error("Error trying to create lockfile {}",  key);
			}
			
		}
		
		return lock;
	}
	
	public void unlockDB() {
		String keyName = config.getS3DBKey() + "_LOCK";
		delete (keyName);
		log.debug("Lock file removed: {}", keyName);
	}
	
	public void connect() {
		log.debug("AWS S3 - connecting");
		String home = System.getProperty("user.home");
		String credentials = home + "/.aws/credentials";
		log.debug("S3: {}", credentials);
		String regionName = config.getS3Region();
		s3client = new  AmazonS3Client(new ProfileCredentialsProvider(credentials, null));
		Region region = RegionUtils.getRegion(regionName);
		s3client.setRegion(region);
		s3client.setS3ClientOptions(S3ClientOptions.builder().enableDualstack().setAccelerateModeEnabled(true).build());
		log.debug("Amazon S3 Connected - Region: {}", region.toString());
	}
	
	public boolean isValidRegion(String s) {
		Region r = RegionUtils.getRegion(s);
		return (r != null);
	}

	public boolean isBucketExists(String bucketName) {
		return s3client.doesBucketExist(bucketName);
	}

	public void createBucket(String bucketName) {
		if (!isBucketExists(bucketName)) {
			log.info("Creating S3 bucket: {}", bucketName);
			s3client.createBucket(new CreateBucketRequest(bucketName));
		}
	}

	public boolean upload(Container c) {
		boolean ok = false;

		StopWatch st = new StopWatch();
		st.start();
		String containerName = config.getContainerLocation() + "/" + c.getContainerName();
		File f = new File(containerName);
		if (!f.exists()) {
			log.error("FATAL: Container file does not exists: {}", f.getAbsolutePath());
			return false;
		}

		String keyName = f.getName();
		String bucketName = config.getS3BucketName();

		createBucket(bucketName);
		
		double size = c.getSize();
		if (size < Config.GB * 2 ) {
			log.info("Uploading single part file {}/{} size {}", bucketName, keyName, sizer(c.getSize()));
			s3client.putObject(new PutObjectRequest(bucketName, keyName, f));
			
			ok = true;
		} else {
			log.info("Uploading multipart part file {}/{} size {} ", bucketName, keyName, sizer(c.getSize()));
			TransferManager tm = new TransferManager(new ProfileCredentialsProvider());
			Upload upload = tm.upload(bucketName, keyName, f);
			log.info("Transfer started. Size {}", sizer(c.getSize()));
			
			// Code from AWS
			// https://docs.aws.amazon.com/AmazonS3/latest/dev/transfer-acceleration-examples.html#transfer-acceleration-examples-java-client-dual-stack
			
	        try {
	        	upload.waitForCompletion();
	        } catch (AmazonClientException amazonClientException) {
	        	log.error("Amazon CLient Exception", amazonClientException);
	        } catch (InterruptedException e) {
	        	log.error("Transfer was interrupted", e);
			}
			log.info("Transfer Completed. ");

			ok = true;
		}
		
		st.stop();
		double rate = (double)c.getSize() * 8.0 / ((double)st.getTime() * 1024.0 * 1024.0) ;
		DecimalFormat df = new DecimalFormat("###,###");
		log.info("File uploaded to Amazon S3: {}/{} Duration: {} Rate: {} Mbps", bucketName, keyName, st, df.format(rate));
		f.delete();
		return ok;
	}

	public void delete(Container c) {
		String containerName = config.getContainerLocation() + "/" + c.getContainerName();
		File f = new File(containerName);
		if (f.exists()) {
			log.info("Container file deleted from disk: {}", containerName);
			f.delete();
		}
		if (c.isUploaded()) {
			String keyName = f.getName();
			delete(keyName);
			log.info("Container file deleted from S3: {}", keyName);
		}
	}

	public void delete(String keyName) {
		String bucketName = config.getS3BucketName();
		s3client.deleteObject(new DeleteObjectRequest(bucketName, keyName));
	}
	
	public boolean download(String key, String fileName) {
		boolean ok = false;
		try {
			String bucketName = config.getS3BucketName();
			
			// TODO - handle glacier files
			
			// TODO - handle large files 

			AmazonS3 s3Client = new AmazonS3Client(new ProfileCredentialsProvider());
			S3Object object = s3Client.getObject(new GetObjectRequest(bucketName, key));
			InputStream objectData = object.getObjectContent();
			try {
				OutputStream output = new FileOutputStream(fileName);
				byte[] buffer = new byte[8 * 1024];
				try {
					int bytesRead;
					while ((bytesRead = objectData.read(buffer)) != -1) {
						output.write(buffer, 0, bytesRead);
					}
				} finally {
					output.close();
				}
			} finally {
				objectData.close();
			}

			log.info("Container reestored from S3: {} ", key);
		} catch (AmazonS3Exception amazonS3Exception) {
			log.error("Amazon S3 exception", amazonS3Exception);
		} catch (Exception ex) {
			log.error("Exception", ex);
		}
		return ok;
	}
	
	public String sizer(long size) {
		String s = "";
		if (size > Config.GB) 
			s = String.format("%,d GB", size / Config.GB);
		else if (size > Config.MB)
			s = String.format("%,d MB", size / Config.MB);
		else if (size > Config.KB)
			s = String.format("%,d KB", size / Config.KB);
		else s = String.format("%,d Byte", size);
		return s;
	}
}
