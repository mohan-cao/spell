package application;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controller.IntroController;
import controller.LevelController;
import controller.SceneController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import resources.UserStats;
import resources.UserStats.Type;
/**
 * Model update event class
 * Created by controllers to update the model.
 * @author Mohan Cao
 *
 */
public class ModelUpdateEvent {
	final Logger logger = LoggerFactory.getLogger(this.getClass());
	private final String _message;
	private final SceneController _sc;
	private final Class<? extends SceneController> _class;
	private SettingsModel _statsModel;
	private MainInterface _main;
	private Game _game;
	public ModelUpdateEvent(SceneController sc, String message){
		_message = message;
		_sc = sc;
		_class = sc.getClass();
	}
	public Class<? extends SceneController> getControllerClass(){
		return _class;
	}
	/**
	 * Sets the main interface in which this object will interact with
	 * @param main
	 */
	public void setMain(MainInterface main){
		_main = main;
	}
	/**
	 * Sets stats model in which this object will interact with
	 * @param stats
	 */
	public void setStatsModel(SettingsModel stats){
		_statsModel = stats;
	}
	/**
	 * Should be called if class is an update from a quiz controller
	 * This must be called by the main application, and will not be called automatically
	 * @param screens
	 * @param screenFXMLs
	 */
	public void updateFromQuizController(Map<String, Parent> screens,Map<String,FXMLLoader> screenFXMLs){
		switch(_message){
		case "quitToMainMenu_onClick":
			if(_game!=null&&!_game.isGameEnded()){
				if(!_game.onExit()){return;}
				String testWord = _game.wordList().get(0);
				_statsModel.getSessionStats().addStat(Type.FAILED, testWord, 1, _game.level());
			}
			_main.requestSceneChange("mainMenu");
			_game = null;
			sendGameUpdateRequest();
			break;
		case "btnConfirm_onClick":
			if(_game!=null&&!_game.isGameEnded()){
				try {
					Method method = _class.getMethod("validateAndSubmitInput");
					method.invoke(_sc);
				} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
					logger.error(e.getMessage());
				}
			}else{
				_game.startGame();
			}
			break;
		case "nextLevel":
			boolean review = _game.isReview();
			_game = new Game(_main, _statsModel, _game.level()+1);
			sendGameUpdateRequest();
			_game.startGame(review);
			break;
		case "changeVoice_onClick":
			_game.changeVoice();
			break;
		case "repeatWord_onClick":
			_game.repeatWord();
			break;
		case "newGame":
			_game.startGame(false);
			break;
		case "reviewGame":
			_game.startGame(true);
			break;
		case "submitWord":
			try {
				Method method = _class.getMethod("getTextAreaInput");
				String word = (String) method.invoke(_sc);
				_game.submitWord(word);
			} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				logger.error(e.getMessage());
			}
			
			break;
		case "cleanup":
			//save and quit
			if(_game!=null&&!_game.isGameEnded()){
				String testWord = _game.wordList().get(0);
				_statsModel.getSessionStats().addStat(Type.FAILED, testWord, 1, _game.level());
			}
			_game = null;
			sendGameUpdateRequest();
			break;
		}
	}
	/**
	 * Should be called if class is an update from a stats controller
	 * This must be called by the main application, and will not be called automatically
	 */
	public void updateFromStatsController(){
		switch(_message){
		case "clearStats":
			_statsModel.getGlobalStats().clearStats();
			_statsModel.getSessionStats().clearStats();
			_statsModel.sessionEnd();
    		break;
		case "requestGlobalStats":
			_sc.onModelChange("globalStatsLoaded", _statsModel.getGlobalStats());
			break;
		case "requestSessionStats":
			_sc.onModelChange("sessionStatsLoaded", _statsModel.getSessionStats());
			break;
		}
	}
	/**
	 * Should be called if class is an update from a level controller
	 * This must be called by the main application, and will not be called automatically
	 */
	public void updateFromLevelController(){
		LevelController lc = (LevelController) _sc;
		switch(_message){
		case "requestLevels":
			ArrayList<Double> levelStats = new ArrayList<Double>();
			Set<Integer> unlockedLevelSet = new LinkedHashSet<Integer>();
			unlockedLevelSet.addAll(_statsModel.getGlobalStats().getUnlockedLevelSet());
			unlockedLevelSet.addAll(_statsModel.getSessionStats().getUnlockedLevelSet());
			ArrayList<Integer> unlockedLevels = new ArrayList<Integer>(unlockedLevelSet);
			Collections.sort(unlockedLevels);
			UserStats sStats = _statsModel.getGlobalStats();
			UserStats gStats = _statsModel.getSessionStats();
			int mastered = 0;
			int failed = 0;
			for(Integer i : unlockedLevels){
				mastered = gStats.getTotalStatsOfLevel(i, UserStats.Type.MASTERED)+sStats.getTotalStatsOfLevel(i, UserStats.Type.MASTERED);
				failed = gStats.getTotalStatsOfLevel(i, UserStats.Type.FAILED)+gStats.getTotalStatsOfLevel(i, UserStats.Type.FAULTED)+sStats.getTotalStatsOfLevel(i, UserStats.Type.FAILED)+sStats.getTotalStatsOfLevel(i, UserStats.Type.FAULTED);
				if((mastered+failed)!=0){
					levelStats.add(i,(mastered)/(double)(failed+mastered));
				}else{
					levelStats.add(i, 0d);
				}
			}
			_sc.onModelChange("levelsLoaded", levelStats);
			break;
		case "startNewGame":
			_game = new Game(_main, _statsModel, lc.getButtonClicked().getLevel());
			sendGameUpdateRequest();
			_main.requestSceneChange("quizMenu");
			break;
		case "startReviewGame":
			_game = new Game(_main, _statsModel, lc.getButtonClicked().getLevel());
			sendGameUpdateRequest();
			_main.requestSceneChange("quizMenu","failed");
			break;
		}
	}
	public void updateFromIntroController(){
		IntroController ic = (IntroController) _sc;
		switch(_message){
		case "requestLevels":
			int size = ApplicationUtility.evaluateMaxLevelInStats(_statsModel.getGlobalStats());
			int[] levels = new int[size];
			for(int i=0;i<size;i++){
				levels[i] = i+1;
			}
			_main.tell("levelsLoaded",levels);
			break;
		case "unlockLevels":
			logger.debug("Unlocked levels: "+ic.getLevelsToUnlock());
			for(int i=1;i<=ic.getLevelsToUnlock();i++){
			_statsModel.getSessionStats().unlockLevel(i);
			}
			_main.requestSceneChange("mainMenu");
			break;
		}
	}
	/**
	 * Should be called if class is an update from a level controller
	 * This must be called by the main application, and will not be called automatically
	 */
	public void updateFromSettingsController() {
		switch(_message){
		case "fetchSettings":
			_sc.onModelChange("settingsReady", _statsModel);
			break;
		default:
		}
	}
	
	/**
	 * Sets the game to the MainController's game
	 * @param game Game
	 */
	public void setGame(Game game){
		_game = game;
	}
	/**
	 * Gets the updated game.
	 * @return Game
	 */
	public Game getUpdatedGame(){
		return _game;
	}
	/**
	 * Sends game updated request back to main interface
	 */
	public void sendGameUpdateRequest(){
		ModelUpdateEvent mue = new ModelUpdateEvent(new GameUpdater(), "updateGame");
		mue.setGame(_game);
		_main.update(mue);
	}
	
}
/**
 * Empty class representing a game update event.
 * @author Mohan Cao
 *
 */
class GameUpdater extends SceneController {
	@Override
	public void init(String[] args) {}
	@Override
	public void cleanup() {}
	@Override
	public void onModelChange(String notificationString, Object... objectsParameters) {}
	@Override
	public void runOnce() {}
}