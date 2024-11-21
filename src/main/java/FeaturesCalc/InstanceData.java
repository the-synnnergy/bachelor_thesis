package FeaturesCalc;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import weka.core.Attribute;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

public class InstanceData
{
    /**
     * Datastructue for holding the calculated feature data for a pair of two artifacts.
     */
    double[] sim_scores_query = new double[FeaturesCalc.Similarities.values().length];
    double[] sim_scores_target = new double[FeaturesCalc.Similarities.values().length];
    PostRetrievalCalc.PostQueryFeatures[] postq_features_query = new PostRetrievalCalc.PostQueryFeatures[FeaturesCalc.Similarities.values().length];
    PreRetrievalCalc.PreretrievalFeatures preq_features_query;
    PostRetrievalCalc.PostQueryFeatures[] postq_features_target = new PostRetrievalCalc.PostQueryFeatures[FeaturesCalc.Similarities.values().length];
    PreRetrievalCalc.PreretrievalFeatures preq_features_target;
    double[] documentStatistics = new double[5];
    String identifier_query;
    String identifier_target;

    public InstanceData()
    {
    }

    public ArrayList<Pair<String, Double>> get_iterableList()
    {
        ArrayList<Pair<String, Double>> features = new ArrayList<>();
        for (FeaturesCalc.Similarities sim : FeaturesCalc.Similarities.values())
        {
            String name = sim.name() + "_query_score";
            features.add(new ImmutablePair<>(name, sim_scores_query[sim.ordinal()]));
        }
        for (FeaturesCalc.Similarities sim : FeaturesCalc.Similarities.values())
        {
            String name = sim.name() + "_target_score";
            features.add(new ImmutablePair<>(name, sim_scores_target[sim.ordinal()]));
        }

        for (FeaturesCalc.Similarities sim : FeaturesCalc.Similarities.values())
        {
            for (PostRetrievalCalc.PostQueryFeatures postq_feature : postq_features_query)
            {
                String name = sim.name() + "_query_preq_";
                features.addAll(postq_feature.toArrayListNamed(name));
            }
        }

        for (FeaturesCalc.Similarities sim : FeaturesCalc.Similarities.values())
        {
            for (PostRetrievalCalc.PostQueryFeatures postq_feature : postq_features_target)
            {
                String name = sim.name() + "_target_preq_";
                features.addAll(postq_feature.toArrayListNamed(name));
            }
        }
        return features;
    }

    @Override
    public String toString()
    {
        List<Pair<String, Double>> list = get_iterableList();
        return list.stream().map(a -> String.valueOf(a.getRight())).collect(Collectors.joining(","));
    }

    public String getIdentifierQuery() {
        return this.identifier_query;
    }

    public String getIdentifierTarget(){
        return this.identifier_target;
    }

    /**
     * get a list of doubles which can be used to create a weka instance, with no class label
     * @return values of features
     */
   public List<Double> getUnlabeledWekaInstance()
   {
       // do this with Instance(double[] data)
       // 2 is for the two PreQuery features!
       List<Double> AttributeValues = new ArrayList<>();
       for(FeaturesCalc.Similarities sim : FeaturesCalc.Similarities.values())
       {
           AttributeValues.add(sim_scores_query[sim.ordinal()]);
       }
       for(FeaturesCalc.Similarities sim : FeaturesCalc.Similarities.values())
       {
           AttributeValues.add(sim_scores_target[sim.ordinal()]);
       }
       AttributeValues.addAll(preq_features_query.getWekaAttributesValues());
       AttributeValues.addAll(preq_features_target.getWekaAttributesValues());
       for(FeaturesCalc.Similarities sim : FeaturesCalc.Similarities.values())
       {
           AttributeValues.addAll(postq_features_query[sim.ordinal()].getWekaAttributesValues());
       }
       for(FeaturesCalc.Similarities sim : FeaturesCalc.Similarities.values())
       {
           AttributeValues.addAll(postq_features_target[sim.ordinal()].getWekaAttributesValues());
       }
       AttributeValues.addAll(DoubleStream.of(documentStatistics).boxed().collect(Collectors.toList()));
       //AttributeValues.add(0.0d);
       System.out.println(AttributeValues.size());

       return AttributeValues;
   }

    /**
     * gets a list of attributes which can be used together with getUnlabeledWekaInstance to create weka instance
     * @return List of attributes
     */
   public static List<Attribute> attributesAsList()
   {
       List<Attribute> AttributesList = new ArrayList<>();
        for(FeaturesCalc.Similarities sim : FeaturesCalc.Similarities.values())
        {
            AttributesList.add(new Attribute("sim_score_query_" + sim.ordinal()));
        }
       for(FeaturesCalc.Similarities sim : FeaturesCalc.Similarities.values())
       {
           AttributesList.add(new Attribute("sim_score_target_" + sim.ordinal()));
       }
       AttributesList.addAll(PreRetrievalCalc.PreretrievalFeatures.getWekaAttributesNames());
       AttributesList.addAll(PostRetrievalCalc.PostQueryFeatures.getWekaAttributesNames());
       AttributesList.add(new Attribute("UniqueTermsQuery"));
       AttributesList.add(new Attribute("TotalTermsQuery"));
       AttributesList.add(new Attribute("UniqueTermsTarget"));
       AttributesList.add(new Attribute("TotalTermsTarget"));
       AttributesList.add(new Attribute("JaccardMeasure"));
       return AttributesList;
   }
}
