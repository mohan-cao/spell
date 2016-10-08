package controller;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import application.ModelUpdateEvent;
import application.SettingsModel;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.input.MouseEvent;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
/**
 * Settings controller
 * @author Mohan Cao
 *
 */
public class SettingsController extends SceneController {
    @FXML private ComboBox<String> festivalSelection;
    @FXML private ComboBox<String> wordSelection;
    private SettingsModel settings;
    final Logger logger = LoggerFactory.getLogger(this.getClass());
	@Override
	public void init(String[] args) {
		application.update(new ModelUpdateEvent(this,"fetchSettings"));
	}
	@FXML
	public void mainOnClick(MouseEvent me){
		application.requestSceneChange("mainMenu");
	}

	@Override
	public void cleanup() {}

	@Override
	public void onModelChange(String notificationString, Object... objectsParameters) {
		switch(notificationString){
		case "settingsReady":
			wordSelection.getItems().clear();
			festivalSelection.getItems().clear();
			settings = (SettingsModel)objectsParameters[0];
			for(File f : settings.wordListsPath()){
				wordSelection.getItems().add(f.getName());
			}
			wordSelection.getItems().add("Change to different list...");
			wordSelection.getSelectionModel().select(0);
			wordSelection.setEditable(false);
			
			festivalSelection.getItems().addAll(application.getVoices());
			festivalSelection.getSelectionModel().select(0);
			
			break;
		}
	}

	@Override
	public void runOnce() {
		wordSelection.valueProperty().addListener(new ChangeListener<String>(){
				@Override
				public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
					if(newValue!=null){
						if(newValue.equals("Change to different list...")){
							//open file chooser
							FileChooser fc = new FileChooser();
							fc.getExtensionFilters().addAll(new ExtensionFilter("Text Files", "*.txt"),new ExtensionFilter("All Files","*"));
							//TODO: close on window close;
							File f = fc.showOpenDialog(null);
							if(f!=null){
								
							}
						}
					}
				}
			});
		festivalSelection.valueProperty().addListener(new ChangeListener<String>(){
			@Override
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
				settings.setPreferredVoice(newValue);
			}
		});
	}

}
