package sam.anime.views;

import static sam.anime.Utils.setAction;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.inject.Inject;
import javax.inject.Singleton;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import sam.anime.AnimeEntry;
import sam.anime.AnimeJsonEntries;
import sam.anime.app.CenterSetter;
import sam.anime.app.Title;
import sam.anime.files.FileEntity;
import sam.fx.helpers.FxButton;
import sam.fx.helpers.FxCell;
import sam.fx.helpers.FxFxml;
import sam.fx.helpers.FxHBox;
import sam.fx.textsearch.FxTextSearch;
@Title("Anime List")
@Singleton
public class AnimeListView extends BorderPane {
	
	
	@FXML private TextField filterTF;
	@FXML private ListView<Wrap> list;
	private final FxTextSearch<Wrap> search;
	private final AnimeEntryView entryView;
	private CenterSetter centerSetter;
	
	private static class Wrap {
		final String title;
		final String key;
		final AnimeEntry entry;
		
		public Wrap(String title, AnimeEntry entry) {
			this.title = title;
			this.key = title.toLowerCase();
			this.entry = entry;
		}
	}
	
	@Inject
	public AnimeListView(AnimeJsonEntries entries, AnimeEntryView entryView, CenterSetter centerSetter) throws IOException {
		FxFxml.load(this, true);
		search = new FxTextSearch<>(s -> s.key, 300, true);
		this.entryView = entryView;
		this.centerSetter = centerSetter;
		
		List<Wrap> data = data(entries);
		
		list.setCellFactory(FxCell.listCell(e -> e.title));
		setAction(list, this::openAnimeEntryView, null);
		
		list.getItems().setAll(data); 
		filterTF.textProperty().addListener((p, o, n) -> search.addSearch(n));
		
		search.setAllData(data);
		search.setOnChange(() -> search.applyFilter(this.list.getItems()));
	}

	private void openAnimeEntryView() {
		entryView.set(selected());
	}

	private AnimeEntry selected() {
		Wrap w = list.getSelectionModel().getSelectedItem();
		return w == null ? null : w.entry;
	}

	private List<Wrap> data(AnimeJsonEntries entries) {
		int len[] = {0};
		entries.forEach(e -> len[0] += e.synonyms.length + 1);
		
		List<Wrap> data = new ArrayList<>(len[0] + 10);
		
		entries.forEach(e -> {
			data.add(new Wrap(e.title, e));
			
			for (String s : e.synonyms) 
				data.add(new Wrap(s, e));
		});
		
		return data;
	}
	
	private HBox buttonbox;
	private Label bindto;
	private Consumer<AnimeEntry> onComplete;
	private String back;
	
	public void selectFor(FileEntity file, Consumer<AnimeEntry> onComplete) {
		centerSetter.setCenter(this, true);
		this.onComplete = onComplete;
		this.back = filterTF.getText();

		if(buttonbox == null) {
			buttonbox = FxHBox.buttonBox(
					bindto = new Label(),
					FxHBox.maxPane(),
					FxButton.button("CANCEL", e -> oncomplete(null)), 
					FxButton.button("OK", e -> oncomplete(selected()))
					);
			
			bindto.setWrapText(true);
			buttonbox.getChildren().forEach(f -> {
				if(f instanceof Button)
					((Button)f).setPrefWidth(80);
			});
		}

		bindto.setText(file.subpathAsString());
		filterTF.setText(file.subpathAsString().replace('/', ' ').replace('\\', ' '));
		setBottom(buttonbox);
	}

	private void oncomplete(AnimeEntry f) {
		centerSetter.back();
		filterTF.setText(back);
		setBottom(null);

		Platform.runLater(() -> this.onComplete.accept(f));
	}
}
