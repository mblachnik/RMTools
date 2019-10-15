/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.prules.rmtools.weights;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.AttributeWeights;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.OperatorCapability;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.features.weighting.AbstractWeighting;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeAttribute;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Marcin
 */
public class Data2Weights extends AbstractWeighting {

    public static final String PARAMETER_WEIGHTS = "Weights_attribute";
    public static final String PARAMETER_NAMES = "Attribute_names_attribute";

    public Data2Weights(OperatorDescription description) {
        super(description, false);
    }

    @Override
    protected AttributeWeights calculateWeights(ExampleSet exampleSet) throws OperatorException {
        AttributeWeights weights = new AttributeWeights();
        Attributes attr = exampleSet.getAttributes();
        String attributeNamesStr = getParameterAsString(PARAMETER_NAMES);
        String attributeWeightsStr = getParameterAsString(PARAMETER_WEIGHTS);
        Attribute attributeNames = attr.get(attributeNamesStr);
        Attribute attributeWeights = attr.get(attributeWeightsStr);
        for (Example e : exampleSet) {
            String tmpAttrName = e.getValueAsString(attributeNames);
            double tmpAttrWeight = e.getValue(attributeWeights);
            weights.setWeight(tmpAttrName, tmpAttrWeight);
        }
        return weights;
    }

    @Override
    public boolean supportsCapability(OperatorCapability capability) {
//        switch (capability) {
//            case NUMERICAL_ATTRIBUTES:
//            case BINOMINAL_ATTRIBUTES:
//            case POLYNOMINAL_ATTRIBUTES:
//                return true;
//            default:
//                return false;
//        }
        return true;
    }

    @Override
    public List<ParameterType> getParameterTypes() {
        List<ParameterType> types = new ArrayList<>();
        types.add(new ParameterTypeAttribute(PARAMETER_NAMES, "Attribute containing names of attributes", this.getExampleSetInputPort()));
        types.add(new ParameterTypeAttribute(PARAMETER_WEIGHTS, "Attribute containing weights of attributes", this.getExampleSetInputPort()));                
        types.addAll(super.getParameterTypes());
        
        return types;
    }
}
