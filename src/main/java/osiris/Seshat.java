/**
 * 
 */
package osiris;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;
import java.util.Scanner;

import org.apache.commons.lang3.StringUtils;

import lombok.extern.log4j.Log4j2;
import osiris.stp.Parser;
import osiris.stp.Result;

/**
 * @author adrian
 *
 */
@Log4j2
public class Seshat {
	private static Properties properties = new Properties();
	private static Config configuration ;
	
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		Seshat seshat = new Seshat(); 
		seshat.run(args);
	}
	
	private void run(String[] args) throws IOException {
		
		Result rc = Result.SUCCESS;
		properties.load(this.getClass().getClassLoader().getResourceAsStream("application.properties"));
		
		log.info("SESHAT started. Version {} ", properties.getProperty("version"));
		
		// Set up the config holder
		configuration = new Config();

		// Create the callback and link to the configuration
		CB cb = new CB(configuration);

		// Set up the parser
		Parser p = new Parser(SeshatGrammar.generate(), cb);
		configuration.setP(p);
		log.debug("Grammar successfully compiled");

		// Init the config
		// configuration.init();
		
		/*
		 * Process init file
		 */

		StringBuilder sb = new StringBuilder();
		String rcfile = System.getProperty("user.home") + "/.seshatrc";
		try {
			Scanner sc = new Scanner(new FileInputStream(rcfile));
			log.debug("Reading rc file: {} ", rcfile);
			p.parse(sc);
		} catch (FileNotFoundException e) {
			// Its ok for the file not to exist
		}

		/*
		 * Process command line args, if any.
		 * Ignore any flags prefixed with "-". We don't use them at all.
		 */
		
		for (String s : args) {
			if (!StringUtils.startsWith(s, "-")) {
				sb.append(s);
				sb.append(" ");
			}
		}

		if (sb.length() > 0) {
			String cmdline = sb.toString();
			log.debug("Command Line: {}", cmdline);
			Scanner sc =new Scanner(cmdline);
			rc = p.parse(sc);
		}

		/* 
		 * Pass control to the console 
		 */
		
		if (rc == Result.SUCCESS) {
			rc = Result.SUCCESS;
			Scanner sc = new Scanner(System.in);
			rc = p.parse(sc);
		}
		
		log.info("SESHAT terminated");
		System.exit(1);
	}
}
