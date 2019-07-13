package sam.anime.files;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import sam.anime.Utils;
import sam.io.serilizers.DataReader;

public class RootDir extends Dir {
	
	public RootDir() {
		super((String)null, null, -1);
		this.subpath = Paths.get("");
	}

	public RootDir(DataReader reader) throws IOException {
		this(reader.readBoolean(), reader); // reader.readBoolean() is needed 
		this.subpath = Paths.get("");
	}

	private RootDir(boolean nothing, DataReader reader) throws IOException {
		super(reader, null);
	}
	@Override
	public String subpathAsString() {
		return "";
	}
	
	private transient Map<String, FileEntity> bysubpath = new HashMap<>();

	public FileEntity findBySubpath(String subpath) {
		FileEntity f = bysubpath.get(subpath);
		
		if(f != null)
			return f;
		
		int size = bysubpath.size();
		
		try {
			if(size == 0) {
				walkFileTree(d -> {
					if(d.isDir())
						bysubpath.putIfAbsent(d.subpathAsString(), d);
				});
				
				f = bysubpath.get(subpath);
				
				Utils.rootLogger().debug("{} {}: listed dirs only", getClass(), "bysubpath");
				
				if(f != null)
					return f;
			}
			
			Stack<Dir> dirs = new Stack<>();
			dirs.add(this);
			
			while(!dirs.isEmpty()) {
				for (FileEntity d : dirs.pop().children) {
					if(d.isDir())
						dirs.add((Dir) d);
					
					bysubpath.putIfAbsent(d.subpathAsString(), d);
					
					if(d.subpathAsString().equals(subpath)) 
						return d;
				}
			}
			
			return null;
		} finally {
			Utils.rootLogger().debug("{} {}: {} -> {}", getClass(), "bysubpath", size, bysubpath.size());
		}
	}
}
