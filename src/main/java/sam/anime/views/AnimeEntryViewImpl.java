package sam.anime.views;

import static sam.anime.Utils.setAction;
import static sam.anime.Utils.wrap;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Stack;

import javax.inject.Inject;
import javax.inject.Provider;

import javafx.beans.binding.Bindings;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import sam.anime.AnimeData;
import sam.anime.AnimeEntry;
import sam.anime.AnimeJsonEntries;
import sam.anime.PictureLoader;
import sam.fx.alert.FxAlert;
import sam.fx.helpers.FxButton;
import sam.fx.helpers.FxConstants;
import sam.fx.helpers.FxCss;
import sam.fx.helpers.FxGridPane;
import sam.fx.helpers.FxHBox;

public class AnimeEntryViewImpl extends BorderPane {
	private final BackableTop title = new BackableTop();
	private final GridPane meta = FxGridPane.gridPane(5);
	private final ImageView picture = new ImageView();
	private final VBox center = new VBox(5);

	public final ListView<String> bindings = new ListView<>();
	public final ListView<String> sources = new ListView<>();
	public final ListView<String> synonyms = new ListView<>();
	public final ListView<String> relations = new ListView<>();
	private Runnable back;

	private final Wrap[] fields;

	private final Provider<DirsView> dirs;
	private final AnimeData animeData;
	private PictureLoader pictureLoader;

	private class Wrap extends Label {
		final Field field;

		public Wrap(String field, int row) {
			meta.addRow(row, new Text(String.format("%-12s: ", field)), this);
			this.field = wrap(() -> AnimeEntry.class.getDeclaredField(field));
			GridPane.setColumnSpan(this, GridPane.REMAINING);
			setWrapText(true);
		}
		public void set(AnimeEntry e) {
			try {
				setText(e == null ? null : String.valueOf(field.get(e)));
			} catch (IllegalArgumentException | IllegalAccessException e1) {
				throw new RuntimeException(e1);
			}
		}
	}

	@Inject
	public AnimeEntryViewImpl(Provider<AnimeJsonEntries> entries, Provider<DirsView> dirs, AnimeData animeData, PictureLoader pictureLoader) throws IOException {
		this.dirs = dirs;
		this.animeData = animeData;
		this.pictureLoader = pictureLoader;

		setTop(title);
		setLeft(picture);
		setBackground(FxCss.background(Color.WHITE));

		title.btn.setOnAction(e -> {
			if(history.isEmpty())
				back.run();
			else 
				set(history.pop());
		});

		picture.setFitWidth(150);
		picture.setPreserveRatio(true);

		center.setPadding(FxConstants.INSETS_5);
		BorderPane.setMargin(picture, FxConstants.INSETS_5);
		setStyle("-fx-font-family: 'Consolas'");

		center.getChildren().add( meta );

		addToCenter("Dir(s)/File(s)", bindings);
		addToCenter("sources", sources);
		addToCenter("synonyms", synonyms);
		addToCenter("relations", relations);

		setCenter(center);

		fields = new Wrap[]{
				new Wrap("title", 0),
				new Wrap("episodes", 1),
				new Wrap("type", 2)
		};

		setOnKeyReleased(e -> {
			switch (e.getCode()) {
				case BACK_SPACE:
					title.btn.fire();
					break;
				default:
					break;
			}
		});

		setAction(relations, () -> {
			AnimeEntry e = entries.get().findByUrl(relations.getSelectionModel().getSelectedItem());
			if(e == null)
				FxAlert.showErrorDialog(relations.getSelectionModel().getSelectedItem(), "no anime found for", null);
			else {
				history.push(current);
				set(e);
			} 
		}, null);

		setAction(bindings, () -> {
			String s = bindings.getSelectionModel().getSelectedItem();
			if(s != null)
				dirs.get().set(s);
		}, null);

		animeData.addOnchange((anime, file) -> {
			if(anime == current && !bindings.getItems().contains(file.subpathAsString()))
				bindings.getItems().add(file.subpathAsString());
		});
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void addToCenter(String title, ListView list) {
		Text t = new Text(title);
		t.textProperty().bind(Bindings.concat(title, " (", Bindings.size(list.getItems()), ")"));

		if(bindings == list) {
			HBox box = new HBox(t, FxHBox.maxPane(),FxButton.button("add", e -> addAction()));
			box.setPadding(FxConstants.INSETS_5);
			center.getChildren().add(box);
			box.setAlignment(Pos.CENTER_LEFT);
		} else {
			center.getChildren().add(t);
		}
		center.getChildren().add(list);
	}

	private void addAction() {
		dirs.get().selectFor(current, f -> {
			if(f != null)
				animeData.bind(current, f);
		});
	}

	private AnimeEntry current;
	private final Stack<AnimeEntry> history = new Stack<>();

	public void set(AnimeEntry e, Runnable back) {
		this.back = back;
		set(e);
	}

	private void set(AnimeEntry e) {
		if(e == current)
			return;

		this.current = e;

		if(e == null) {
			title.text.setText(null);
			for (Wrap w : fields) 
				w.setText(null);

			picture.setImage(null);

			sources.getItems().clear();
			relations.getItems().clear();
			synonyms.getItems().clear();
			bindings.getItems().clear();
		} else {
			title.text.setText(e.title);

			for (Wrap w : fields) 
				w.set(e);

			sources.getItems().setAll(e.sources);
			relations.getItems().setAll(e.relations);
			synonyms.getItems().setAll(e.synonyms);
			bindings.getItems().setAll(animeData.boundBy(current));

			pictureLoader.set(e, picture);
		}
	}
}
