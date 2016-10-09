package controller;

import java.net.URISyntaxException;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import application.ModelUpdateEvent;
import application.SettingsModel;
import javafx.animation.FadeTransition;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.StackPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaPlayer.Status;
import javafx.scene.paint.Color;
import javafx.util.Duration;
/**
 * A view-controller that is bound to the levels_layout fxml
 * @author Mohan Cao
 * @author Ryan MacMillan (previously, code is now gone)
 */
public class MainMenuController extends SceneController{
	final Logger logger = LoggerFactory.getLogger(this.getClass());
	@FXML private Button nQuizBtn;
	@FXML private Button vStatsBtn;
	@FXML private Button cStatsBtn;
	@FXML private Button rMistakesBtn;
	@FXML private StackPane back;
	@FXML private Label title;
	private boolean isMuted;
	private MediaPlayer media;
	@Override
	@FXML public void runOnce(){
		isMuted=false;
		try {
			media = new MediaPlayer(new Media(getClass().getClassLoader().getResource("resources/Home.mp3").toURI().toString()));
			media.setAutoPlay(false);
			media.setCycleCount(Integer.MAX_VALUE);
		} catch (URISyntaxException e) {
			logger.error("media isnt work sorry");
		}
		DropShadow ds = new DropShadow(BlurType.GAUSSIAN, Color.BLACK, 15,0.3, 0, 10);
		title.setEffect(ds);
		Task<BackgroundImage> task = new Task<BackgroundImage>(){
			@Override
			protected BackgroundImage call() throws Exception {
				Image img = new Image("https://source.unsplash.com/category/nature/1920x1080");
				BackgroundImage bgimg = new BackgroundImage(img, BackgroundRepeat.NO_REPEAT,BackgroundRepeat.NO_REPEAT, BackgroundPosition.CENTER, new BackgroundSize(BackgroundSize.AUTO, BackgroundSize.AUTO, false, false, false, true));
				return bgimg;
			}
			public void succeeded(){
				try {
					BackgroundImage bg = get();
					if(!bg.getImage().isError()){
						FadeTransition fadeOut = new FadeTransition(Duration.millis(2000),back);
						fadeOut.setFromValue(1);
						fadeOut.setToValue(0);
						fadeOut.setOnFinished(e -> {
							FadeTransition fadeIn = new FadeTransition(Duration.millis(2000),back);
							fadeIn.setFromValue(0);
							fadeIn.setToValue(1);
							back.setBackground(new Background(bg));
							fadeIn.playFromStart();
						});
						fadeOut.playFromStart();
					}
				} catch (InterruptedException | ExecutionException e) {
					logger.error(e.getMessage());
				}
			}
			
		};
		new Thread(task).start();
	}
	/**
	 * Listener for new quiz navigation button
	 * @param e MouseEvent
	 */
	@FXML public void newQuiz(MouseEvent e){
		media.pause();
		application.requestSceneChange("levelMenu");
	}
	/**
	 * Listener for Stats view navigation button
	 * @param e MouseEvent
	 */
	@FXML public void viewStats(MouseEvent e){
		media.pause();
		application.requestSceneChange("statsMenu");
	}
	/**
	 * Listener for review mistakes view navigation button
	 * @param e MouseEvent
	 */
	@FXML public void reviewMistakes(MouseEvent e){
		media.pause();
		application.requestSceneChange("levelMenu","failed");
	}
	@FXML public void changeSettings(MouseEvent e){
		media.pause();
		application.requestSceneChange("settingsMenu");
	}
	@Override
	public void init(String[] args) {
		application.update(new ModelUpdateEvent(this,"getMutedPreference"));
		if(!isMuted)media.play();
	}
	@Override
	public void onModelChange(String fieldName, Object...objects ) {
		switch(fieldName){
		case "settingsReady":
			SettingsModel sm = (SettingsModel)objects[0];
			isMuted = sm.isMuted();
		}
	}
	@Override
	public void cleanup() {}
}
