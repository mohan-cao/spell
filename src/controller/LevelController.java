package controller;

import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;

public class LevelController extends SceneController {
	@FXML private Accordion levelAccordion;
	@FXML private boolean review;
	@FXML public void initialize(){
		//empty for subclasses to override
		levelAccordion.getPanes().clear();
		/*
		 * TODO: doesn't really work yet (autoresize based on content width)
		 */
		levelAccordion.getPanes().addListener(new ListChangeListener<TitledPane>(){

			public void onChanged(javafx.collections.ListChangeListener.Change<? extends TitledPane> c) {
				double prefWidth = levelAccordion.getWidth();
				for(TitledPane t : levelAccordion.getPanes()){
					if(t.getPrefWidth()>prefWidth){
						prefWidth = t.getPrefWidth();
					}
				}
				levelAccordion.setPrefWidth(prefWidth);
			}

			
		});
	}
	@FXML
	public void quitToMainMenu(MouseEvent me){
		application.requestSceneChange("mainMenu");
	}
	@Override
	public void init(String[] args) {
		// TODO Auto-generated method stub
		application.update(this, "levelViewLoaded");
		if(args!=null && args.length>0 && args[0].equals("failed")){
			review = true;
			application.update(this, "requestReviewGameLevels");
		}else{
			review = false;
			application.update(this, "requestNewGameLevels");
		}
	}
	@Override
	public void onModelChange(String fieldName, Object...objects) {
		switch(fieldName){
		case "levelsLoaded":
			int[] stats = (int[])objects[0];
			for(int i=0;i<stats.length;i++){
				TitledPane newPane = new TitledPane();
				VBox contentPane = new VBox();
				Button newGameBtn = new Button("Start Game");
				contentPane.getChildren().add(new Label("Spelling accuracy"));
				contentPane.getChildren().add(new Label("(words mastered/total): "+stats[i]));
				contentPane.getChildren().add(newGameBtn);
				newPane.setText("Level " + (i+1));
				newPane.setContent(contentPane);
				levelAccordion.getPanes().add(newPane);
			}
			break;
		}
	}
	@Override
	public void cleanup() {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void onExit() {
		// TODO Auto-generated method stub
		
	}

}
