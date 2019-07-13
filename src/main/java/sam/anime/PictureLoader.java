package sam.anime;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.nio.file.StandardOpenOption.READ;
import static javafx.application.Platform.runLater;
import static sam.anime.Utils.SELF_DIR_KEY;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import sam.fx.alert.FxAlert;
import sam.internetutils.InternetUtils;
import sam.nopkg.EnsureSingleton;
import sam.reference.WeakPool;

@Singleton
public class PictureLoader {
	private static final EnsureSingleton SINGLETON = new EnsureSingleton();
	{ SINGLETON.init(); }
	
	private final Path pictures;
	private final WeakPool<InternetUtils> pool;
	private final Executor executor;
	
	@Inject
	public PictureLoader(@Named(SELF_DIR_KEY) Path selfDir, Executor executor) throws IOException {
		this.executor = executor;
		
		pictures = selfDir.resolve("pictures");
		
		Files.createDirectories(pictures);
		pool = new WeakPool<>(true, InternetUtils::new);
	}
	
	private Path p;
	private AnimeEntry current; 

	public Image forAnime(AnimeEntry e) {
		computePath(e);
		if(Files.notExists(p))
			return null;
		
		try {
			return new Image(Files.newInputStream(p, READ));
		} catch (IOException e1) {
			FxAlert.showErrorDialog(p, "failed to load img", e1);
		}
		return null;
	}

	private void computePath(AnimeEntry e) {
		if(e != current) {
			p = pictures.resolve(e.id().replace(':', '_'));
			current = e;
		}
	}

	public void download(AnimeEntry e, Consumer<Image> onComplete) {
		computePath(e);
		Path p = this.p;
		
		executor.execute(() -> {
			InternetUtils internet = pool.poll();
			
				try {
					Files.move(internet.download(e.picture), p, REPLACE_EXISTING);
					runLater(() -> onComplete.accept(forAnime(e)));
				} catch (Throwable e1) {
					runLater(() -> { FxAlert.showErrorDialog(p, "failed to download img", e1); onComplete.accept(null) ; });
				} finally {
					pool.add(internet);
				}
		});
	}

	public void set(AnimeEntry e, ImageView imgv) {
		Image img = forAnime(e);
		imgv.setImage(img);

		if(img == null) 
			download(e, imgv::setImage);
	}

}
