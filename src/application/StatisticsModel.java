package application;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.scene.control.Alert;
import resources.StoredStats;
/**
 * Statistics model class
 * Acts as the statistics data model. Stores serializable StoredStats objects
 * 
 * @author Mohan Cao
 *
 */
public class StatisticsModel {
	final Logger logger = LoggerFactory.getLogger(StatisticsModel.class);
	public static final String STATS_PATH = System.getProperty("user.dir")+"/.user/stats.ser";
	private StoredStats sessionStats;
	private StoredStats globalStats;
	private MainInterface application;
	private boolean _isFirstTime;
	/**
	 * Create new Statistics model that is not linked to a main interface.
	 * You will have to manually save the session data later, and you cannot use global stats.
	 */
	public StatisticsModel() {
		this(null);
	}
	/**
	 * Create new Statistics model linked to a main interface.
	 * Stats are automatically saved upon exit.
	 * If stats file is corrupted, throws RuntimeException
	 * @param main Main interface
	 */
	public StatisticsModel(MainInterface main) {
		//initiate session stats and main interface
		sessionStats = new StoredStats();
		application = main;
		_isFirstTime = false;
		//create new stats file if it does not exist
		File file = new File(STATS_PATH);
		if(!file.exists()){
			file.getParentFile().mkdirs();
			try {
				FileOutputStream fo = new FileOutputStream(file);
				ObjectOutputStream oos = new ObjectOutputStream(fo);
				oos.writeObject(new StoredStats());
				oos.close();
				fo.close();
				_isFirstTime = true;
			} catch (IOException e) {
				logger.error(e.getMessage());
			}
		}
		//load global stats using the application model if possible
		if(main!=null){
			Object temp = application.loadObjectFromFile(STATS_PATH);
			if(temp instanceof StoredStats){
				globalStats = (StoredStats)temp;
			}else{
				Alert alert = new Alert(Alert.AlertType.INFORMATION, "Your stats file was corrupted or outdated.\nIt is now updated to a newer version");
				alert.showAndWait();
				globalStats = new StoredStats();
				_isFirstTime = true;
			}
		}
	}
	public boolean isFirstTime(){
		return _isFirstTime;
	}
	/**
	 * Ends session.
	 * The current session data is stored to the global data and serialized.
	 */
	public void sessionEnd(){
		logger.debug("session ended, storing stats.");
		if(globalStats==null){return;}
		logger.debug("session stats levels unlocked:"+sessionStats.getUnlockedLevelSet());
		globalStats.addStats(sessionStats);
		logger.debug("global stats levels unlocked:"+globalStats.getUnlockedLevelSet());
		sessionStats.clearStats();
		application.writeObjectToFile(STATS_PATH, globalStats);
	}
	/**
	 * Gets session stats.
	 * @return Session StoredStats
	 */
	public StoredStats getSessionStats(){
		return sessionStats;
	}
	/**
	 * Store stats for session. The session data will be saved upon exit.
	 * @param stats Stats to store.
	 * @throws Exception when trying to store null session
	 */
	public void storeSessionStats(StoredStats stats) throws Exception {
		if(stats==null){throw new Exception("Trying to reset session stats, not allowed. Use resetSessionStats().");}
		sessionStats = stats;
	}
	/**
	 * Resets session stats.
	 */
	public void resetSessionStats(){
		sessionStats = new StoredStats();
	}
	/**
	 * Gets global stats
	 * @return Global StoredStats
	 */
	public StoredStats getGlobalStats(){
		return (globalStats!=null)?globalStats:null;
	}
	/**
	 * Set main application for interaction
	 * @param main Main class
	 */
	public void setMain(MainInterface main){
		application = main;
	}
	
}