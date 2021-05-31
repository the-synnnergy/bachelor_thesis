import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.MutablePair;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class FeatureCalc {
    private List<ImmutablePair<String,String>> corpus1;
    private List<ImmutablePair<String,String>> corpus2;
    private Map<ImmutablePair<String,String>,TraceabilityRecoveryFeatures> documents_to_features;
    public FeatureCalc(List<ImmutablePair<String, String>> corpus1, List<ImmutablePair<String, String>> corpus2) throws IOException {
        this.corpus1 = corpus1;
        this.corpus2 = corpus2;
        for(ImmutablePair<String,String> pair1 : corpus1){
            for(ImmutablePair <String,String> pair2 : corpus2){
                documents_to_features.put(new ImmutablePair<>(pair1.left, pair2.left), new TraceabilityRecoveryFeatures(pair1.left,pair2.left));
            }
        }
    }

    public MutablePair<Map,Map> calculate_Features() throws IOException {
        SimilarityCalc[] corpus1_SimCalc = new SimilarityCalc[Similarities.values().length];
        SimilarityCalc[] corpus2_SimCalc = new SimilarityCalc[Similarities.values().length];
        for(Similarities sim : Similarities.values()){
            corpus1_SimCalc[sim.ordinal()] = new SimilarityCalc(corpus1, sim);
            corpus2_SimCalc[sim.ordinal()] = new SimilarityCalc(corpus2, sim);
        }
        for(ImmutablePair<String,String> document : corpus1){
            float[] sims = new float[Similarities.values().length];
            for (int i = 0; i< corpus2_SimCalc.length;i++){
                Map<String, Float> results = corpus2_SimCalc[i].getCalculatedSimilarities(document.right);
                int finalI = i;
                results.forEach((k, v) -> add_Similarity_to_Feature(document.left,k,v, finalI));
            }
        }
        return null;
    }

    private void add_Similarity_to_Feature(String document1, String document2, float score, int similarity){
        documents_to_features.get(new ImmutablePair<String,String>(document1,document2)).setSimilarityScore(score,document1,similarity);
    }
}