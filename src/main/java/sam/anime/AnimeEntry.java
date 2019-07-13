package sam.anime;

import java.io.IOException;
import java.io.Serializable;

import sam.io.serilizers.DataReader;
import sam.io.serilizers.DataWriter;

public class AnimeEntry implements Serializable {
	private static final long serialVersionUID = 7763641333284801606L;

	private String id;
	public final String thumbnail;
	public final String type;
	public final String title;
	public final String picture;
	public final int episodes;
	
	public final String[] sources;
	public final String[] synonyms;
	public final String[] relations;
	
	public AnimeEntry(
			String thumbnail, 
			String[] sources, 
			String[] synonyms, 
			String type, 
			String title,
			String[] relations, 
			String picture, 
			int episodes) {
		
		this.thumbnail = thumbnail;
		this.sources = sources;
		this.synonyms = synonyms;
		this.type = type;
		this.title = title;
		this.relations = relations;
		this.picture = picture;
		this.episodes = episodes;
	}
	
	public AnimeEntry(DataReader d) throws IOException {
		this.id = d.readUTF();
		this.thumbnail = d.readUTF();
		this.type = d.readUTF();
		this.title = d.readUTF();
		this.picture = d.readUTF();
		this.episodes = d.readInt();
		this.sources = readArray(d);
		this.synonyms = readArray(d);
		this.relations = readArray(d);
	}

	private String[] readArray(DataReader d) throws IOException {
		String[] s = new String[d.readInt()];
		for (int i = 0; i < s.length; i++) 
			s[i] = d.readUTF();
		
		return s;
	}

	public String id() {
		if(id == null)
			 id = JsonLoader.id(sources);
		
		return id;
	};

	public void write(DataWriter d) throws IOException {
		 d.writeUTF(this.id());
		 d.writeUTF(this.thumbnail);
		 d.writeUTF(this.type);
		 d.writeUTF(this.title);
		 d.writeUTF(this.picture);
		 d.writeInt(this.episodes);
		 writeArray(d, this.sources);
		 writeArray(d, this.synonyms);
		 writeArray(d, this.relations);
	}

	private void writeArray(DataWriter d, String[] s) throws IOException {
		d.writeInt(s.length);
		
		for (int i = 0; i < s.length; i++) 
			d.writeUTF(s[i]);
	}
}
