package resources;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Set;

import application.SettingsModel;

public class ApplicationStorage implements Serializable {
	private static final long serialVersionUID = 1L;
	private HashMap<String,File> _spellingListPath;
	private String _currentListKey;
	private String _preferredVoice;
	private boolean _musicMuted;
	public ApplicationStorage(){
		_spellingListPath = new HashMap<String,File>();
		File f = new File(SettingsModel.DEFAULT_WORDLIST);
		_spellingListPath.put("Default Word-list",f);
		_currentListKey = "Default Word-list";
	}
	/**
	 * Adds word list absolute path to stored paths
	 * @param path
	 */
	public void addWordListPath(String path){
		File f = new File(path);
		_spellingListPath.put(f.getName(),f);
	}
	/**
	 * Gets a set of all stored wordlists
	 * @return
	 */
	public Set<String> getWordListsName() {
		return _spellingListPath.keySet();
	}
	/**
	 * Sets *current* word list key to the key given
	 * @param key identifier
	 * @return if key was set
	 */
	public boolean setCurrentList(String key){
		if(_spellingListPath.containsKey(key)){
			_currentListKey = key;
			return true;
		}
		return false;
	}
	/**
	 * Gets current spelling list
	 * @return
	 */
	public File getCurrentList() {
		return _spellingListPath.get(_currentListKey);
	}
	
	/**
	 * Get current list key for display
	 */
	public String getCurrentListKey(){
		return _currentListKey;
	}
	
	/**
	 * Gets preferred voice.
	 * @return
	 */
	public String getPreferredVoice() {
		return _preferredVoice;
	}
	/**
	 * Sets preferred voice.
	 * @param newValue
	 */
	public void setPreferredVoice(String newValue) {
		_preferredVoice = newValue;
	}

	/**
	 * Toggles BGM preferences
	 * @return state that it was toggled to
	 */
	public void toggle(){
		_musicMuted = !_musicMuted;
	}
	public boolean muted(){
		return _musicMuted;
	}
}
