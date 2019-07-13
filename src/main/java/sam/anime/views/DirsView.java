package sam.anime.views;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;
import static sam.anime.Utils.BG_BLACK;
import static sam.anime.Utils.BG_GRAY;
import static sam.anime.Utils.SELF_DIR_KEY;
import static sam.anime.Utils.lines;
import static sam.anime.Utils.readSystemResourceLines;
import static sam.anime.Utils.rootLogger;
import static sam.anime.Utils.wrap;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.Stack;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import sam.anime.AnimeData;
import sam.anime.AnimeEntry;
import sam.anime.app.CenterSetter;
import sam.anime.app.OnStopQueue;
import sam.anime.app.Title;
import sam.anime.files.Dir;
import sam.anime.files.FileEntity;
import sam.anime.files.RootDir;
import sam.fx.alert.FxAlert;
import sam.fx.helpers.FxButton;
import sam.fx.helpers.FxFxml;
import sam.fx.helpers.FxHBox;
import sam.fx.helpers.FxUtils;
import sam.myutils.MyUtilsBytes;
@Singleton
@Title("Dirs")
public class DirsView extends BorderPane {

	@FXML private Text count;
	@FXML private BackableTop top;
	@FXML private Button bindBtn;
	@FXML private Button hideBtn;
	@FXML private ListView<FileEntity> list;
	@FXML private HBox bottom;
	@FXML private CheckBox onlyunbindCB;
	private boolean onlyunbind;

	private Dir current;
	private Stack<Object> history = new Stack<>();
	private IdentityHashMap<FileEntity, String> currentlabels;
	private FileEntity selected;
	private StringBuilder sb;

	private IdentityHashMap<Dir, IdentityHashMap<FileEntity, String>> labels;
	private IdentityHashMap<Dir, String> dir_labels;
	private IdentityHashMap<Dir, FileEntity> scroll = new IdentityHashMap<>();
	private Set<String> hidden;
	private boolean hiddenMod;
	private AnimeData animeData;
	private CenterSetter centerSetter;
	private RootDir rootdir;
	private Provider<AnimeListView> animeList;
	private boolean refresh;

	@Inject
	public DirsView(RootDir rootdir, OnStopQueue stopQueue, Provider<AnimeData> animeDataP, CenterSetter centerSetter, Provider<AnimeListView> animeList,
			@Named(SELF_DIR_KEY) Path selfDir
			) {

		rootLogger().debug("init: {}", getClass());
		this.rootdir = rootdir;
		this.animeList = animeList;

		current = rootdir; 
		if(rootdir == null)
			return;

		this.centerSetter = centerSetter;

		try {
			Path p = selfDir.resolve(getClass().getName().concat(".hidden"));
			hidden = Files.notExists(p) ? new HashSet<>() : lines(p).collect(Collectors.toSet());

			FxFxml.load(this, true);

			stopQueue.runOnStop(() -> {
				if(hiddenMod) {
					try {
						Files.write(p, hidden, CREATE, TRUNCATE_EXISTING, WRITE);
						rootLogger().debug("saved: {}", p);
					} catch (IOException e1) {
						FxAlert.showErrorDialog(p, "failed to save", e1);
					}
				}
			});
		} catch (IOException e1) {
			setCenter(FxUtils.createErrorTa("fxml error", "failed to load fxml for "+getClass(), e1));
			return;
		}

		this.animeData = animeDataP.get();
		dir_labels = new IdentityHashMap<>();
		labels = new IdentityHashMap<>();

		sb = new StringBuilder();

		bindBtn.setDisable(true);
		hideBtn.setDisable(true);

		top.btn.setOnAction(e -> {
			if(history.isEmpty())
				set(rootdir);
			else {
				Object o = history.pop();
				if(o instanceof Dir)
					set((Dir)o);
				else  {
					centerSetter.back();
					top.btn.fire();
				}
			}
		});
		onlyunbindCB.selectedProperty().addListener((p, o, n) -> {
			onlyunbind = n;
			set(current);
		});

		int size = 15;

		Function<String, Image> img = s -> new Image(ClassLoader.getSystemResourceAsStream("icons/"+s), size, size, true, true); 

		Image folder = img.apply("folder.png");
		Image boundFolder = img.apply("multimedia.png");
		Image video = img.apply("video-file.png");
		Image file = img.apply("file.png");
		Image boundFile = img.apply("video-player.png");

		Set<String> exts =  wrap(() -> readSystemResourceLines("video.exts", s -> s.collect(Collectors.toSet())));

		list.setCellFactory(c -> new ListCell<FileEntity>() {
			Supplier<ImageView> d = supp(folder);
			Supplier<ImageView> f = supp(file);
			Supplier<ImageView> v = supp(video);
			Supplier<ImageView> bd = supp(boundFolder);
			Supplier<ImageView> bf = supp(boundFile);

			{
				setWrapText(true);
				setGraphicTextGap(10);
			}

			@Override
			protected void updateItem(FileEntity item, boolean empty) {
				super.updateItem(item, empty);

				if(empty || item == null) {
					setText(null);
					setGraphic(null);
				} else {
					if(!onlyunbind && animeData.isBound(item)) 
						setGraphic(item.isDir() ? bd.get() : bf.get());
					else 
						setGraphic(item.isDir() ? d.get() : exts.contains(item.ext()) ? v.get() : f.get());	

					setText(currentlabels.get(item));
				}
			}
		});

		list.getSelectionModel().selectedItemProperty().addListener((p, o, n) -> {
			selected = n;
			bindBtn.setDisable(selected == null);
			hideBtn.setDisable(selected == null);
		});

		list.setOnMouseClicked(e -> {
			if(e.getClickCount() > 1 && selected instanceof Dir)
				set((Dir)selected);
		});
		list.setOnKeyReleased(e -> {
			switch (e.getCode()) {
				case ENTER:
					set((Dir)selected);
					break;
				case BACK_SPACE:
					if(!top.btn.isDisable())
						top.btn.fire();
					break;
				default:
					break;
			}
		});

		list.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		count.textProperty().bind(Bindings.size(list.getItems()).asString());

		animeData.addOnchange((anime, file_) -> {
			if(refresh) {
				Platform.runLater(() -> {
					if(refresh)
						rfrsh();
				});
			}
		});
		set(current);
	}

	protected static Supplier<ImageView> supp(Image img) {
		return new Supplier<ImageView>() {
			ImageView m ;

			@Override
			public ImageView get() {
				if(m == null)
					m = new ImageView(img);

				return m;
			}
		};
	}

	@FXML
	private void bindAction(Event e) {
		animeList.get().selectFor(selected, f -> {
			if(f != null) {
				refresh = false;
				list.getSelectionModel().getSelectedItems().forEach(s -> animeData.bind(f, s));
				refresh = true;
				rfrsh();
			}
		});
	}
	private void rfrsh() {
		if(onlyunbind)
			list.getItems().removeIf(item -> animeData.isBound(item));
		list.refresh();
	}

	@FXML
	private void hideAction(Event e) {
		list.getSelectionModel()
		.getSelectedItems()
		.forEach(s -> hidden.add(s.subpathAsString()));

		hiddenMod = true;
		applyHidden();
	}

	private void applyHidden() {
		list.getItems().removeIf(f -> hidden.contains(f.subpathAsString()));
	}

	public String label(FileEntity item) {
		if(item.isDir())
			return dirLabel(false, (Dir)item);
		else {
			sb.setLength(0);
			sb.append(item.name()).append("   | ");
			MyUtilsBytes.bytesToHumanReadableUnits(item.size(), false, sb);
			return sb.toString();
		}
	}

	private void set(Dir dir) {
		scroll.put(current, list.getSelectionModel().getSelectedItem());

		this.current = dir;
		boolean b = dir instanceof RootDir; 
		top.btn.setDisable(b);
		top.btn.setBackground(b ? BG_GRAY : BG_BLACK);

		list.getSelectionModel().clearSelection();

		currentlabels = labels.get(dir);
		if(currentlabels == null) {
			currentlabels = new IdentityHashMap<>(dir.length());
			dir.forEach(d -> currentlabels.put(d, label(d)));
			labels.put(dir, currentlabels);
		}

		list.getItems().setAll(dir.getChildren());

		applyHidden();
		top.text.setText(dir_labels.computeIfAbsent(dir, d -> dirLabel(true, d)));

		rfrsh();

		FileEntity f = this.scroll.remove(dir);

		if(f != null) 
			select(f);

	}
	private String dirLabel(boolean subpath, Dir dir) {
		sb.setLength(0);
		sb.append(subpath ? dir.subpath().toString() : dir.name()).append("  | ")
		.append("D: ").append(dir.deepDirCount())
		.append(", F: ").append(dir.deepFileCount()).append(" | ");

		MyUtilsBytes.bytesToHumanReadableUnits(dir.size(), false, sb);
		return sb.toString();
	}

	private HBox buttonbox;
	private Label bindto;
	private Consumer<FileEntity> onComplete;
	private Dir back;

	public void selectFor(AnimeEntry entry, Consumer<FileEntity> onComplete) {
		centerSetter.setCenter(this, true);
		this.onComplete = onComplete;
		this.back = this.current;

		if(buttonbox == null) {
			buttonbox = FxHBox.buttonBox(
					bindto = new Label(),
					FxHBox.maxPane(),
					FxButton.button("CANCEL", e -> oncomplete(null)), 
					FxButton.button("OK", e -> oncomplete(selected))
					);

			bindto.setWrapText(true);
			buttonbox.getChildren().forEach(f -> {
				if(f instanceof Button)
					f.minWidth(70);
			});
		}

		bindto.setText(entry.title);
		set(rootdir);
		setBottom(buttonbox);
	}

	private void oncomplete(FileEntity f) {
		this.onComplete.accept(f);
		centerSetter.back();
		set(back);
		setBottom(bottom);
	}
	
	private static final Object OBJECT = new Object();
	
	public void set(String subpath) {
		FileEntity f = rootdir.findBySubpath(subpath);

		if(f == null)
			FxAlert.showErrorDialog(subpath, "no Dir found with subpath", null);
		else {
			centerSetter.setCenter(this, true);
			
			if(f.isDir())
				set((Dir)f);
			else {
				set(f.parent());
				Platform.runLater(() -> select(f));
			}
			
			history.push(OBJECT);
		}
	}

	private void select(FileEntity f) {
		list.getSelectionModel().clearSelection();
		list.getSelectionModel().select(f);	
	}
}
