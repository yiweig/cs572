package project1;

import org.apache.lucene.search.similarities.BasicStats;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.search.similarities.SimilarityBase;

/**
 * Created by yiwei on 2/10/16.
 */
public class TfIdfSimilarity extends SimilarityBase {
    @Override
    protected float score(BasicStats basicStats, float v, float v1) {
        return 0;
    }

    @Override
    public String toString() {
        return null;
    }
}
