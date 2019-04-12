package biznasearch.search_engine;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.spell.Dictionary;
import org.apache.lucene.search.spell.LuceneDictionary;
import org.apache.lucene.search.spell.SpellChecker;
import org.apache.lucene.search.suggest.DocumentDictionary;
import org.apache.lucene.search.suggest.FileDictionary;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class SpellCheckerIndexer {

    private String indexDir;
//    private Analyzer analyzer;
    private SpellChecker spell;

    public SpellCheckerIndexer(String indexDir) throws IOException {

        this.indexDir = indexDir;
        spellIndexBusinessName();

    }

    public void spellIndexBusinessName() throws IOException {
        Path path = Paths.get(indexDir, "spellCheck");
        Directory dir = FSDirectory.open(path);
        DirectoryReader indexReader = DirectoryReader.open(FSDirectory.open(Paths.get(indexDir,"businesses")));
        Dictionary dictionary = new LuceneDictionary(indexReader,"business_name");
        IndexWriterConfig config = new IndexWriterConfig(new StandardAnalyzer());
        spell = new SpellChecker(dir);
        spell.indexDictionary(dictionary,config,false);
        System.out.println(">>> success ");

    }

    public boolean isNull(){
        return this.spell == null;
    }

    public String[] getSimmilars(String word, int num) throws IOException {
        return spell.suggestSimilar(word, num);
    }

}
