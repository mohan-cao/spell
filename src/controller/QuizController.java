package controller;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.concurrent.ExecutionException;

import javax.net.ssl.HttpsURLConnection;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import application.ModelUpdateEvent;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.paint.Paint;

/**
 * A view-controller that is bound to the quiz_layout fxml
 * @author Mohan Cao
 *
 */
public class QuizController extends SceneController{
	@FXML private Label outputLabel;
	@FXML private Label correctWordLabel;
	@FXML private Label definition;
	@FXML private TextArea wordTextArea;
	@FXML private Button confirm;
	@FXML private Button voiceBtn;
	@FXML private Button repeatBtn;
	@FXML private ProgressBar progress;
	@FXML private FlowPane buttonPanel;
	
	@Override
	@FXML public void runOnce(){
		Tooltip tts = new Tooltip("Change TTS voice");
		Tooltip repeat = new Tooltip("Say the word again");
		Tooltip.install(voiceBtn,tts);
		Tooltip.install(repeatBtn, repeat);
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
		System.out.println("mouse event");
		final String def = wordTextArea.getText();
		Task<String> getreq = new Task<String>(){
			private static final String app_id = "25890eb1";
            private static final String app_key = "9f5c79bde4f7961c3e38d8f1c31e0a79";
			@Override
			protected String call() throws Exception {
				URL url = new URL("https://od-api.oxforddictionaries.com:443/api/v1/entries/en/"+def+"/examples");
                HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
                urlConnection.setRequestProperty("Accept","application/json");
                urlConnection.setRequestProperty("app_id",app_id);
                urlConnection.setRequestProperty("app_key",app_key);
                // read the output from the server
                BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                StringBuffer stringBuilder = new StringBuffer();
                String line = null;
                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line + "\n");
                }
                return stringBuilder.toString();
			}
			public void done(){
				try {
					JsonObject json = Json.parse(get()).asObject();
					JsonObject result1 = json.get("results").asArray().get(0).asObject();
					JsonObject entry1 = result1.get("lexicalEntries").asArray().get(0).asObject().get("entries").asArray().get(0).asObject();
					JsonArray examples = entry1.get("senses").asArray().get(0).asObject().get("examples").asArray();
					System.out.println(examples.get(0).asObject().get("text").asString());
					System.out.println("this works: "+get());
				} catch (InterruptedException | ExecutionException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		};
		new Thread(getreq).start();
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
			break;
		case "resetGame":
			outputLabel.setText("Well done!");
			outputLabel.setTextFill(Paint.valueOf("black"));
			if(objectParameters.length==2){
				correctWordLabel.setText("You got "+objectParameters[0]+" out of "+objectParameters[1]+" words correct.");
			}else if(objectParameters.length==3){
				correctWordLabel.setText("You got "+objectParameters[0]+" out of "+objectParameters[1]+" words correct."
					+ "\nThe last word was \""+objectParameters[2]+"\"");
			}
			wordTextArea.setDisable(true);
			confirm.setText("Restart?");
			break;
		case "masteredWord":
			outputLabel.setText("Well done");
			outputLabel.setTextFill(Paint.valueOf("#44a044"));
			correctWordLabel.setText("Correct, the word is \""+objectParameters[0]+"\"");
			progress.setStyle("-fx-accent: lightgreen;");
			break;
		case "faultedWord":
			outputLabel.setText("Try again!");
			outputLabel.setTextFill(Paint.valueOf("#cf8f14"));
			correctWordLabel.setText("Sorry, that wasn't quite right");
			progress.setStyle("-fx-accent: #ffbf44;");
			break;
		case "lastChanceWord":
			outputLabel.setText("Last try!");
			outputLabel.setTextFill(Paint.valueOf("#cf8f14"));
			correctWordLabel.setText("Let's slow it down...");
			progress.setStyle("-fx-accent: #ffbf44;");
			break;
		case "failedWord":
			outputLabel.setText("Incorrect");
			outputLabel.setTextFill(Paint.valueOf("orangered"));
			correctWordLabel.setText("The word was \""+objectParameters[0]+"\"");
			progress.setStyle("-fx-accent: orangered;");
			break;
		case "setProgress":
			progress.setProgress(Double.class.cast(objectParameters[0]));
			break;
		case "showRewards":
			buttonPanel.setVisible(true);
			break;
		case "gameWin":
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
