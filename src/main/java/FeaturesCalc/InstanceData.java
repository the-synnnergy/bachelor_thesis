package FeaturesCalc;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.w3c.dom.Attr;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class InstanceData
{
    double[] sim_scores_query = new double[FeaturesCalc.Similarities.values().length];
    double[] sim_scores_target = new double[FeaturesCalc.Similarities.values().length];
    PostQueryCalc.PostQueryFeatures[] postq_features_query = new PostQueryCalc.PostQueryFeatures[FeaturesCalc.Similarities.values().length];
    PreQueryCalc.PrequeryFeatures preq_features_query;
    PostQueryCalc.PostQueryFeatures[] postq_features_target = new PostQueryCalc.PostQueryFeatures[FeaturesCalc.Similarities.values().length];
    PreQueryCalc.PrequeryFeatures preq_features_target;
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
            for (PostQueryCalc.PostQueryFeatures postq_feature : postq_features_query)
            {
                String name = sim.name() + "_query_preq_";
                features.addAll(postq_feature.to_ArrayList_named(name));
            }
        }

        for (FeaturesCalc.Similarities sim : FeaturesCalc.Similarities.values())
        {
            for (PostQueryCalc.PostQueryFeatures postq_feature : postq_features_target)
            {
                String name = sim.name() + "_target_preq_";
                features.addAll(postq_feature.to_ArrayList_named(name));
            }
        }
        return features;
    }

    @Override
    public String toString()
    {
        // does not work #TODO fix
        List<Pair<String, Double>> list = get_iterableList();
        return list.stream().map(a -> String.valueOf(a.getRight())).collect(Collectors.joining(","));
    }

    public String getIdentifierQuery() {
        return this.identifier_query;
    }

    public String getIdentifierTarget(){
        return this.identifier_target;
    }

   public Instance getUnlabeledWekaInstance()
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
       // #TODO fix calculcating preq features only!
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
        System.out.println(AttributeValues.size());
        DenseInstance instance = new DenseInstance(AttributeValues.size());
        instance = (DenseInstance) instance.copy(AttributeValues.stream().mapToDouble(Double::doubleValue).toArray());
        return instance;
   }

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
       AttributesList.addAll(PreQueryCalc.PrequeryFeatures.getWekaAttributesNames());
       AttributesList.addAll(PostQueryCalc.PostQueryFeatures.getWekaAttributesNames());

       return AttributesList;
   }
}
