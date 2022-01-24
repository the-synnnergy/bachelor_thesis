package FeaturesCalc;

import weka.attributeSelection.AttributeSelection;
import weka.attributeSelection.CfsSubsetEval;
import weka.attributeSelection.GreedyStepwise;
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


    /**
     *  returns indices of indices of important features via Cfs Subseteval #TODO make more generic
     * @param instances
     * @return
     * @throws Exception
     */
    public static int[] get_subselected_features_indices(Instances instances) throws Exception
    {
        AttributeSelection attsel = new AttributeSelection();  // package weka.attributeSelection!
        CfsSubsetEval eval = new CfsSubsetEval();
        GreedyStepwise search = new GreedyStepwise();
        search.setSearchBackwards(true);
        attsel.setEvaluator(eval);
        attsel.setSearch(search);
        attsel.SelectAttributes(instances);
        // obtain the attribute indices that were selected
        return attsel.selectedAttributes();
    }


}
