package sam.anime;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;
import static sam.anime.Utils.SELF_DIR_KEY;
import static sam.anime.Utils.rootLogger;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import sam.anime.app.OnStopQueue;
import sam.anime.files.FileEntity;
import sam.nopkg.EnsureSingleton;
@Singleton
public class AnimeData {
	private static final EnsureSingleton SINGLETON = new EnsureSingleton();
	{ SINGLETON.init(); }
	
	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	private final Set<String> boundFiles = new HashSet<>();
	private final Map<String, Set<String>> bound;
	private boolean modified;
	private List<BiConsumer<AnimeEntry, FileEntity>> onchange;
	
	@Inject
	public AnimeData(OnStopQueue stopQueue, @Named(SELF_DIR_KEY) Path selfDir) throws JsonSyntaxException, JsonIOException, IOException {
		Path p = selfDir.resolve(getClass().getName().concat(".bound.json"));
		this.bound = Files.notExists(p) ? new HashMap<>() : new Gson().fromJson(Files.newBufferedReader(p), new TypeToken<HashMap<String, Set<String>>>(){}.getType());
		this.bound.forEach((s,t) -> boundFiles.addAll(t));
		
		stopQueue.runOnStop(() -> {
			if(!modified)
				return;
			
			try(BufferedWriter w = Files.newBufferedWriter(p, WRITE, TRUNCATE_EXISTING, CREATE)) {
				new GsonBuilder()
				.setPrettyPrinting()
				.create()
				.toJson(bound, w);
				
			} catch (Throwable e) {
				rootLogger().error("failed to save {}: {}", getClass(), p, e);
			}
		});
	}
	
	public void bind(AnimeEntry anime, FileEntity file) {
		Set<String> set = bound.get(anime.id());
		if(set == null)
			bound.put(anime.id(), set = new HashSet<>());
		
		if(set.add(file.subpathAsString())) {
			boundFiles.add(file.subpathAsString());
			modified = true;
			
			if(onchange != null)
				onchange.forEach(c -> c.accept(anime, file));
			
			logger.debug("bind \"{}\" -> \"{}\"", anime.title, file.subpathAsString());
		}
	}
	
	public void addOnchange(BiConsumer<AnimeEntry, FileEntity> onchange) {
		if(this.onchange == null )
			this.onchange = new ArrayList<>();
		
		this.onchange.add(onchange);
	}
	
	public Collection<String> boundBy(AnimeEntry anime) {
		return anime == null ? null : bound.getOrDefault(anime.id(), Collections.emptySet());
	}
	public boolean isBound(FileEntity f) {
		return f == null ? false : boundFiles.contains(f.subpathAsString());
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public List<AnimeEntry> allBoundAnimes(AnimeJsonEntries entries) {
		ArrayList list = new ArrayList(bound.keySet());
		list.replaceAll(s -> entries.findById((String)s));
		
		return list;
	}
	
}
