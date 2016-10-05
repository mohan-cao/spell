package controller;

import java.util.concurrent.ExecutionException;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.Transition;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.util.Duration;
/**
 * A view-controller that is bound to the levels_layout fxml
 * @author Mohan Cao
 * @author Ryan MacMillan
 */
public class MainMenuController extends SceneController{
	@FXML private Button nQuizBtn;
	@FXML private Button vStatsBtn;
	@FXML private Button cStatsBtn;
	@FXML private Button rMistakesBtn;
	@FXML private StackPane back;
	@FXML private Label title;
	/**
	 * Listener for new quiz navigation button
	 * @param e MouseEvent
	 */
	@FXML public void initialize(){
		back.setBackground(new Background(new BackgroundFill(Color.BLACK, null, null)));
		title.setStyle("-fx-text-fill: #fff;");
		Task<BackgroundImage> task = new Task<BackgroundImage>(){

			@Override
			protected BackgroundImage call() throws Exception {
				Image img = new Image("https://hd.unsplash.com/24/DSC02021.JPG");
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
					title.setStyle("-fx-text-fill: #fff;");
				} catch (InterruptedException | ExecutionException e) {
					e.printStackTrace();
				}
			}
			
		};
		new Thread(task).start();
		//back.setStyle("-fx-background-image: url(\"https://unsplash.it/800/600/?random&blur\");");
	}
	@FXML public void newQuiz(MouseEvent e){
		application.requestSceneChange("levelMenu");
	}
	/**
	 * Listener for Stats view navigation button
	 * @param e MouseEvent
	 */
	@FXML public void viewStats(MouseEvent e){
		application.requestSceneChange("statsMenu");
	}
	/**
	 * Listener for review mistakes view navigation button
	 * @param e MouseEvent
	 */
	@FXML public void reviewMistakes(MouseEvent e){
		application.requestSceneChange("levelMenu","failed");
	}
	@Override
	public void init(String[] args) {}
	@Override
	public void onModelChange(String fieldName, Object...objects ) {}
	@Override
	public void cleanup() {}
}
