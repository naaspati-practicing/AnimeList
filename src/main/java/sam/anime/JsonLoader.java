package sam.anime;

import static sam.anime.Utils.readSystemResourceLines;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.UnaryOperator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
public class JsonLoader {
	private final Logger logger = LoggerFactory.getLogger(getClass());
	private final UnaryOperator<String>[] idExtractors;

	public JsonLoader() throws IOException {
		this.idExtractors = readSystemResourceLines("idExtractors.tsv", stream -> 
			stream.map(s -> {
				int n = s.indexOf('\t');
				if(n < 0) {
					if(!s.trim().isEmpty())
						logger.error("bad line: {}", s);
					
					return null;
				} else 
					return new IdExtractor(s.substring(0, n), s.substring(n+1));
			})
			.filter(Objects::nonNull)
			.toArray(IdExtractor[]::new)
		);
	}

	private static class IdExtractor implements UnaryOperator<String> {
		final String u1, u2, ret;

		public IdExtractor(String url, String ret) {
			this.u1 = "https://".concat(url);
			this.u2 = "http://".concat(url);

			this.ret = ret.concat(":");
		}

		@Override
		public String apply(String t) {
			if(t.startsWith(u1) || t.startsWith(u2))
				return ret.concat(t.substring(t.lastIndexOf('/') + 1));
			return null;
		}
	}

	private static JsonLoader parser;

	public AnimeJsonEntries parse(Path jsonfile) throws IOException {
		parser = this;
		AnimeJsonEntries data = new Gson().fromJson(Files.newBufferedReader(jsonfile), AnimeJsonEntries.class);
		for (AnimeEntry e : data.data) 
			e.id(); // load ids

		parser = null;
		return data; 
	}

	public static String id(String[] sources) {
		for (UnaryOperator<String> extractor : parser.idExtractors) {
			for (Object os : sources) {
				String s = (String)os;
				String k = extractor.apply(s);

				if(k != null)
					return k;
			}
		}
		return null;
	}

}
