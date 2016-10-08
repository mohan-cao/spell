package controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;

import com.sun.javafx.collections.ObservableListWrapper;

import application.ModelUpdateEvent;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.concurrent.Task;
import javafx.event.EventType;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.SortType;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import resources.UserStats;
import resources.UserStats.Type;
/**
 *
 * A view-controller that is bound to the stats_layout fxml
 * @author Mohan Cao
 *
 */
public class StatsController extends SceneController{
    @FXML private BarChart<String, Number> barChartView;
    @FXML private Button mainMenuBtn;
    @FXML private Button clearStatsBtn;
    @FXML private TableView<Word> statsTable;
    @FXML private ComboBox<String> statsSelection;
    @FXML private TextField filterField;
    public class Word implements Comparable<Word>{
    	private StringProperty _word;
    	private IntegerProperty _level;
    	private IntegerProperty _mastered;
    	private IntegerProperty _faulted;
    	private IntegerProperty _failed;
    	private DoubleProperty _mastery;
    	public Word(String word, int level, int mastered, int faulted, int failed, double mastery){
    		setProperties(word,level,mastered,faulted,failed,mastery);
    	}
    	public void setProperties(String word, int level, int mastered, int faulted, int failed, double mastery){
    		wProperty().set(word);
    		mProperty().set(mastered);
    		lProperty().set(level);
    		faultProperty().set(faulted);
    		failProperty().set(failed);
    		masteryProperty().set(mastery);

    	}
    	public String getWProperty(){
    		return wProperty().get();
    	}
    	public int getMProperty(){
    		return mProperty().get();
    	}
    	public int getLProperty(){
    		return lProperty().get();
    	}
    	public int getFaultProperty(){
    		return faultProperty().get();
    	}
    	public int getFailProperty(){
    		return failProperty().get();
    	}
    	public double getMasteryProperty(){
    		return masteryProperty().get();
    	}
    	public StringProperty wProperty(){
    		if (_word == null) _word = new SimpleStringProperty(this, "wProperty");
            return _word; 
    	}
    	public IntegerProperty mProperty(){
    		if (_mastered == null) _mastered = new SimpleIntegerProperty(this, "mProperty");
            return _mastered; 
    	}
    	public IntegerProperty lProperty(){
    		if (_level == null) _level = new SimpleIntegerProperty(this, "lProperty");
            return _level; 
    	}
    	public DoubleProperty masteryProperty(){
    		if (_mastery == null) _mastery = new SimpleDoubleProperty(this, "masteryProperty");
            return _mastery; 
    	}
    	public IntegerProperty faultProperty(){
    		if (_faulted == null) _faulted = new SimpleIntegerProperty(this, "faultProperty");
            return _faulted; 
    	}
    	public IntegerProperty failProperty(){
    		if (_failed == null) _failed = new SimpleIntegerProperty(this, "failProperty");
            return _failed;
    	}
		@Override
		public int compareTo(Word arg0) {
			return lProperty().get()-arg0.lProperty().get();
		}
    }
	@Override
    @FXML public void runOnce(){
		barChartView.getYAxis().setLabel("Percentage");
		statsSelection.getItems().addAll("Global statistics", "Session statistics");
		statsSelection.getSelectionModel().select(1);
		statsSelection.setEditable(false);
		StatsController thisController = this;
		statsSelection.valueProperty().addListener(new ChangeListener<String>(){
			SceneController sc = thisController;
			@Override
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
				if(newValue.equals("Global statistics")){
					application.update(new ModelUpdateEvent(sc, "requestGlobalStats"));
				}else if(newValue.equals("Session statistics")){
					application.update(new ModelUpdateEvent(sc, "requestSessionStats"));
				}
			}
		});
	}
	/**
	 * Listener for quit to main menu navigation button
	 * @param me MouseEvent
	 */
	@FXML
	public void quitToMainMenu(MouseEvent me){
		application.requestSceneChange("mainMenu");
	}
	/**
	 * Listener for clear statistics button to clear statistics
	 * @param me MouseEvent
	 */
	@FXML
	public void clearStats(MouseEvent me){
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle("Are you sure?");
        alert.setContentText("Your stats will be cleared! You can't undo this change.");
        Optional<ButtonType> response = alert.showAndWait();
        if(response.get()==ButtonType.OK){
        	application.update(new ModelUpdateEvent(this, "clearStats"));
        	barChartView.getData().clear();
    		barChartView.layout();
    		if(statsTable.getItems()!=null) statsTable.getItems().clear();statsTable.layout();
        }
	}
	@Override
	public void init(String[] args) {
		barChartView.setAnimated(false);
		barChartView.setLegendVisible(false);
		if(statsSelection.getSelectionModel().getSelectedItem().equals("Global statistics")){
		application.update(new ModelUpdateEvent(this, "requestGlobalStats"));
		}else if(statsSelection.getSelectionModel().getSelectedItem().equals("Session statistics")){
		application.update(new ModelUpdateEvent(this, "requestSessionStats"));
		}
	}
	/**
	 * 
	 * @param stats
	 * @return
	 */
	public ObservableList<Word> generateList(final UserStats stats){
		List<Word> wordlist = new LinkedList<Word>();
		for(String key : stats.getKeys()){
			int mastered = stats.getStat(Type.MASTERED, key);
			int failed = stats.getStat(Type.FAILED, key);
			int faulted = stats.getStat(Type.FAULTED, key);
			if(mastered+failed+faulted==0){continue;}
			wordlist.add(new Word(key,stats.getLevel(key),mastered,failed,faulted,Math.round(mastered/(double)(mastered+failed+faulted)*100)));
		}
		return new ObservableListWrapper<Word>(wordlist);
	}
	/**
	 * Helper method for when the statistics change.
	 * Updates the chart with the new data accordingly.
	 * Detailed stats are created (in another thread) and displayed in a text area.
	 * @param stats
	 */
	private void statsChange(final UserStats stats){
		barChartView.getData().clear();
		final int mastered=stats.getTotalStatsOfType(Type.MASTERED);
		final int faulted=stats.getTotalStatsOfType(Type.FAULTED);
		final int failed=stats.getTotalStatsOfType(Type.FAILED);
		final int total = ((mastered+faulted+failed)==0)?1:mastered+faulted+failed;
		XYChart.Series<String, Number> series1 = new XYChart.Series<>();
		XYChart.Data<String,Number> masteredData = new XYChart.Data<String,Number>("Total Mastered", mastered*100/(double)total);
		XYChart.Data<String,Number> faultedData = new XYChart.Data<String,Number>("Total Faulted", faulted*100/(double)total);
		XYChart.Data<String,Number> failedData = new XYChart.Data<String,Number>("Total Failed", failed*100/(double)total);
		
		series1.getData().add(masteredData);
		series1.getData().add(faultedData);
		series1.getData().add(failedData);
		barChartView.getData().add(series1);
		masteredData.getNode().setStyle("-fx-bar-fill: #33cc66;");
		masteredData.getNode().setOnMouseEntered(e -> {
			masteredData.setExtraValue(mastered*100/(double)total);
		});
		faultedData.getNode().setStyle("-fx-bar-fill: #ffcc66;");
		failedData.getNode().setStyle("-fx-bar-fill: #cc6666;");
		Task<ObservableList<Word>> loader = new Task<ObservableList<Word>>(){
			protected ObservableList<Word> call() throws Exception {
				return generateList(stats);
			}
			public void succeeded(){
				try {
					ObservableList<Word> obs = this.get();
					
					TableColumn<Word,String> levelColumn = new TableColumn<Word,String>("Level");
					levelColumn.setCellValueFactory(new PropertyValueFactory<Word,String>("lProperty"));
					levelColumn.setSortType(SortType.ASCENDING);
					levelColumn.setSortable(true);
					TableColumn<Word,String> wordColumn = new TableColumn<Word,String>("Word");
					wordColumn.setSortable(true);
					wordColumn.setCellValueFactory(new PropertyValueFactory<Word,String>("wProperty"));
					TableColumn<Word,Integer> masteredColumn = new TableColumn<Word,Integer>("Mastered");
					masteredColumn.setCellValueFactory(new PropertyValueFactory<Word,Integer>("mProperty"));
					TableColumn<Word,Integer> faultedColumn = new TableColumn<Word,Integer>("Faulted");
					faultedColumn.setCellValueFactory(new PropertyValueFactory<Word,Integer>("faultProperty"));
					TableColumn<Word,Integer> failedColumn = new TableColumn<Word,Integer>("Failed");
					failedColumn.setCellValueFactory(new PropertyValueFactory<Word,Integer>("failProperty"));
					TableColumn<Word,Double> masteryColumn = new TableColumn<Word,Double>("Mastery");
					masteryColumn.setCellValueFactory(new PropertyValueFactory<Word,Double>("masteryProperty"));
					statsTable.getColumns().clear();
					
					//add each one once instead of using addAll, which results in non-typed adding
					statsTable.getColumns().add(levelColumn);
					statsTable.getColumns().add(wordColumn);
					statsTable.getColumns().add(masteredColumn);
					statsTable.getColumns().add(faultedColumn);
					statsTable.getColumns().add(failedColumn);
					statsTable.getColumns().add(masteryColumn);
					statsTable.setItems(obs);
					//predicate for words
					
					//make obs filterable
					FilteredList<Word> filterable = new FilteredList<>(obs, p-> {
						String newData = filterField.textProperty().get();
						if(newData==null||newData.isEmpty()) return true;
						String lowerFilter = newData.toLowerCase();
						if(lowerFilter.matches("^[0-9]+")){
							if(Integer.parseInt(lowerFilter)==p.getLProperty()){
								return true;
							}
						}
						if(p.getWProperty().contains(lowerFilter)){
							return true;
						}
						return false;
					});
					filterField.textProperty().addListener((o,oldData,newData) -> {
						filterable.setPredicate(p -> {
							if(newData==null||newData.isEmpty()) return true;
							String lowerFilter = newData.toLowerCase();
							if(lowerFilter.matches("^[0-9]+")){
								if(Integer.parseInt(lowerFilter)==p.getLProperty()){
									return true;
								}
							}
							if(p.getWProperty().contains(lowerFilter)){
								return true;
							}
							return false;
						});
						
					});
					SortedList<Word> sorted = new SortedList<>(filterable);
					sorted.comparatorProperty().bind(statsTable.comparatorProperty());
					statsTable.setItems(sorted);
					
				} catch (InterruptedException | ExecutionException e) {
					e.printStackTrace();
				}
			}
		};
		new Thread(loader).run();
	}
	
	@Override
	public void onModelChange(String notificationString, Object... objectParameters) {
		switch(notificationString){
		case "globalStatsLoaded":
			statsChange((UserStats)objectParameters[0]);
			break;
		case "sessionStatsLoaded":
			statsChange((UserStats)objectParameters[0]);
			break;
		}
	}
	@Override
	public void cleanup() {
	}

}
