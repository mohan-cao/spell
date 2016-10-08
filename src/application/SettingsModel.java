package application;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.scene.control.Alert;
import resources.ApplicationStorage;
import resources.UserStats;
/**
 * Statistics model class
 * Acts as the settings data model. Stores serializable StoredStats objects
 * 
 * @author Mohan Cao
 *
 */
public class SettingsModel {
	final Logger logger = LoggerFactory.getLogger(SettingsModel.class);
	public static final String DEFAULT_WORDLIST;
	//public static final String STATS_PATH = System.getProperty("user.dir")+"/.user/stats.ser";
	public static final String SETTINGS_PATH = System.getProperty("user.dir")+"/.user/settings.bin";
	private UserStats sessionStats;
	private UserStats globalStats;
	private ApplicationStorage appStorage;
	private MainInterface application;
	private boolean _isFirstTime;
	
	static{
		String temp = null;
		try {
			File file = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI());
			temp = file.getParent()+"/NZCER-spelling-lists.txt";
		} catch (URISyntaxException e) {
			LoggerFactory.getLogger(Game.class).error("could not find default word list");
		}
		DEFAULT_WORDLIST = temp;
	}
	/**
	 * Create new Statistics model that is not linked to a main interface.
	 * You will have to manually save the session data later, and you cannot use global stats.
	 */
	public SettingsModel() {
		this(null);
	}
	/**
	 * Create new Statistics model linked to a main interface.
	 * Stats are automatically saved upon exit.
	 * If stats file is corrupted, throws RuntimeException
	 * @param main Main interface
	 */
	public SettingsModel(MainInterface main) {
		//initiate session stats and main interface
		sessionStats = new UserStats();
		application = main;
		_isFirstTime = false;
		//initiate application storage. create one if not present.
		File file = new File(SETTINGS_PATH);
		if(!file.exists()){
			file.getParentFile().mkdirs();
			application.writeObjectToFile(file.getAbsolutePath(), new ApplicationStorage());
		}
		if(main!=null){
			Object temp = application.loadObjectFromFile(SETTINGS_PATH);
			if(temp instanceof ApplicationStorage){
				appStorage = (ApplicationStorage)temp;
			}else{
				logger.debug("application settings class: "+temp.getClass());
				Alert alert = new Alert(Alert.AlertType.INFORMATION, "Your settings file was corrupted or outdated.\nIt is now updated to a newer version");
				alert.showAndWait();
				appStorage = new ApplicationStorage();
				_isFirstTime = true;
			}
		}
		//create new stats file if it does not exist
		File file1 = new File(getAbsoluteStatsPath(appStorage.getCurrentList()));
		if(!file1.exists()){
			file1.getParentFile().mkdirs();
			application.writeObjectToFile(file1.getAbsolutePath(), new UserStats());
			_isFirstTime =true;
		}
		//load global stats using the application model if possible
		if(main!=null){
			Object temp = application.loadObjectFromFile(getAbsoluteStatsPath(appStorage.getCurrentList()));
			if(temp instanceof UserStats){
				globalStats = (UserStats)temp;
			}else{
				logger.debug("stats list class: "+temp.getClass());
				Alert alert = new Alert(Alert.AlertType.INFORMATION, "Your stats file was corrupted or outdated.\nIt is now updated to a newer version");
				alert.showAndWait();
				globalStats = new UserStats();
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
		if(globalStats!=null){
			logger.debug("session stats levels unlocked:"+sessionStats.getUnlockedLevelSet());
			globalStats.addStats(sessionStats);
			logger.debug("global stats levels unlocked:"+globalStats.getUnlockedLevelSet());
			sessionStats.clearStats();
			application.writeObjectToFile(getAbsoluteStatsPath(appStorage.getCurrentList()), globalStats);
		}
		if(appStorage!=null){
			application.writeObjectToFile(SETTINGS_PATH, appStorage);
		}
	}
	/**
	 * Gets session stats.
	 * @return Session StoredStats
	 */
	public UserStats getSessionStats(){
		return sessionStats;
	}
	/**
	 * Store stats for session. The session data will be saved upon exit.
	 * @param stats Stats to store.
	 * @throws Exception when trying to store null session
	 */
	public void storeSessionStats(UserStats stats) throws Exception {
		if(stats==null){throw new Exception("Trying to reset session stats, not allowed. Use resetSessionStats().");}
		sessionStats = stats;
	}
	/**
	 * Resets session stats.
	 */
	public void resetSessionStats(){
		sessionStats = new UserStats();
	}
	/**
	 * Gets global stats
	 * @return Global StoredStats
	 */
	public UserStats getGlobalStats(){
		return (globalStats!=null)?globalStats:null;
	}
	/**
	 * Set main application for interaction
	 * @param main Main class
	 */
	public void setMain(MainInterface main){
		application = main;
	}
	private String getAbsoluteStatsPath(File spellinglist){
		return System.getProperty("user.dir")+"/.user/"+spellinglist.getName()+".ser";
	}
	/**
	 * Gets word list paths from stored stats
	 * @return
	 */
	public Set<String> wordListsPath() {
		return appStorage.getWordListsName();
	}
	public File getCurrentList(){
		logger.debug("got word list"+appStorage.getCurrentList());
		return appStorage.getCurrentList();
	}
	/**
	 * Adds word to word list path for global stats
	 * @param path
	 */
	public void addWordListPath(String path){
		if(new File(path).exists()){
			appStorage.addWordListPath(path);
		}
	}
	/**
	 * Sets the word list.
	 * This method ENDS the current session. Do not use it during a game or an inconsistent state may occur.
	 * @param list
	 */
	public void setWordList(String list){
		//Set word list in application storage. First ends session with current list.
		logger.debug("set word list" + list);
		sessionEnd();
		appStorage.setCurrentList(list);
		//then load list stats
		Object temp = application.loadObjectFromFile(getAbsoluteStatsPath(appStorage.getCurrentList()));
		if(temp instanceof UserStats){
			globalStats = (UserStats)temp;
		}else{
			Alert alert = new Alert(Alert.AlertType.INFORMATION, "Your stats file was corrupted or outdated.\nIt is now updated to a newer version");
			alert.showAndWait();
			globalStats = new UserStats();
			_isFirstTime = true;
		}
	}
	/**
	 * Gets preferred voice
	 * @return
	 */
	public String preferredVoice(){
		return appStorage.getPreferredVoice();
	}
	/**
	 * Sets preferred voice
	 * @param newValue
	 */
	public void setPreferredVoice(String newValue) {
		appStorage.setPreferredVoice(newValue);
	}
	
}