package sam.anime;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.scene.control.ListView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Background;
import javafx.scene.paint.Color;
import sam.fx.helpers.FxCss;

public interface Utils {
	
	Background BG_BLACK = FxCss.background(Color.BLACK);
	Background BG_GRAY = FxCss.background(Color.GRAY);
	Background BG_WHITE = FxCss.background(Color.WHITE);
	String SELF_DIR_KEY = "SELF_DIR";

	static <E> E readSystemResourceLines(String name, Function<Stream<String>, E> mapper) throws IOException {
		try(InputStream is = ClassLoader.getSystemResourceAsStream(name);
				InputStreamReader isr = new InputStreamReader(is);
				BufferedReader reader = new BufferedReader(isr)) {

			return mapper.apply(
					reader.lines()
					.filter(s -> !s.isEmpty() && s.charAt(0) != '#')
					);
		}
	}
	
	static <E> E wrap(Callable<E> callable) {
		try {
			return callable.call();
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	static Logger rootLogger() {
		return LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
	}

	static Stream<String> lines(Path p) throws IOException {
		return Files.lines(p)
				.filter(s -> !s.isEmpty() && s.charAt(0) != '#');
	}

	@SuppressWarnings("rawtypes")
	static void setAction(ListView list, Runnable action, Consumer<KeyEvent> keyReleasedHandler) {
		list.setOnMouseClicked(e -> {
			if(e.getClickCount() >  1)
				action.run();
		});
		
		list.setOnKeyReleased(e -> {
			if(keyReleasedHandler != null)
			keyReleasedHandler.accept(e);
			else if(e.getCode() == KeyCode.ENTER)
				action.run();
		});
	}

	static Path cachePath(Path p) {
		return p.resolveSibling(p.getFileName().toString().concat(".cache"));
	}

}
