import weka.core.tokenizers.*;

public class VectorSpaceModel {
    boolean stemming;
    public VectorSpaceModel(boolean stemming) {
        this.stemming = stemming;
    }

    public VectorSpaceModel() {
        this.stemming = true;
    }

}
