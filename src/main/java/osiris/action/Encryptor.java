package osiris.action;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Arrays;
import java.util.Random;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import lombok.Data;
import lombok.extern.log4j.Log4j2;
import osiris.database.BackupFile;
import osiris.database.Container;
import osiris.database.FileVersion;

@Log4j2
@Data
public class Encryptor {
    private static final char[] DB_KEY = "w!z%C*F-JaNcRfUjXn2r5u8x/A?D(G+KbPeSgVkYp3s6v9y$B&E)H@McQfTjWmZq".toCharArray();
    private static final byte[] DB_SALT = "fLxBFE1rv69pZ3ExGl9IWBU5nw5qiGx6m1rsci6ZbWeCOwko1hQ0WjLvufutZUEc".getBytes();
    private static final byte[] DB_IV = "y$B&E)H@McQfTjWn".getBytes(); // 16 bytes IV
    private static final String PW = "abcdefghihklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890!£$%^&*()_-+=[{]};:@#~,<.>/?|\\\"`¬";

    private static final String AES = "AES/CBC/PKCS5PADDING"; 
    private static final String HMAC = "HmacSHA256";
    private static final String FACTORY = "PBKDF2WithHmacSHA256";
    private static final Random RANDOM = new SecureRandom();
    private final int MAX_FILE_BUF = 1024;
    
    private Cipher cipher;
    private SecretKey secret;
    
    private byte[] iv;
    private byte[] salt;
    private char[] password;

    
    /*********************** FILES 
     * @throws IOException 
     * @throws BadPaddingException 
     * @throws IllegalBlockSizeException ******************************************/
      
    
    public long encryptfile(BackupFile buf, RandomAccessFile raf) throws IllegalBlockSizeException, BadPaddingException, IOException { 
    	byte[] buffer = new byte[MAX_FILE_BUF];
    	byte[] output = null;
		genCipher(Cipher.ENCRYPT_MODE);
  		FileInputStream fis = new FileInputStream(new File(buf.getName())); 	
  		log.debug("Backing up file: {} ", buf.getName());
 		long tread = 0;
 		long twrite = 0;
 		int nread = 0;
 		while ((nread = fis.read (buffer)) > 0 ) {
 			if (nread >= MAX_FILE_BUF) 
 				output = cipher.update(buffer);
 			else {
 				byte[] op = Arrays.copyOf(buffer, nread);
 				output = cipher.doFinal(op);
 			}
 			tread += nread;
 			raf.write(output);
 			twrite += output.length;
 		 }
 		fis.close();
  		log.debug("Backed up file: {} Encrypted: {}; Read: {} ",buf.getName(), twrite, tread);
		return twrite;
    }
    
    public void decryptTofile(RandomAccessFile raf, FileVersion v, String pathname) throws IOException, IllegalBlockSizeException, BadPaddingException { 
    	// Lets get some info: 
    	
    	long offset = v.getOffset();
    	long size = v.getSavedsize();   	
    	byte[] iv = v.getIv();
    	
    	Container c = v.getContainer();
    	byte[] salt = c.getSalt();
    	char[] password = c.getPassword();
    	genSecretKey(password, salt);
    	
		genCipher(Cipher.DECRYPT_MODE, iv);
		FileOutputStream fos = new FileOutputStream(new File(pathname));
		raf.seek(offset);
 		log.debug("Restoring file file: {} ", pathname);
 		log.debug("\tOffset: {} ", offset);
 		log.debug("\tLength: {} ", size);
		 		
		byte[] output;
		while (size > 0) {
			int len = (size > MAX_FILE_BUF) ? MAX_FILE_BUF : (int) size;
			byte[] input = new byte[len];
			int read = raf.read(input);
			size = size - len;
			if (read < MAX_FILE_BUF)
				output = cipher.doFinal(input);
			else
				output = cipher.update(input);
			fos.write(output);
		}
		fos.close();
    }
    
    /*********************** HMAC ******************************************/
    
    
    public static byte[] HMAC(byte[] content) {
    	byte[] hmac = null;
     	SecretKeySpec keySpec = new SecretKeySpec( DB_SALT, HMAC);
 		try {
 			Mac mac = Mac.getInstance("HmacSHA256");
		    mac.init(keySpec);
		    hmac = mac.doFinal(content);
		} catch (Exception e) {
			log.error("Unable to compute HMAC", e);
		}
   	
    	return hmac;
    }
    	 
    /*********************** KEY ******************************************/
  
    /**
     * Generate a random password. This will be between 24 and 100 chars in length. It may not be printable. 
     */
    public void genPassword() {
    	int l = 24 + RANDOM.nextInt(76);
    	int k = PW.length();
    	password = new char[l];
    	for (int i=0; i<l; i++){ 
    		int p = RANDOM.nextInt(k);
    		password[i] = PW.charAt(p);
    	}
    	log.debug("Generated Password: {} ",password);
     }
    
    public byte[] genSalt() { 
    	salt = new byte[16];
    	RANDOM.nextBytes(salt);
       	log.debug("Generated Salt: {} ",salt);
       	return salt; 
    }
    
    public void genSecretKey() {
    	genSalt();
    	genPassword();
    	genSecretKey(password, salt);
    }
 
    public void genSecretKey(char[] pw, byte[] salt) {
 		try {
			SecretKeyFactory factory = SecretKeyFactory.getInstance(FACTORY);
	    	KeySpec spec = new PBEKeySpec(pw, salt, 65536, 256);
	    	SecretKey tmp = factory.generateSecret(spec);
	    	secret = new SecretKeySpec(tmp.getEncoded(), "AES");
	    	log.debug("SK Password: {} ",pw);
	    	log.debug("SK Salt: {} ",salt);
	    	log.debug("Generated secret key: {} ",secret.hashCode());
		} catch (Exception e) {
			log.fatal("Error creating AES cipher ", e);
		} 
      }
    
    
    public  void genCipher(int mode,  byte[] iv) {
    	try {
 	    	cipher = Cipher.getInstance(AES);
	    	cipher.init(mode, secret, new IvParameterSpec(iv));
	    	log.debug("Generated cipher: {} ",mode, iv);
		} catch (Exception e) {
			log.fatal("Error creating AES cipher ", e);
		} 
    }

    public  void genCipher(int mode) {
    	try {
    		genIV();
    		genCipher(mode, iv);
		} catch (Exception e) {
			log.fatal("Error creating AES cipher ", e);
		} 
    }

    private void genIV() { 
    	iv = new byte[16];
    	for (int i=0; i<16; i++){ 
    		iv[i] = (byte)RANDOM.nextInt(255);
    	}
    	log.debug("Generated IV: {} ", iv);
   }
    
    /*********************** DB 
     * @throws BadPaddingException 
     * @throws IllegalBlockSizeException ******************************************/

	public byte[] decryptDB(byte[] crypt) throws IllegalBlockSizeException, BadPaddingException {
		return process(Cipher.DECRYPT_MODE, crypt);
	}

	public byte[] encryptDB(byte[] plain) throws IllegalBlockSizeException, BadPaddingException {
		return process(Cipher.ENCRYPT_MODE, plain);
	}
	
	private byte[] process(int mode, byte[] input) throws IllegalBlockSizeException, BadPaddingException {
		genSecretKey(DB_KEY, DB_SALT);
		genCipher(mode, DB_IV);
		return  cipher.doFinal(input);
	}

 }