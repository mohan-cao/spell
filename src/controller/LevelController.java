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
import javafx.scene.layout.StackPane;
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
	@FXML private StackPane back;
	private MediaPlayer media;
	private LevelButton lastButtonClicked;
	private Button currentHoveredButton;
	private boolean isMuted;
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
			media = new MediaPlayer(new Media(getClass().getClassLoader().getResource("resources/KingOfTheDesertLoop.mp3").toURI().toString()));
			media.setAutoPlay(false);
			media.setCycleCount(Integer.MAX_VALUE);
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
		media.pause();
		application.requestSceneChange("mainMenu");
	}
	@Override
	public void init(String[] args) {
		tileContainer.getChildren().clear();
		application.update(new ModelUpdateEvent(this, "levelViewLoaded"));
		if(args!=null && args.length>0 && args[0].equals("failed")){
			review = true;
		}else{
			review = false;
		}
		application.update(new ModelUpdateEvent(this, "requestLevels"));
		application.update(new ModelUpdateEvent(this,"getMutedPreference"));
		if(!isMuted)media.play();
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
						media.pause();
						application.update(new ModelUpdateEvent(this,"startReviewGame"));
					}else{
						media.pause();
						application.update(new ModelUpdateEvent(this,"startNewGame"));
					}
				});
				final long score = (long) stats.get(i);
				newBtn.addEventHandler(MouseEvent.MOUSE_ENTERED, event -> {
					if(currentHoveredButton!=null&&currentHoveredButton.equals(newBtn)) return;
					levelStatsLbl.setText("Personal best: "+score);
					currentHoveredButton = newBtn;
				});
				newBtn.setMinSize(200, 200);
				newBtn.setPrefHeight(Button.USE_COMPUTED_SIZE);
				Text text = new Text(""+i);
				text.setStyle("-fx-font-size: "+(60+Math.min((i/10)*2,20))+"px;");
				newBtn.setGraphic(text);
				newBtn.setStyle("-fx-background-radius:100px;");
				tileContainer.getChildren().add(newBtn);
			}
			break;
		case "settingsReady":
			SettingsModel sm = (SettingsModel)objects[0];
			isMuted = sm.isMuted();
		}
	}
	@Override
	public void cleanup() {}

}
