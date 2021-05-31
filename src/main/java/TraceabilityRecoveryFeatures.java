public class TraceabilityRecoveryFeatures {

    private String document1;
    private String document2;
    private double[] features_document1_query = new double[Util.FEATURE_COUNT];
    private double[] features_document2_query = new double[Util.FEATURE_COUNT];
    public float[] doc1_as_query_sims = new float[Similarities.values().length];
    public float[] doc2_as_query_sims = new float[Similarities.values().length];

    public TraceabilityRecoveryFeatures(String document1, String document2) {
        this.document1 = document1;
        this.document2 = document2;
    }

    public void setSimilarityScore(float score, String document, int i){
        if(document.equals(document1)) doc1_as_query_sims[i] = score;
        if(document.equals(document2)) doc2_as_query_sims[i] = score;
    }
}
