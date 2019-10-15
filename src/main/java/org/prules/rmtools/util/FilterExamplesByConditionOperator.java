/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.prules.rmtools.util;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.set.MappedExampleSet;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetPassThroughRule;
import com.rapidminer.operator.ports.metadata.MDInteger;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeAttribute;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeStringCategory;
import com.rapidminer.parameter.UndefinedParameterError;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author Marcin
 */
public class FilterExamplesByConditionOperator extends Operator {
    
    public static final String PARAMETER_SELECT_ATTRIBUTE = "Select attribute";    
    public static final String PARAMETER_OPERATIONS = "Type of operation";
    public static final String PARAMETER_ONLY_FIRST = "Keep only first value";
    
    private InputPort inputExampleSetPort = getInputPorts().createPort("examplePort",ExampleSet.class);
    private OutputPort outputExampleSetPort = getOutputPorts().createPort("exampleSet");
    private OutputPort outputOriginalExampleSetPort = getOutputPorts().createPort("original");
    private MDInteger numberOfSamples = new MDInteger();
            
    public FilterExamplesByConditionOperator(OperatorDescription description) {
        super(description);
        numberOfSamples = new MDInteger();
        getTransformer().addPassThroughRule(inputExampleSetPort, outputOriginalExampleSetPort);
        getTransformer().addRule( new ExampleSetPassThroughRule(inputExampleSetPort, outputExampleSetPort, SetRelation.EQUAL){
            @Override
            public ExampleSetMetaData modifyExampleSet(ExampleSetMetaData metaData) throws UndefinedParameterError {
                ExampleSetMetaData md = super.modifyExampleSet(metaData);                
                md.setNumberOfExamples(numberOfSamples);
                return md;
            }            
        });
    }

    @Override
    public void doWork() throws OperatorException {
        super.doWork(); 
        ExampleSet exampleSet = inputExampleSetPort.getDataOrNull(ExampleSet.class);
        outputOriginalExampleSetPort.deliver(exampleSet);
        String attributeName = this.getParameterAsString(PARAMETER_SELECT_ATTRIBUTE);
        boolean onlyFirst = this.getParameterAsBoolean(PARAMETER_ONLY_FIRST);
        OperationTypes operation = OperationTypes.valueOf(this.getParameterAsString(PARAMETER_OPERATIONS));
        int index[] = operation.getExampleIndexesMatchingCondition(exampleSet, attributeName, onlyFirst);        
        numberOfSamples = new MDInteger(index.length);
        ExampleSet newExampleSet = new MappedExampleSet(exampleSet,index);
        outputExampleSetPort.deliver(newExampleSet);
    }
    
    /**
     * Creates configuration parameters 
     * @return 
     */
     @Override
    public List<ParameterType> getParameterTypes() {
        List<ParameterType> types = new LinkedList<>();
        types.add(new ParameterTypeAttribute(PARAMETER_SELECT_ATTRIBUTE, PARAMETER_SELECT_ATTRIBUTE, inputExampleSetPort));
        types.add(new ParameterTypeStringCategory(PARAMETER_OPERATIONS, PARAMETER_OPERATIONS, OperationTypes.getValueNames()));        
        types.add(new ParameterTypeBoolean(PARAMETER_ONLY_FIRST, PARAMETER_ONLY_FIRST, true));        
        types.addAll(super.getParameterTypes());
        //types.addAll(StrictDecimalFormat.getParameterTypes(this));
        return types;
    }
    
    
}
