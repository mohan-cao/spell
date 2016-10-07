package controller;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
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
		final String def = wordTextArea.getText();
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
								application.sayWord(1.3, "kal_diphone", str);
								System.out.println(str);
							}
						}
					}
					
				} catch (InterruptedException ie){logger.error(ie.getMessage());}catch(ExecutionException e) {
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
