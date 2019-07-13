package sam.anime.files;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sam.io.serilizers.DataReader;
import sam.io.serilizers.DataWriter;
import sam.nopkg.Resources;
import sam.string.StringSplitIterator;

public class Walker {
	private final Logger logger = LoggerFactory.getLogger(getClass());
	private static final long MARKER = 1562869353468L;

	private class DTemp {
		final Dir dir;
		final List<FileEntity> files = new ArrayList<>();

		public DTemp(Dir dir) {
			this.dir = dir;
		}

	}
	
	public RootDir read(Path walked_file) throws IOException {
		return read(walked_file, true);
	}

	public RootDir read(Path walked_file, boolean loadCacheIfFound) throws IOException {
		Path cachePath = walked_file.resolveSibling(walked_file.getFileName()+".cached");
		RootDir ret = loadCacheIfFound ? readCached(cachePath) : null;
		
		if(ret != null)
			return ret;
		
		Map<Path, DTemp> dirs = new HashMap<>();

		DTemp root = new DTemp(new RootDir());
		dirs.put(null, root);

		Files.lines(walked_file)
		.forEach(new Consumer<String>() {
			Path path;
			DTemp dir = root;

			List<String> list = new ArrayList<>();

			@Override
			public void accept(String t) {
				if(t.isEmpty())
					return;

				list.clear();
				new StringSplitIterator(t, '|').forEachRemaining(list::add);

				if(list.size() < 2)
					logger.error("bad line: \"{}\"", t);
				else {
					Path p = Paths.get(list.get(0));
					FileEntity f;
					Path pp = p.getNameCount() == 1 ? null : p.getParent(); 
					dir = Objects.equals(pp, path) ? dir : dirs.get(pp);
					path = pp;

					if(list.size() == 2) {
						f = new Dir(p, dir.dir, Long.parseLong(list.get(1)));
						dirs.put(p, new DTemp((Dir)f));
					} else if(list.size() == 3)
						f = new FileEntity(p, dir.dir, Long.parseLong(list.get(1)), Long.parseLong(list.get(2)));
					else
						throw new RuntimeException();

					dir.files.add(f);
				}
			}
		});

		Comparator<FileEntity> comparator = (a, b) -> {
			if(a.isDir() == b.isDir())
				return a.name().compareTo(b.name());
			else 
				return a.isDir() ? -1 : 1;
		};
		
		dirs.forEach((s,t) -> {
			t.dir.children = t.files.toArray(new FileEntity[t.files.size()]);
			Arrays.sort(t.dir.children, comparator);
		});
		
		if(loadCacheIfFound) 
			writeCache(cachePath, (RootDir)root.dir);			
		 else 
			logger.debug("read raw: {}", walked_file);
		
		return (RootDir) root.dir;
	}

	public void writeCache(Path cachePath, RootDir dir) throws IOException {
		try(FileChannel fc = FileChannel.open(cachePath, WRITE, CREATE, TRUNCATE_EXISTING);
				Resources r = Resources.get();
				DataWriter w = new DataWriter(fc, r.buffer())) {
			
			w.setEncoder(r.encoder());
			
			w.writeLong(MARKER);
			dir.write(w);
			
			logger.debug("write cache: {}", cachePath);
		}
	}

	public RootDir readCached(Path cachePath) throws IOException {
		if(Files.notExists(cachePath))
			return null;
		
		try(FileChannel fc = FileChannel.open(cachePath, READ);
				Resources r = Resources.get();
				DataReader reader = new DataReader(fc, r.buffer())) {
			
			if(reader.readLong() != MARKER)
				throw new IOException("bad cache file: \""+cachePath+"\"");
			
			reader.setChars(r.chars());
			reader.setDecoder(r.decoder());
			reader.setStringBuilder(r.sb());
			
			logger.debug("loaded cache: {}", cachePath); 
			
			return new RootDir(reader);
		}
	}
}
