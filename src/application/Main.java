package application;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ExecutionException;

import controller.IntroController;
import controller.LevelController;
import controller.QuizController;
import controller.SceneController;
import controller.StatsController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import resources.StoredStats;

/**
 * Main entry class (Application) This class is the entry to the JavaFX
 * application Acts as the application model
 * 
 * @author Mohan Cao
 * @author Ryan MacMillan
 *
 */
public class Main extends Application implements MainInterface {
	private Map<String, Parent> screens; // maps keys to scenes
	private Map<String, FXMLLoader> screenFXMLs; // maps keys to fxmlloaders,
													// needed to get controllers
	private SceneController currentController; // current controller to
												// displayed scene
	private StatisticsModel statsModel;
	private Game game;
	private Queue<Task<Integer>> festivalTasks;
	private FestivalService festivalService;
	private boolean _firstTimeRun;
	Stage _stage;
	{
		screens = new HashMap<String, Parent>();
		screenFXMLs = new HashMap<String, FXMLLoader>();
		_firstTimeRun = false;
		statsModel = new StatisticsModel(this);
		_firstTimeRun = statsModel.isFirstTime();
		festivalService = new FestivalService();
		festivalTasks = new LinkedList<Task<Integer>>();
	}

	@Override
	public void start(Stage primaryStage) {
		this._stage = primaryStage;
		buildMainScenes();
		try {
			primaryStage.setTitle("VoxSpell v1.1.0");
			if(_firstTimeRun){
				requestSceneChange("firstTime");
			}else{
				requestSceneChange("mainMenu");
			}
			primaryStage.show();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void stop() {
		currentController.cleanup();
		statsModel.sessionEnd();
		festivalService.cleanup();
	}

	public Object loadObjectFromFile(String path) {
		try {
			File file = new File(path);
			if (!file.exists()) {
				return null;
			}
			FileInputStream fileIn = new FileInputStream(file);
			ObjectInputStream instr = new ObjectInputStream(fileIn);
			Object obj = instr.readObject();
			instr.close();
			return obj;
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (InvalidClassException ice) {
			writeObjectToFile(path, new StoredStats());
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public boolean writeObjectToFile(String path, Object obj) {
		try {
			File file = new File(path);
			FileOutputStream fileout = new FileOutputStream(file);
			ObjectOutputStream outstr = new ObjectOutputStream(fileout);
			outstr.writeObject(obj);
			outstr.close();
			fileout.close();
			return true;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * Exports internal resource to file system
	 * @param resource path of file in jar
	 * @param location location of file to export to
	 * @throws IOException
	 * @author Mohan Cao
	 * @author Ryan MacMillan
	 */
	private void exportResource(String resource, String newFilePath) throws IOException {
		InputStream stream = null;
		OutputStream resStreamOut = null;
		try {
			stream = getClass().getResourceAsStream(resource);
			if(stream == null)throw new IOException("Failed to get resource " + resource);
			int readBytes;
			byte[] buffer = new byte[4096];
			resStreamOut = new FileOutputStream(newFilePath);
			while ((readBytes = stream.read(buffer)) > 0) {
				resStreamOut.write(buffer, 0, readBytes);
			}
		} catch (Exception ex) {
			throw ex;
		} finally {
			if(stream!=null)stream.close();
			if(resStreamOut!=null)resStreamOut.close();
		}
	}
	/**
	 * Builds scenes for the application
	 */
	private void buildMainScenes() {
		BufferedReader br = null;
		try {
			br = new BufferedReader(
					new InputStreamReader(this.getClass().getClassLoader().getResourceAsStream("config.cfg")));
			String line;
			String[] strSplit;
			while ((line = br.readLine()) != null) {
				strSplit = line.split(",");
				try {
					URL loc;
					FXMLLoader fxml = null;
					Parent menu = null;
					if ((loc = getClass().getClassLoader().getResource(strSplit[1])) != null) {
						fxml = new FXMLLoader(loc);
						menu = (Parent) fxml.load();
						screens.put(strSplit[0], menu);
						screenFXMLs.put(strSplit[0], fxml);
					}

				} catch (IOException ioex) {
					System.err.println("Scene loading error");
					ioex.printStackTrace();
				}
			}
		} catch (IOException e) {

			throw new RuntimeException("Config files corrupted");
		} finally {
			try {
				if (br != null) {
					br.close();
				}
			} catch (IOException e1) {
			}
		}
	}

	public Collection<String> getAvailableSceneKeys() {
		return screens.keySet();
	}

	/**
	 * Request scene change, by default the current stage, with data parameters
	 */
	public boolean requestSceneChange(String key, String... data) {
		boolean success = false;
		if (screens.containsKey(key)) {
			currentController = screenFXMLs.get(key).getController();
			currentController.setApplication(this);
			success = requestSceneChange(key, _stage, data);
			currentController.init(data);
		}
		return success;
	}

	/**
	 * Request scene change in particular stage with data parameters Does not
	 * initialise the controller
	 * 
	 * @param key
	 * @param stage
	 * @param data
	 * @return
	 */
	public boolean requestSceneChange(String key, Stage stage, String... data) {
		if (screens.containsKey(key)) {
			if(stage.getScene()==null){
				stage.setScene(new Scene(screens.get(key)));
			}else{
				stage.getScene().setRoot(screens.get(key));
			}
			stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
				public void handle(WindowEvent event) {
					if (game != null && !game.onExit()) {
						event.consume();
					}
				}
			});
			return true;
		}
		return false;
	}

	public void tell(String message, Object... objectParams) {
		// propagate + notify currentController (view-controller) of changes
		currentController.onModelChange(message, objectParams);
	}
	/**
	 * Festival service class.
	 * 
	 * @author Mohan Cao
	 *
	 */
	class FestivalService extends Service<Integer> {
		private Process _pb;
		private String _voice;
		private String[] _words;
		private int _speed;
		{
			try {
				Process p = new ProcessBuilder("/bin/bash", "-c", "type -p festival").start();
				BufferedReader isr = new BufferedReader(new InputStreamReader(p.getInputStream()));
				String output = isr.readLine();
				if (output == null || output.isEmpty()) {
					Alert alert = new Alert(AlertType.ERROR);
					alert.setContentText("Could not find Festival text-to-speech\nsynthesiser. Sorry about that.");
					alert.showAndWait();
					//Platform.exit();
				}else{
					_pb = new ProcessBuilder(output).start();
				}
			} catch (IOException e) {
				System.err.println("IOException");
			}
		}

		public final void setWordsToList(int speed, String... words) {
			_words = words;
			_speed = speed;
		}

		public void cleanup() {
			if(_pb!=null)_pb.destroy();
		}
		
		public final void setVoice(String voice) {
			_voice = voice;
		}

		@Override
		protected Task<Integer> createTask() {
			final String voice = _voice;
			final String[] words = _words;
			final int speed = _speed;
			return new Task<Integer>() {
				protected Integer call() throws Exception {
					BufferedWriter bw = new BufferedWriter(new PrintWriter(_pb.getOutputStream()));
					for (int i = 0; i < words.length; i++) {
						bw.write("(Parameter.set 'Duration_Stretch " + speed + ")");
						bw.write("(voice_" + voice + ")");
						bw.write("(SayText \"" + words[i] + "\")");
					}
					bw.flush();
					return 0;
				}

				public void succeeded() {
					try {
						if (!festivalTasks.isEmpty()&&get()==0) {
							Task<Integer> task = festivalTasks.poll();
							new Thread(task).start();
						}
					} catch (InterruptedException | ExecutionException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			};
		}

	}

	/**
	 * Creates a new process of Festival that says a word
	 * 
	 * @param speed list of speeds of the words being said
	 * @param voiceType voice to use (kal_diphone or akl_nz_jdt_diphone)
	 * @param words list of words to be said
	 */
	public void sayWord(final int speed, final String voiceType, final String... words) {
		festivalService.setVoice(voiceType);
		festivalService.setWordsToList(speed, words);
		if (!festivalTasks.isEmpty()) {
			festivalTasks.add(festivalService.createTask());
		}
		Task<Integer> festivalTask = festivalService.createTask();
		new Thread(festivalTask).start();
	}

	/**
	 * Called by scene controller to update the main application
	 * 
	 * @param mue ModelUpdateEvent
	 */
	public void update(ModelUpdateEvent mue) {
		// Game must be updated
		if (mue.getControllerClass().equals(GameUpdater.class)) {
			game = mue.getUpdatedGame();
		}
		mue.setMain(this);
		mue.setGame(game);
		mue.setStatsModel(statsModel);
		if (mue.getControllerClass().equals(QuizController.class)) {
			mue.updateFromQuizController(screens, screenFXMLs);
		} else if (mue.getControllerClass().equals(StatsController.class)) {
			mue.updateFromStatsController();
		} else if (mue.getControllerClass().equals(LevelController.class)) {
			mue.updateFromLevelController();
		} else if (mue.getControllerClass().equals(IntroController.class)){
			mue.updateFromIntroController();
		}
	}

	public static void main(String[] args) {
		launch(args);
	}
}
