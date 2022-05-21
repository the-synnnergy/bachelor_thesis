package FeaturesCalc;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class InstanceData
{
    double[] sim_scores_query = new double[FeaturesCalc.Similarities.values().length];
    double[] sim_scores_target = new double[FeaturesCalc.Similarities.values().length];
    PostQueryCalc.PostQueryFeatures[] postq_features_query = new PostQueryCalc.PostQueryFeatures[FeaturesCalc.Similarities.values().length];
    PreQueryCalc.PrequeryFeatures[] preq_features_query = new PreQueryCalc.PrequeryFeatures[FeaturesCalc.Similarities.values().length];
    PostQueryCalc.PostQueryFeatures[] postq_features_target = new PostQueryCalc.PostQueryFeatures[FeaturesCalc.Similarities.values().length];
    PreQueryCalc.PrequeryFeatures[] preq_features_target = new PreQueryCalc.PrequeryFeatures[FeaturesCalc.Similarities.values().length];
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
            for (PreQueryCalc.PrequeryFeatures preq_feature : preq_features_query)
            {
                String name = sim.name() + "_query_preq_";
                features.addAll(preq_feature.to_ArrayList_named(name));
            }
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
            for (PreQueryCalc.PrequeryFeatures preq_feature : preq_features_target)
            {
                String name = sim.name() + "_target_preq_";
                features.addAll(preq_feature.to_ArrayList_named(name));
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

    public Double[] getAttributeValues()
    {
        List<Double> attributes = new ArrayList<>();
        for (FeaturesCalc.Similarities sim : FeaturesCalc.Similarities.values())
        {
            attributes.add(sim_scores_query[sim.ordinal()]);
        }
        for (FeaturesCalc.Similarities sim : FeaturesCalc.Similarities.values())
        {
            attributes.add(sim_scores_target[sim.ordinal()]);
        }


    }

    public String[] attributesNames()
    {
        return null;
    }
}
