package controller;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.javafx.collections.ObservableListWrapper;
import com.sun.javafx.collections.ObservableSetWrapper;

import application.ModelUpdateEvent;
import application.SettingsModel;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableListBase;
import javafx.collections.ObservableSet;
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
    ObservableList<String> words;
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
			settings = (SettingsModel)objectsParameters[0];
			setSelection(false,settings.wordListsPath(),"Change to different list...");
			if(wordSelection.getSelectionModel().getSelectedItem()==null)wordSelection.getSelectionModel().select(0);
			wordSelection.setEditable(false);
			festivalSelection.getItems().addAll(application.getVoices());
			if(festivalSelection.getSelectionModel().getSelectedItem()==null)festivalSelection.getSelectionModel().select(0);
			festivalSelection.setEditable(false);
			break;
		}
	}
	private void setSelection(boolean keepPrevious, Collection<String> newCollection, String...strings){
		if(!keepPrevious){
			words.clear();
		}
		if(settings.wordListsPath()!=null)words.addAll(settings.wordListsPath());
		if(strings!=null)words.addAll(strings);
		
	}
	@Override
	public void runOnce() {
		words = FXCollections.observableArrayList();
		wordSelection.setItems(words);
		wordSelection.setOnAction(e -> {
			e.consume();
			String newValue = wordSelection.getSelectionModel().getSelectedItem();
			if(newValue!=null){
				if(newValue.equals("Change to different list...")){
					//open file chooser
					FileChooser fc = new FileChooser();
					fc.getExtensionFilters().addAll(new ExtensionFilter("Text Files", "*.txt"),new ExtensionFilter("All Files","*"));
					//TODO: close on window close;
					File f = fc.showOpenDialog(null);
					if(f!=null){
						settings.addWordListPath(f.getAbsolutePath());
						settings.setWordList(f.getName());
						if(!words.contains(f.getName()))words.add(f.getName());
						Platform.runLater(new Runnable(){
							public void run() {
								wordSelection.getSelectionModel().clearSelection();
								wordSelection.getSelectionModel().select(f.getName());
							}
						});
					}
				}else{
					settings.setWordList(newValue);
				}
			}
		});
		/*wordSelection.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>(){
				@Override
				public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
					
				}
			});*/
		festivalSelection.valueProperty().addListener(new ChangeListener<String>(){
			@Override
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
				settings.setPreferredVoice(newValue);
			}
		});
	}

}
