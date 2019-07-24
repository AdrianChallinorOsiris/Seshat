package osiris;

import java.io.File;

import lombok.extern.log4j.Log4j2;

/**
 * Just a bunch of static utility functions. 
 * @author adrianchallinor
 *
 */
@Log4j2

/**
 * Output memory statistics 
 * @author adrianchallinor
 *
 */
public class Util {
	public static void memstats() {
		long GB = 1024l * 1024l * 1024l;
		int cores = Runtime.getRuntime().availableProcessors();
		int freemem = (int) (Runtime.getRuntime().freeMemory() / GB);
		int maxmem = (int) (Runtime.getRuntime().maxMemory() / GB);
		int totmem = (int) (Runtime.getRuntime().totalMemory() / GB);

		log.info("Cores: {}; Free Mem: {}GB; Max Mem: {}GB; Tot Mem: {}GB", cores, freemem, maxmem, totmem);
	}

	public static boolean createDir(String dirname) { 
		boolean ok = true;
		File prefixDir = new File(dirname);

		if (prefixDir.exists()) {
			if (prefixDir.isDirectory()) {
				log.info("Restoring to: {}", prefixDir.getAbsolutePath());
			} else {
				log.error("The location {} is not a directory", prefixDir.getAbsolutePath());
				ok = false;;
			}
		} else {
			log.debug("Creating directory tree for: {}", prefixDir.getAbsolutePath());
			if (prefixDir.mkdirs())
				log.info("Created directory tree: {} ", prefixDir.getAbsolutePath());
			else {
				log.error("Failed to created directory tree {}", prefixDir.getAbsolutePath());
				ok = false;
			}
		}
		
		return ok;
	}
	
}
