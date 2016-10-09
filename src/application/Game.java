package application;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import javax.net.ssl.HttpsURLConnection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import resources.UserStats.Type;
/**
 * Game model class
 * Contains the logic for evaluating words and storing game state, and acts as the Game data model.
 * Created on demand for "review" and "new" games.
 * 
 * Requires a MainInterface and StatisticsModel to function properly.
 * 
 * @author Mohan Cao
 * @author Ryan Macmillan
 *
 */
public class Game {
	final Logger logger = LoggerFactory.getLogger(Game.class);
	public static final int WORDS_NUM = 10;
	public static final int SAY_SPEED_INTRO = 1;
	public static final int SAY_SPEED_DEFAULT = 1;
	public static final int SAY_SPEED_SLOW = 2;
	public static final int SAY_SPEED_VERYSLOW = 3;
	private SettingsModel stats;
	private MainInterface main;
	private List<String> wordList;
	
	private boolean faulted;
	private boolean prevFaulted;
	private boolean review;
	private boolean gameEnded;
	
	private int wordListSize;
	private int _level;
	private int _correct;
	private int _incorrect;
	
	
	private List<String> voices;
	private String voiceType;
	
	public Game(MainInterface app, SettingsModel statsModel){
		this(app,statsModel,1);
	}
	public Game(MainInterface app, SettingsModel statsModel, int level){
		main = app;
		stats = statsModel;
		wordList = new LinkedList<String>();
		voiceType = stats.preferredVoice();
		voices = main.getVoices();
		_level = level;
	}
	
	/**
	 * Get word list
	 * @return
	 * @author Mohan Cao
	 */
	public List<String> wordList(){
		return wordList;
	}
	/**
	 * Checks if game has ended
	 * @return true/false
	 * @author Mohan Cao
	 */
	public boolean isGameEnded(){
		return gameEnded;
	}
	public boolean isReview(){
		return review;
	}
	/**
	 * Toggles from kal_diphone voice to akl_nz_jdt_diphone or vice versa
	 * @author Mohan Cao
	 * @author Ryan MacMillan
	 */
	public void changeVoice(){
		voiceType = voices.remove(0);
		voices.add(voiceType);
		
		switch(voiceType){
		case "kal_diphone":
			main.sayWord(SAY_SPEED_DEFAULT, voiceType, "Using American English");
			break;
		case "akl_nz_jdt_diphone":
			main.sayWord(SAY_SPEED_DEFAULT, voiceType, "Using New Zealand English");
			break;
		case "rab_diphone":
			main.sayWord(SAY_SPEED_DEFAULT, voiceType, "Using British English");
		default:
			main.sayWord(SAY_SPEED_DEFAULT, voiceType, "Using "+voiceType.replace("_", " "));
		}
	}
	/**
	 * Returns voice type
	 * @return voice string
	 */
	public String getVoice(){
		final String voice = voiceType;
		return voice;
	}
	/**
	 * Get current level.
	 * @return level
	 */
	public int level() {
		return _level;
	}
	/**
	 * Gets word list from file system path
	 * @return whether the word list has been successfully fetched to the wordList variable
	 * @author Mohan Cao
	 * @author Ryan MacMillan
	 */
	private boolean getWordList(){
		try {
			File file = stats.getCurrentList();
			if(!file.exists()){
				Alert alert = new Alert(AlertType.ERROR,"You don't have a word list!\nPlease put one in "+main.getClass().getProtectionDomain().getCodeSource().getLocation().toURI().toString());
				alert.showAndWait();
				return false;
			}
			FileReader fi = new FileReader(file);
			BufferedReader br = new BufferedReader(fi);
			String line = null;
			while((line= br.readLine())!=null){
				if(line.contains("%Level "+_level)){
					line = line.split("%Level ")[1];
					_level = Integer.parseInt(line);
					line = br.readLine();
					while(line!=null&&!line.startsWith("%Level ")){
						wordList.add(line.trim());
						line = br.readLine();
					}
					break;
				}
			}
			Collections.shuffle(wordList);
			br.close();
			return true;
		} catch (IOException | URISyntaxException e) {
			logger.error("word list fetching error");
			return false;
		}
	}
	/**
	 * Starts game in previous mode
	 */
	public void startGame(){
		startGame(review);
	}
	/**
	 * Starts the game with option for practice mode (review)
	 * @param practice review -> true
	 * @author Mohan Cao
	 */
	public void startGame(boolean practice){
		_correct=0;
		_incorrect=0;
		gameEnded=false;
		main.tell("gameStartConfigure", _level);
		review=false; //assume not reviewing words
		if(practice){
			HashSet<String> set = new HashSet<String>();
			set.addAll(stats.getGlobalStats().getKeys(Type.FAILED,_level));
			set.addAll(stats.getGlobalStats().getKeys(Type.FAULTED,_level));
			set.addAll(stats.getSessionStats().getKeys(Type.FAILED,_level));
			set.addAll(stats.getSessionStats().getKeys(Type.FAULTED,_level));
			wordList.addAll(set);
			if(wordList.size()==0){
				Alert alert = new Alert(AlertType.ERROR);
				alert.setTitle("");
				alert.setHeaderText("No words to review");
				alert.setContentText("You haven't any words to review!\nDo a spelling quiz first.");
				alert.showAndWait();
				gameEnded=true;
				main.requestSceneChange("levelMenu","failed");
				return;
			}
			Collections.shuffle(wordList);
			review=true; //reviewing words
		}else{
			getWordList();
			if(wordList.size()==0){
				main.tell("gameWin");
				gameEnded=true;
			}
		}
		if(!wordList.isEmpty()){
				wordList = wordList.subList(0, (wordList.size()>=WORDS_NUM)?WORDS_NUM:wordList.size());
				main.sayWord(SAY_SPEED_INTRO,voiceType,"Please spell the spoken words.");
				main.sayWord(SAY_SPEED_DEFAULT,voiceType,wordList.get(0));
		}
		//set faulted=false for first word
		main.tell("setProgress",0d);
		wordListSize=(wordList.size()!=0)?wordList.size():1;
		faulted=false;
	}
	
	/**
	 * Repeat the word using festival
	 * @author Ryan MacMillan
	 */
	public void repeatWord(){
		if(!gameEnded){
		main.sayWord(SAY_SPEED_DEFAULT, voiceType, wordList.get(0));
		}
	}
	
	/**
	 * Called when game is going to exit.
	 * @return true (default) or false to indicate cancellation of exiting
	 * @author Mohan Cao
	 */
	public boolean onExit(){
		if(gameEnded){
			return true;
		}
		Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle("Are you sure you want to quit?");
        alert.setContentText("You will lose progress\nIf you are in the middle of a word,\nit will be incorrect");
        Optional<ButtonType> response = alert.showAndWait();
        if(response.get()==ButtonType.OK){
        	return true;
        }else{
        	return false;
        }
	}
	/**
	 * Controller interacts with this method to check and redeliver status.
	 * A means of synchronizing controller and model resources.
	 */
	public void tick(){
		
	}
	
	
	/**
	 * Check word against game logic
	 * @param word
	 * @author Mohan Cao
	 */
	public void submitWord(String word){
		if(!gameEnded){
			int speed = SAY_SPEED_DEFAULT;
			boolean prev2Faulted = prevFaulted;
			prevFaulted = faulted;
			String testWord = wordList.get(0);
			//mark as faulted if word is wrong
			faulted=!word.toLowerCase().equals(testWord.toLowerCase());
			if(!faulted&&!prevFaulted){
				//mastered
				main.tell("masteredWord",testWord);
				faulted=false;
				stats.getSessionStats().addStat(Type.MASTERED,testWord, 1, _level);
				_correct++;
				// if review, remove from failedlist and faultedlist
				if(review){
				stats.getGlobalStats().setStats(Type.FAILED, testWord, 0);
				stats.getSessionStats().setStats(Type.FAILED, testWord, 0);
				stats.getGlobalStats().setStats(Type.FAULTED, testWord, 0);
				stats.getSessionStats().setStats(Type.FAULTED, testWord, 0);
				}
				wordList.remove(0);
			}else if(faulted&&!prevFaulted){
				//faulted once => set faulted
				main.tell("faultedWord",testWord);
				speed = SAY_SPEED_SLOW;
				main.sayWord(speed,voiceType, testWord);
				main.sayWord(SAY_SPEED_DEFAULT,voiceType, "The word is");
			}else if(!faulted&&prevFaulted){
				//correct after faulted => store faulted
				main.tell("masteredWord",testWord);
				stats.getSessionStats().addStat(Type.FAULTED,testWord, 1, _level);
				_incorrect++;
				wordList.remove(0);
			}else if(review&&!prev2Faulted){
				//give one more chance in review, set speed to very slow
				main.tell("lastChanceWord",testWord);
				speed = SAY_SPEED_VERYSLOW;
				main.sayWord(speed,voiceType, testWord);
				main.sayWord(SAY_SPEED_DEFAULT,voiceType, "The word is");
			}else{
				//faulted twice => failed
				main.tell("failedWord",testWord);
				faulted=false;
				stats.getSessionStats().addStat(Type.FAILED, testWord, 1, _level);
				wordList.remove(0);
				_incorrect++;
			}
			if(wordList.size()!=0){
				main.sayWord(speed,voiceType, wordList.get(0));
			}else{
				//end game
				if(prevFaulted||faulted||prev2Faulted){
					main.tell("resetGame",_correct,(_correct+_incorrect),testWord);
				}else{
					main.tell("resetGame",_correct,(_correct+_incorrect));
				}
				
				if(!review&&(_correct/(double)(_incorrect+_correct))>=0.9){
					stats.getSessionStats().unlockLevel(_level+1);
					main.tell("showRewards");
				}
				gameEnded=true;
			}
			//set progressbars for progress through quiz and also denote additional separation for faulted words
			main.tell("setProgress",(wordListSize-wordList.size()+((faulted)?0.5:0))/(double)wordListSize);
		}
	}
	public void getAndSayExample(){
		final String def = wordList.get(0);
		Task<String> getreq = new Task<String>(){
			private static final String app_id = "25890eb1";
            private static final String app_key = "9f5c79bde4f7961c3e38d8f1c31e0a79";
            private String getFromURL(final URL url) throws Exception{
                HttpsURLConnection urlConnection2 = (HttpsURLConnection) url.openConnection();
                urlConnection2.setRequestProperty("Accept","application/json");
                urlConnection2.setRequestProperty("app_id",app_id);
                urlConnection2.setRequestProperty("app_key",app_key);
                // read the output from the server
                BufferedReader reader2 = null;
                reader2 = new BufferedReader(new InputStreamReader(urlConnection2.getInputStream()));
                StringBuffer stringBuilder2 = new StringBuffer();
                String line2 = null;
                while ((line2 = reader2.readLine()) != null) {
                    stringBuilder2.append(line2 + "\n");
                }
                return stringBuilder2.toString();
            }
			@Override
			protected String call() throws Exception {
				try{
					return getFromURL(new URL("https://od-api.oxforddictionaries.com:443/api/v1/entries/en/"+def+"/examples"));
				}catch(IOException ie){}
                JsonObject json = Json.parse(getFromURL(new URL("https://od-api.oxforddictionaries.com:443/api/v1/inflections/en/"+def))).asObject();
                JsonObject result1 = json.get("results").asArray().get(0).asObject();
                String id = result1.get("lexicalEntries").asArray().get(0).asObject().get("inflectionOf").asArray().get(0).asObject().get("id").asString();
                return getFromURL(new URL("https://od-api.oxforddictionaries.com:443/api/v1/entries/en/"+id+"/examples"));
                
			}
			public void done(){
				try {
					JsonObject json = Json.parse(get()).asObject();
					JsonObject result1 = json.get("results").asArray().get(0).asObject();
					JsonObject entry1 = null;
					for(JsonValue lexEntries : result1.get("lexicalEntries").asArray()){
						String lexCat = lexEntries.asObject().getString("lexicalCategory", null);
						if(lexCat!=null){
							lexCat = lexCat.toLowerCase();
							if(!lexCat.equals("residual")){
								entry1 = lexEntries.asObject().get("entries").asArray().get(0).asObject();
								break;
							}
						}
					}
					if(entry1==null)return;
					JsonArray examples = entry1.get("senses").asArray();
					JsonValue temp = null;
					JsonArray out = null;
					for(JsonValue jv : examples){
						temp = jv.asObject().get("examples");
						if(temp!=null){
							out = temp.asArray();
							if(out!=null){
								String str = out.get(0).asObject().get("text").asString().replaceAll("[^\\sa-zA-Z0-9']", " ");
								main.sayWord(1.3, voiceType, str);
								logger.debug("Word was: "+str);
								break;
							}
						}
					}
					
				} catch (InterruptedException ie){logger.error(ie.getMessage());}catch(ExecutionException e) {
					logger.error(e.getLocalizedMessage());
				}
			}
		};
		new Thread(getreq).start();
	}
	
}
