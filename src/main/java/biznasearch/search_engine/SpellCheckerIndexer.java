package biznasearch.search_engine;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.spell.LuceneDictionary;
import org.apache.lucene.search.spell.PlainTextDictionary;
import org.apache.lucene.search.spell.SpellChecker;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Paths;

public class SpellCheckerIndexer {

    private String indexDir;
    private Analyzer analyzer;
    private SpellChecker spellChecker;

    public SpellCheckerIndexer(String indexDir){

        this.indexDir = indexDir;

    }

    public void spellIndexBusinessName() throws IOException {
        analyzer = new StandardAnalyzer();
        Directory dir = FSDirectory.open(Paths.get(indexDir, "spellCheck"));
        IndexWriterConfig config = new IndexWriterConfig(analyzer);

        spellChecker = new SpellChecker(dir);
        spellChecker.indexDictionary(new PlainTextDictionary(Paths.get(indexDir, "spellCheck")),config,false);

    }

    public String[] getSimmilars(String word, int num) throws IOException {
        return spellChecker.suggestSimilar(word, num);
    }

}
