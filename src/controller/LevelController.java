package controller;

import java.net.URISyntaxException;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import application.ModelUpdateEvent;
import application.SettingsModel;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.TilePane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.text.Text;

/**
 * A view-controller that is bound to the levels_layout fxml
 * @author Mohan Cao
 * @author Ryan MacMillan
 */
public class LevelController extends SceneController {
	final Logger logger = LoggerFactory.getLogger(this.getClass());
	@FXML private Label levelStatsLbl;
	@FXML private TilePane tileContainer;
	@FXML private boolean review;
	private MediaPlayer media;
	private LevelButton lastButtonClicked;
	private Button currentHoveredButton;
	/**
	 * A small TitledPane that remembers its level.
	 * @author Mohan Cao
	 *
	 */
	public class LevelButton extends Button {
		private int _level;
		public LevelButton(int level){
			super();
			_level = level;
		}
		public int getLevel(){
			return _level;
		}
	}
	public LevelButton getButtonClicked(){
		return lastButtonClicked;
	}
	@Override
	@FXML public void runOnce(){
		try {
			media = new MediaPlayer(new Media(getClass().getClassLoader().getResource("resources/levelannouncer.mp3").toURI().toString()));
			media.setAutoPlay(false);
		} catch (URISyntaxException e) {
			logger.error("media isnt work sorry");
		}
		lastButtonClicked=null;currentHoveredButton=null;
	}
	/**
	 * Quit to main menu button
	 * @param me MouseEvent
	 */
	@FXML
	public void quitToMainMenu(MouseEvent me){
		application.requestSceneChange("mainMenu");
	}
	@Override
	public void init(String[] args) {
		//empty for subclasses to override
		media.stop();
		media.play();
		tileContainer.getChildren().clear();
		application.update(new ModelUpdateEvent(this, "levelViewLoaded"));
		if(args!=null && args.length>0 && args[0].equals("failed")){
			review = true;
		}else{
			review = false;
		}
		application.update(new ModelUpdateEvent(this, "requestLevels"));
	}
	@Override
	public void onModelChange(String fieldName, Object...objects) {
		switch(fieldName){
		case "levelsLoaded":
			ArrayList<?> stats = (ArrayList<?>)(objects[0]);
			for(int i=1;i<stats.size();i++){
				LevelButton newBtn = new LevelButton(i);
				newBtn.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
					lastButtonClicked=newBtn;
					if(review){
						application.update(new ModelUpdateEvent(this,"startReviewGame"));
					}else{
						application.update(new ModelUpdateEvent(this,"startNewGame"));
					}
				});
				final long masteredpercentage=Math.round((Double)stats.get(i)*100);
				newBtn.addEventHandler(MouseEvent.MOUSE_ENTERED, event -> {
					if(currentHoveredButton!=null&&currentHoveredButton.equals(newBtn)) return;
					levelStatsLbl.setText("Mastery (words mastered/total): "+masteredpercentage+"%");
					currentHoveredButton = newBtn;
				});
				newBtn.setMinSize(50, 50);
				newBtn.setPrefHeight(Button.USE_COMPUTED_SIZE);
				Text text = new Text(""+i);
				text.setStyle("-fx-font-size: "+(16+Math.min((i/10)*2,20))+"px;");
				newBtn.setGraphic(text);
				tileContainer.getChildren().add(newBtn);
			}
			break;
		}
	}
	@Override
	public void cleanup() {}

}
