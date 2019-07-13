package sam.anime.views;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.FlowPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import sam.anime.AnimeData;
import sam.anime.AnimeEntry;
import sam.anime.AnimeJsonEntries;
import sam.anime.PictureLoader;
import sam.anime.Utils;
import sam.anime.app.Title;
import sam.fx.helpers.FxConstants;
import sam.fx.helpers.FxCss;
import sam.nopkg.EnsureSingleton;

@Title("Bound Animes")
@Singleton
public class BoundAnimes extends BorderPane {
	private static final EnsureSingleton SINGLETON = new EnsureSingleton();
	{ SINGLETON.init(); }
	
	private final FlowPane root = new FlowPane(7,7);
	private final ScrollPane scrollPane = new ScrollPane(root);
	private final IdentityHashMap<AnimeEntry, Wrap> wraps = new IdentityHashMap<>();
	private final PictureLoader pictureLoader;
	private AnimeEntryView animeEntryView;

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Inject
	public BoundAnimes(AnimeJsonEntries entries, AnimeData animeData, PictureLoader pictureLoader, Provider<AnimeEntryView> animeEntryView) {
		this.pictureLoader = pictureLoader;
		setCenter(scrollPane);

		Text count = new Text();
		count.textProperty().bind(Bindings.size(root.getChildren()).asString());
		
		root.setPadding(FxConstants.INSETS_10);
		BorderPane.setMargin(count, FxConstants.INSETS_5);
		BorderPane.setAlignment(count, Pos.CENTER_RIGHT);
		
		setBottom(count);
		
		root.setAlignment(Pos.CENTER);
		scrollPane.setFitToWidth(true);

		List list = animeData.allBoundAnimes(entries);
		
		animeData.addOnchange((anime, file) -> {
			if(((Wrap)root.getChildren().get(0)).entry == anime)
				return;
			
			Wrap w = wraps.get(anime);
			if(w == null)
				w = new Wrap(anime);
			else 
				root.getChildren().remove(w);
			
			root.getChildren().add(0, w);
		});
		
		list.replaceAll(e -> new Wrap((AnimeEntry)e));
		root.getChildren().setAll(list);
		
		root.setOnMouseClicked(e -> {
			Object o =  e.getTarget();
			if(o instanceof Wrap) {
				Wrap w = (Wrap) o;
				if(selected != null)
					selected.setBorder(null);
				
				w.setBorder(selected_border);
				selected = w;
				
				if(e.getClickCount() > 1) {
					if(this.animeEntryView == null)
						this.animeEntryView = animeEntryView.get();
					
					this.animeEntryView.set(w.entry);
				}
			}
			e.consume();
		});
		
	}
	
	private final int WIDTH = 100;
	private final int HEIGHT = 150;
	private final Border border = FxCss.border(Color.LIGHTGRAY, BorderStrokeStyle.SOLID, new BorderWidths(1));
	private final List<BackgroundFill> fills = Collections.singletonList(new BackgroundFill(Color.WHITE, CornerRadii.EMPTY, Insets.EMPTY));
	private final Background text_bg = new Background(new BackgroundFill(Color.BLACK.deriveColor(0, 0, 0, 0.5), CornerRadii.EMPTY, Insets.EMPTY));
	private final int MAX_LEN = 17;
	private final Font font = Font.font("Consolas", 10);
	private Wrap selected;
	private final Border selected_border = FxCss.border(Color.CRIMSON);

	private class Wrap extends BorderPane {
		final Label text = new Label();
		final AnimeEntry entry;
		
		public Wrap(AnimeEntry e) {
			this.entry = e;
			wraps.put(e, this);
			
			setBottom(text);
			
			setWidth(WIDTH);
			setHeight(HEIGHT);
			
			setMaxWidth(WIDTH);
			setMaxHeight(HEIGHT);
			
			setMinWidth(WIDTH);
			setMinHeight(HEIGHT);

			setBackground(Utils.BG_WHITE);
			setBorder(border);

			text.setText(e.title.length() < MAX_LEN ? e.title : e.title.substring(0, MAX_LEN - 3).concat("..."));
			text.setWrapText(true);
			text.setAlignment(Pos.CENTER);
			text.setTextAlignment(TextAlignment.CENTER);
			text.setTextFill(Color.WHITE);
			text.setBackground(text_bg);
			text.setPrefWidth(WIDTH);
			text.setFont(font);
			
			setAlignment(text, Pos.CENTER);
			
			Image m = pictureLoader.forAnime(entry);
			if(m == null)
				pictureLoader.download(entry, this::setImg);
			else
				setImg(m);
		}

		private void setImg(Image m) {
			BackgroundImage bg  = new BackgroundImage(m, BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.DEFAULT, new BackgroundSize(BackgroundSize.AUTO, BackgroundSize.AUTO, true, true, true, false));
			setBackground(new Background(fills, Collections.singletonList(bg)));
		}
	}

}
