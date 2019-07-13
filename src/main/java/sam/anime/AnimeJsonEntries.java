package sam.anime;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;
import static sam.anime.Utils.rootLogger;

import java.io.IOException;
import java.io.Serializable;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.function.Consumer;

import sam.io.serilizers.DataReader;
import sam.io.serilizers.DataWriter;
import sam.nopkg.Resources;
public class AnimeJsonEntries implements Serializable {
	private static final long serialVersionUID = 8477870661296610635L;

	final AnimeEntry[] data;
	private transient HashMap<String, AnimeEntry>  byurl ;
	private transient HashMap<String, AnimeEntry>  byid ;

	public AnimeJsonEntries(AnimeEntry[] data) {
		this.data = data;
	}

	public AnimeJsonEntries(Path cache) throws IOException {
		try(FileChannel fc = FileChannel.open(cache, READ);
				Resources r = Resources.get();
				DataReader d = new DataReader(fc, r.buffer());
				) {

			if(d.readLong() != serialVersionUID)
				throw new IOException("bad cache file");

			d.setChars(r.chars());
			d.setDecoder(r.decoder());
			d.setStringBuilder(r.sb());

			this.data = new AnimeEntry[d.readInt()];

			for (int i = 0; i < data.length; i++) 
				data[i] = new AnimeEntry(d);
		}
	}

	public void forEach(Consumer<AnimeEntry> action) {
		for (AnimeEntry e : data) 
			action.accept(e);
	}

	public AnimeEntry findByUrl(String url) {
		if(byurl == null)  {
			byurl = new HashMap<>(data.length * 4);
			for (AnimeEntry e : data) {
				for (String u : e.sources) 
					byurl.put(u, e);
			}

			rootLogger().debug("filled buyurl");
		}

		return byurl.get(url);
	}

	public void write(Path cache) throws IOException {
		try(FileChannel fc = FileChannel.open(cache, WRITE, CREATE, TRUNCATE_EXISTING);
				Resources r = Resources.get();
				DataWriter dw = new DataWriter(fc, r.buffer());
				) {
			dw.setEncoder(r.encoder());

			dw.writeLong(serialVersionUID);
			dw.writeInt(data.length);

			for (AnimeEntry e : data) 
				e.write(dw);
		}
	}

	public AnimeEntry findById(String id) {
		if(byid == null)  {
			byid = new HashMap<>(data.length + 2);
			for (AnimeEntry e : data) 
				byid.put(e.id(), e);

			rootLogger().debug("filled buyurl");
		}
		
		return byid.get(id);
	}
}
