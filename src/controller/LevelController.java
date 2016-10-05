package controller;

import java.util.ArrayList;

import application.ModelUpdateEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;

/**
 * A view-controller that is bound to the levels_layout fxml
 * @author Mohan Cao
 * @author Ryan MacMillan
 */
public class LevelController extends SceneController {
	@FXML private Label levelStatsLbl;
	@FXML private TilePane tileContainer;
	@FXML private boolean review;
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
	@FXML public void runOnce(){lastButtonClicked=null;currentHoveredButton=null;}
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
				newBtn.setPrefSize(50, 50);
				newBtn.setStyle("-fx-font-size: "+(int)0.4*50+"px;");
				newBtn.setText(""+i);
				tileContainer.getChildren().add(newBtn);
			}
			break;
		}
	}
	@Override
	public void cleanup() {}

}
