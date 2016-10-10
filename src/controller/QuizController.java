package controller;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.ExecutionException;

import javax.net.ssl.HttpsURLConnection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import application.ModelUpdateEvent;
import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableDoubleValue;
import javafx.beans.value.ObservableStringValue;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Paint;

/**
 * A view-controller that is bound to the quiz_layout fxml
 * @author Mohan Cao
 *
 */
public class QuizController extends SceneController{
	final Logger logger = LoggerFactory.getLogger(this.getClass());
	@FXML private Label outputLabel;
	@FXML private Label correctWordLabel;
	@FXML private Label definition;
	@FXML private TextArea wordTextArea;
	@FXML private Button confirm;
	@FXML private Button voiceBtn;
	@FXML private Button repeatBtn;
	@FXML private ProgressBar progress;
	@FXML private FlowPane buttonPanel;
	@FXML private TextField csText;
	@FXML private TextField pbText;
	@FXML private TextField msText;
	private LongProperty personalBest;
	private LongProperty maximum;
	private LongProperty currentscore;
	
	@Override
	@FXML public void runOnce(){
		personalBest = new SimpleLongProperty();
		maximum = new SimpleLongProperty();
		currentscore = new SimpleLongProperty();
		Tooltip tts = new Tooltip("Change TTS voice");
		Tooltip repeat = new Tooltip("Say the word again");
		Tooltip.install(voiceBtn,tts);
		Tooltip.install(repeatBtn, repeat);
		csText.textProperty().bind(Bindings.format("%d", currentscore));
		pbText.textProperty().bind(Bindings.format("%d", personalBest));
		msText.textProperty().bind(Bindings.format("%d", maximum));
	}
	
	public MediaPlayer getAudioFromResources(String resource){
		try {
			MediaPlayer media = new MediaPlayer(new Media(getClass().getClassLoader().getResource(resource).toURI().toString()));
			return media;
		} catch (URISyntaxException e) {
			logger.error("media isnt work sorry");
		}
		return null;
	}
	
	/**
	 * Listener for change voice button
	 * @param me MouseEvent: mouse clicked button
	 * @author Ryan Macmillan
	 */
	@FXML
	public void changeVoice(MouseEvent me){
		application.update(new ModelUpdateEvent(this, "changeVoice_onClick"));
	}
	
	/**
	 * Listener for repeat word button
	 * @param me MouseEvent: mouse clicked button
	 * @author Ryan MacMillan
	 */
	@FXML
	public void repeatWord(MouseEvent me){
		application.update(new ModelUpdateEvent(this, "repeatWord_onClick"));
	}
	
	/**
	 * Listener for quit to main menu navigation button
	 * @param me MouseEvent
	 * @author Mohan Cao
	 */
	@FXML
	public void quitToMainMenu(MouseEvent me){
		application.update(new ModelUpdateEvent(this, "quitToMainMenu_onClick"));
	}
	/**
	 * Listener for text area key entered
	 * Prevents enter from entering a newline character
	 * @param ke KeyEvent from textArea
	 * @author Mohan Cao
	 */
	@FXML
	public void textAreaEnter(KeyEvent ke){
		if(ke.getCode()==KeyCode.ENTER){
			ke.consume();
			validateAndSubmitInput();
		}
	}
	/**
	 * Listener for text area character typed (after being typed)
	 * @param ke KeyEvent from textArea
	 * @author Mohan Cao
	 */
	@FXML
	public void textAreaType(KeyEvent ke){
		if(ke.getCharacter().matches("[^A-Za-z'\\s]")){
			ke.consume();
		}
	}
	/**
	 * Listener for confirmation button (for marking of the word)
	 * @param me MouseEvent
	 * @author Mohan Cao
	 */
	@FXML
	public void btnConfirm(MouseEvent me){
		application.update(new ModelUpdateEvent(this, "btnConfirm_onClick"));
	}
	@FXML
	public void btnNextLevel(MouseEvent me){
		application.update(new ModelUpdateEvent(this, "nextLevel"));
	}
	@FXML
	public void getDefinition(MouseEvent me){
		application.update(new ModelUpdateEvent(this,"getAndSayDefinition"));
	}
	/**
	 * Validates input before sending it to the marking algorithm
	 * @author Mohan Cao
	 */
	public void validateAndSubmitInput(){
		if(wordTextArea.getText().isEmpty()){
			//prevent accidental empty string submission for user acceptance, show brief tooltip
			
			Tooltip tip = new Tooltip("Please enter a word!");
			tip.setAutoHide(true);
			tip.show(wordTextArea, wordTextArea.localToScreen(wordTextArea.getBoundsInLocal()).getMaxX(), wordTextArea.localToScreen(wordTextArea.getBoundsInLocal()).getMinY());
			new Thread(new Task<Void>(){
				@Override
				protected Void call() throws Exception {
					Thread.sleep(1000);
					return null;
				}
				public void succeeded(){
					tip.hide();
					Tooltip.uninstall(wordTextArea, tip);
				}
			}).start();
			return;
		}
		if(wordTextArea.getText().length()>50){
			//prevent overflow, show tooltip
			Tooltip tip = new Tooltip("Word is far too long!");
			tip.setAutoHide(true);
			tip.show(wordTextArea, wordTextArea.localToScreen(wordTextArea.getBoundsInLocal()).getMaxX(), wordTextArea.localToScreen(wordTextArea.getBoundsInLocal()).getMinY());
			new Thread(new Task<Void>(){
				@Override
				protected Void call() throws Exception {
					Thread.sleep(1000);
					return null;
				}
				public void succeeded(){
					tip.hide();
					Tooltip.uninstall(wordTextArea, tip);
				}
			}).start();
			return;
		}
		application.update(new ModelUpdateEvent(this, "submitWord"));
		wordTextArea.setText("");
		wordTextArea.requestFocus();
	}
	/**
	 * Called when Application model notifies controller-view of view change
	 * 
	 */
	public void init(String[] args) {
		if(args!=null && args.length>0 && args[0].equals("failed")){
			application.update(new ModelUpdateEvent(this, "reviewGame"));
		}else{
			application.update(new ModelUpdateEvent(this, "newGame"));
		}
		buttonPanel.setVisible(false);
		
	}
	/**
b	 * Gets text area input
	 * @return textarea text
	 * @author Mohan Cao
	 */
	public String getTextAreaInput(){
		return wordTextArea.getText();
	}
	
	public void cleanup() {
		application.update(new ModelUpdateEvent(this, "cleanup"));
	}
	public void onModelChange(String signal, Object... objectParameters) {
		switch(signal){
		case "gameStartConfigure":
			logger.debug("gameStart model update");
			confirm.setDisable(false);
			buttonPanel.setVisible(false);
			wordTextArea.setDisable(false);
			confirm.setText("Check");		
			wordTextArea.requestFocus();
			outputLabel.setText("Level "+(int)objectParameters[0]);
			outputLabel.setTextFill(Paint.valueOf("black"));
			correctWordLabel.setText("Please spell the spoken words.\n"
					+ "Feel free to replay the word anytime with the right side buttons.\n"
					+ "You may also change the voice if you find it necessary.");
			personalBest.set((long) objectParameters[1]);
			maximum.set((long) objectParameters[2]);
			currentscore.set(0);
			break;
		case "resetGame":
			logger.debug("resetGame model update");
			MediaPlayer media = getAudioFromResources("resources/victory announcer.mp3");
			media.play();
			outputLabel.setText("Well done!");
			outputLabel.setTextFill(Paint.valueOf("black"));
			currentscore.set((long) objectParameters[0]);
			if(objectParameters.length==1){
				correctWordLabel.setText("Points scored this round: "+objectParameters[0]);
			}else if(objectParameters.length==2){
				correctWordLabel.setText("Points scored this round: "+objectParameters[0]+"\nThe last word was \""+objectParameters[1]+"\"");
			}
			wordTextArea.setDisable(true);
			confirm.setText("Restart?");
			break;
		case "masteredWord":
			logger.debug("masteredWord model update");
			outputLabel.setText("Well done");
			outputLabel.setTextFill(Paint.valueOf("#44a044"));
			correctWordLabel.setText("Correct, the word is \""+objectParameters[0]+"\"");
			progress.setStyle("-fx-accent: lightgreen;");
			currentscore.set((long)objectParameters[1]);
			break;
		case "faultedWord":
			logger.debug("faultedWord model update");
			outputLabel.setText("Try again!");
			outputLabel.setTextFill(Paint.valueOf("#cf8f14"));
			correctWordLabel.setText("Sorry, that wasn't quite right");
			progress.setStyle("-fx-accent: #ffbf44;");
			currentscore.set((long)objectParameters[1]);
			break;
		case "lastChanceWord":
			logger.debug("lastChanceWord model update");
			outputLabel.setText("Last try!");
			outputLabel.setTextFill(Paint.valueOf("#cf8f14"));
			correctWordLabel.setText("Let's slow it down...");
			progress.setStyle("-fx-accent: #ffbf44;");
			currentscore.set((long)objectParameters[1]);
			break;
		case "failedWord":
			logger.debug("failedWord model update");
			outputLabel.setText("Incorrect");
			outputLabel.setTextFill(Paint.valueOf("orangered"));
			correctWordLabel.setText("The word was \""+objectParameters[0]+"\"");
			progress.setStyle("-fx-accent: orangered;");
			currentscore.set((long)objectParameters[1]);
			break;
		case "setProgress":
			progress.setProgress(Double.class.cast(objectParameters[0]));
			break;
		case "showRewards":
			logger.debug("showRewards model update");
			buttonPanel.setVisible(true);
			break;
		case "gameWin":
			buttonPanel.setVisible(false);
			logger.debug("winner model update");
			outputLabel.setText("You win!");
			outputLabel.setTextFill(Paint.valueOf("#44a044"));
			correctWordLabel.setText("You have achieved mastery in all levels.\nWell done.");
			progress.setStyle("-fx-accent: lightgreen;");
			progress.setProgress(1);
			wordTextArea.setDisable(true);
			confirm.setDisable(true);
			break;
		}
	}
}
