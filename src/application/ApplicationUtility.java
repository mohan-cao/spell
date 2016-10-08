package application;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import resources.UserStats;

public final class ApplicationUtility {
	static Logger logger = LoggerFactory.getLogger(ApplicationUtility.class);
	public static int evaluateMaxLevelInStats(UserStats stats){
		try{
			File file = stats.getCurrentList();
			logger.debug("wordlist" + file);
			if(!file.exists()){
				return 0;
			}
			FileReader fi = new FileReader(file);
			BufferedReader br = new BufferedReader(fi);
			String line = null;
			int level = 0;
			while((line= br.readLine())!=null){
				if(line.contains("%Level ")){
					line = line.split("%Level ")[1];
					level = Integer.parseInt(line);
				}
			}
			br.close();
			return level;
		}catch(IOException io){
			logger.error("could not find wordlist");
		}
		return 0;
	}
}
