package sam.anime.files;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Stack;
import java.util.function.Consumer;

import sam.io.serilizers.DataReader;
import sam.io.serilizers.DataWriter;
import sam.myutils.Checker;

public class Dir extends FileEntity {
	FileEntity[] children;

	public Dir(Path subpath, Dir parent, long lastmodified) {
		super(subpath, parent, lastmodified, -1);
	}
	public Dir(String name, Dir parent, long lastmodified) {
		super(name, parent, lastmodified, -1);
	}
	public Dir(DataReader reader, Dir parent) throws IOException {
		super(reader, parent);

		this.children = new FileEntity[reader.readInt()];
		for (int i = 0; i < children.length; i++) 
			children[i] = reader.readBoolean() ? new Dir(reader, this) : new FileEntity(reader, this);
	}

	@Override
	public void write(DataWriter w) throws IOException {
		super.write(w);

		w.writeInt(children.length);

		for (FileEntity f : children) 
			f.write(w);
	}

	@Override
	public long size() {
		if(size < 0) {
			if(Checker.isEmpty(children))
				size = 0;
			else
				size = Arrays.stream(children).mapToLong(d -> d.size()).sum();
		}

		return size;
	}

	int count = -1, dcount = -1;

	private void count() {
		count = 0;
		dcount = 0;
		
		if(Checker.isEmpty(children))
			return;

		for (FileEntity f : children) {
			if(f.isDir()) {
				count += ((Dir)f).deepFileCount();
				dcount += ((Dir)f).deepDirCount();
				dcount++;
			} else 
				count++;
		}
	}

	public int deepFileCount() {
		if(count < 0)
			count();

		return count;
	}
	
	public int deepDirCount() {
		if(dcount < 0)
			count();

		return dcount;
	}

	@Override
	public boolean isDir() {
		return true;
	}
	public void print(String indent, Appendable sb) throws IOException {
		Arrays.sort(children, Comparator.comparing(FileEntity::name));

		for (FileEntity f : children) {
			sb.append(indent).append(f.name()).append('\n');
			if(f.isDir())
				((Dir)f).print(indent.concat("  "), sb);
		}
	}

	public void walkFileTree(Consumer<FileEntity> consumer) {
		Stack<Dir> dirs = new Stack<>();
		dirs.add(this);

		while(!dirs.isEmpty()) {
			dirs.pop()
			.forEach(d -> {
				if(d.isDir())
					dirs.add((Dir) d);
				consumer.accept(d);
			});
		}
	}

	public void forEach(Consumer<FileEntity> consumer) {
		for (FileEntity f : children)
			consumer.accept(f);
	}
	@Override
	public String toString() {
		return name + "|" + lastmodified;
	}

	private List<FileEntity> list;
	public List<FileEntity> getChildren() {
		if(list == null)
			list = Collections.unmodifiableList(Arrays.asList(children));

		return list;
	}
	public int length() {
		return children.length;
	}
}
