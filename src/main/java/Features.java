public class Features {
    // Define the Features via Numbers for easy Array access, first the 21 pre query features
    public final static int AVG_IDF = 0;
    public final static int MAX_IDF = 1;
    public final static int DEV_IDF = 2;
    public final static int AVG_ICTF = 3;
    public final static int MAX_ICTF = 4;
    public final static int DEV_ICTF = 5;
    public final static int AVG_ENTROPY = 6;
    public final static int MED_ENTROPY = 7;
    public final static int MAX_ENTROPY = 8;
    public final static int DEV_ENTROPY = 9;
    public final static int QUERY_SCOPE = 10;
    public final static int SIMPLE_CLAR_SCORE = 11;
    public final static int AVG_VAR = 12;
    public final static int MAX_VAR = 13;
    public final static int SUM_VAR = 14;
    public final static int COHE_SCORE = 15;
    public final static int AVG_SCQ = 16;
    public final static int MAX_SCQ = 17;
    public final static int SUM_SCQ = 18;
    public final static int AVG_PMI = 19;
    public final static int MAX_PMI = 20;
    // now the seven post retrieval query features
    public final static int SUBQUERY_OL = 21;
    public final static int ROBUSTNESS_SCORE = 22;
    public final static int FIRST_RANK_CHANGE = 23;
    public final static int CLUST_TENDENCY = 24;
    public final static int SPATIAL_AUTOCORR = 25;
    public final static int WEIGHTED_INFORMATION_GAIN = 26;
    public final static int NORMALIZED_QUERY_COMMITMENT = 27;
}
