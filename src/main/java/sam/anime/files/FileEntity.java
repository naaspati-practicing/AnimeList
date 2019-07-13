package sam.anime.files;

import java.io.IOException;
import java.nio.file.Path;

import sam.io.serilizers.DataReader;
import sam.io.serilizers.DataWriter;

public class FileEntity {
	protected final String name;
	protected final Dir parent;
	protected long lastmodified;
	protected long size;
	protected Path subpath;
	
	public FileEntity(Path subpath, Dir parent, long lastmodified, long size) {
		this(subpath.getFileName().toString(), parent, lastmodified, size);
		this.subpath = subpath;
	}
	
	public FileEntity(String name, Dir parent, long lastmodified, long size) {
		this.name = name;
		this.parent = parent;
		this.lastmodified = lastmodified;
		this.size = size;
	}

	public FileEntity(DataReader reader, Dir parent) throws IOException {
		this.parent = parent;
		
		this.lastmodified = reader.readLong();
		this.size = reader.readLong();
		this.name = reader.readUTF();
	}
	public void write(DataWriter w) throws IOException {
		w.writeBoolean(isDir());
		w.writeLong(this.lastmodified);
		w.writeLong(this.size);
		w.writeUTF(this.name);
	}

	public long lastModified() {
		return lastmodified;
	}
	public String name() {
		return name;
	}
	public long size() {
		return size;
	}
	public boolean isDir() {
		return false;
	}
	public Path subpath() {
		if(subpath == null)
			subpath = parent.subpath().resolve(name);
			
		return subpath;
	}
	public Dir parent() {
		return parent;
	}
	@Override
	public String toString() {
		return name + "|" + lastmodified + "|" + size;
	}
	
	private String NULL_EXT = new String();

	private String ext;
	public String ext() {
		if(ext == null) {
			int n = name.lastIndexOf('.');
			if(n < 0)
				ext = NULL_EXT;
			else 
				ext = name.substring(n + 1).toLowerCase();
		}
		
		return ext == NULL_EXT ? null : ext;
	}
	
	private String subpathS;

	public String subpathAsString() {
		if(subpathS == null)
			subpathS = parent.subpathAsString() + "/" + name;
		
		return subpathS;
	}
}
