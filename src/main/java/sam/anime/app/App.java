package sam.anime.app;
import static sam.anime.Utils.BG_BLACK;
import static sam.anime.Utils.cachePath;
import static sam.anime.Utils.readSystemResourceLines;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Named;

import org.codejargon.feather.Feather;
import org.codejargon.feather.Provides;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.Background;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.stage.Stage;
import sam.anime.AnimeEntry;
import sam.anime.AnimeJsonEntries;
import sam.anime.JsonLoader;
import sam.anime.Utils;
import sam.anime.files.RootDir;
import sam.anime.files.Walker;
import sam.anime.views.AnimeEntryView;
import sam.anime.views.AnimeEntryViewImpl;
import sam.fx.alert.FxAlert;
import sam.fx.helpers.FxFxml;
import sam.fx.helpers.FxUtils;
import sam.myutils.MyUtilsPath;
import sam.myutils.System2;
public class App extends Application implements OnStopQueue {
	private final Logger logger = LoggerFactory.getLogger(getClass());

	private Stage stage;
	private Feather feather;
	private BorderPane root;
	private ExecutorService thread;
	private ListView<Wrap> list;

	@SuppressWarnings({"rawtypes", "unchecked"})
	private class Wrap {
		private final Class cls;
		private final String title;
		private Node instance;

		public Wrap(String s) {
			try {
				this.cls = Class.forName(s);
			} catch (ClassNotFoundException e) {
				throw new RuntimeException(e);
			}

			Title t = (Title) cls.getAnnotation(Title.class);
			this.title = t == null ? cls.getSimpleName() : t.value();
		}

		public Node instance() {
			if(instance == null)
				instance = (Node) feather.instance(cls);
			return instance;
		}
	}

	@Override
	public void start(Stage stage) throws Exception {
		this.stage = stage;
		FxAlert.setParent(stage);

		try {
			List<Object> modules = new ArrayList<>();
			modules.add(this);

			readSystemResourceLines("providers", 
					strm -> { strm.map(s -> Utils.wrap(() -> Class.forName(s).newInstance()))
						.forEach(modules::add); return null; });

			FxFxml.setFxmlDir(ClassLoader.getSystemResource("fxml"));
			this.feather = Feather.with(modules);

			Wrap[] wraps = readSystemResourceLines("views", strm -> strm.map(Wrap::new).toArray(Wrap[]::new));

			if(wraps.length == 0)
				throw new IllegalStateException("no views specified");

			list = new ListView<>(FXCollections.observableArrayList(wraps));

			list.setCellFactory(c -> new ListCell<Wrap>() {
				Background bgc;
				Paint fill;

				@Override
				protected void updateItem(Wrap item, boolean empty) {
					super.updateItem(item, empty);

					if(empty || item == null)
						setText(null);
					else 
						setText(item.title);
				}

				@Override
				public void updateSelected(boolean selected) {
					super.updateSelected(selected);

					if(fill == null) {
						fill = getTextFill();
						bgc = getBackground();
					}

					if(selected) {
						setBackground(BG_BLACK);
						setTextFill(Color.WHITE);	
					} else {
						setBackground(bgc);
						setTextFill(fill);
					}
				}
			});

			list.getSelectionModel().selectedItemProperty().addListener((p, o, n) -> setView(n));

			list.setMaxWidth(100);
			list.setPrefWidth(100);

			this.root = new BorderPane();
			this.root.setLeft(list);

			stage.setScene(new Scene(root));
			stage.setWidth(600);
			stage.setHeight(500);
			stage.show();
		} catch (Throwable e) {
			FxUtils.setErrorTa(stage, "failed to load feather", null, e);
			stage.show();
		}
	}

	private void setView(Wrap wrap) {
		if(wrap == null)
			setView((Node)null);
		else {
			if(wrap.instance == null) {
				setView(new Group(new ProgressIndicator()));
				Node node = wrap.instance();
				Platform.runLater(() -> setView(node));
			} else {
				setView(wrap.instance);
			}
		}

	}

	private void setView(Node node) {
		root.setCenter(node);
		if(node != null && node instanceof OnSelect)
			((OnSelect) node).onSelect();
	}

	private RootDir rootDir;

	@Provides
	public RootDir rootDir() {
		if(rootDir == null)
			rootDir = wrap("rootDir", () -> new Walker().read(Paths.get(System2.lookup("walked.file"))));

		return rootDir;
	}

	private AnimeJsonEntries entries; 

	@Provides
	public AnimeJsonEntries entries() {
		if(entries == null)  {
			Path p = Paths.get(System2.lookup("json.path"));
			Path cache = cachePath(p) ;
			
			if(Files.exists(cache)) {
				try {
					this.entries = new AnimeJsonEntries(cache);
					logger.debug("loaded {}", cache);
					return  this.entries;
				} catch (Throwable e) {
					logger.error("failed to load: {}", cache, e);
				}
			}
			
			this.entries = wrap("entries", () -> new JsonLoader().parse(p));
			logger.debug("loaded {}", p);
			
			try {
				this.entries.write(cache);
			} catch (IOException e) {
				logger.error("failed to write: {}", cache, e);
			}
		}
		
		return entries;
	}

	private <E> E wrap(String title, Callable<E> callable) {
		try {
			E e = callable.call();
			logger.info("loaded: "+ title);
			return e;
		} catch (Throwable e) {
			title = "failed to load "+title;
			FxUtils.setErrorTa(stage, title, title, e);
			return null;
		}
	}

	@Override
	public void stop() throws Exception {
		runOnStop.forEach(r -> {
			try {
				r.run();
			} catch (Throwable e) {
				logger.error("error", e);
			}
		});
		System.exit(0);
	}

	@Provides
	public Executor thread() {
		if(thread == null) {
			int count = Integer.parseInt(System2.lookup("THREAD_COUNT", "1").trim());
			thread = count < 2 ? Executors.newSingleThreadExecutor() : Executors.newFixedThreadPool(count);
		}
		return thread;
	} 

	private final List<Runnable> runOnStop = Collections.synchronizedList(new ArrayList<>());

	@Provides
	private OnStopQueue onStopQueue() {
		return this;
	} 

	private AnimeEntryView entryView;

	@Provides
	private AnimeEntryView animeEntryView() {
		if(entryView == null) {
			entryView = new AnimeEntryView() {
				Node back;
				final AnimeEntryViewImpl impl = feather.instance(AnimeEntryViewImpl.class);
				
				@Override
				public void set(AnimeEntry entry) {
					back = root.getCenter();
					root.setCenter(impl);
					
					impl.set(entry, () -> root.setCenter(back));
				}
			};
		}

		return entryView;
	} 

	@Override
	public void runOnStop(Runnable task) {
		runOnStop.add(Objects.requireNonNull(task));
	}
	
	private final CenterSetter centerSetter = new CenterSetter() {
		Node back;
		
		@Override
		public void setCenter(Node node, boolean disableList) {
			this.back = root.getCenter();
			root.setCenter(node);
			list.setDisable(disableList);
		}
		
		@Override
		public void back() {
			root.setCenter(back);
			list.setDisable(false);
		}
	};
	
	@Provides
	public CenterSetter centerSetter() {
		return centerSetter;
	}
	
	private Path selfDir = MyUtilsPath.selfDir();
	@Provides
	@Named(Utils.SELF_DIR_KEY)
	public Path selfDir() {
		return selfDir;
	}

}
