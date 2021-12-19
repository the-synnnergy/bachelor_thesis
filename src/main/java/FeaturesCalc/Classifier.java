package FeaturesCalc;

import weka.classifiers.trees.RandomForest;
import weka.core.Instances;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

public class Classifier
{
    /***
     *
     * @param instances
     * @return
     * @throws Exception
     */
    public static RandomForest buildClassifier(Instances instances) throws Exception
    {
        RandomForest rf = new RandomForest();
        rf.buildClassifier(instances);
        return rf;
    }

    public static void saveClassifier(RandomForest rf, String filename) throws IOException
    {
        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filename));
        oos.writeObject(rf);
        oos.flush();
        oos.close();
    }
}
