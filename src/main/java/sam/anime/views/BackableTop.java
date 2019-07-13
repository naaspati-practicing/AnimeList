package sam.anime.views;

import static sam.anime.Utils.BG_BLACK;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import sam.fx.helpers.FxConstants;
public class BackableTop extends HBox {
	final Button btn = new Button("<");
	final Label text = new Label();
	
	public BackableTop() {
		getChildren().addAll(btn, text);
		setAlignment(Pos.CENTER_LEFT);
		
		btn.setFont(Font.font("Consolas", FontWeight.EXTRA_BOLD, 25));
		btn.setPadding(new Insets(2, 5, 2, 5));
		btn.setTextFill(Color.WHITE);
		btn.setBackground(BG_BLACK);
		
		text.setFont(Font.font("Consolas"));
		text.setWrapText(true);
		
		btn.getStyleClass().clear();
		HBox.setMargin(text, FxConstants.INSETS_5);
	}

}
