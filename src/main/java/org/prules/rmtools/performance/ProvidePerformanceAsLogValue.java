/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.prules.rmtools.performance;

import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ValueDouble;
import com.rapidminer.operator.performance.PerformanceVector;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeList;
import com.rapidminer.parameter.ParameterTypeString;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * @author Marcin
 */
public class ProvidePerformanceAsLogValue extends Operator {

    /**
     * This class estimate performance of any rule or tree learner. If the input 
     * object is a tree the output is number of nodes in the tree 
     * The parameter name for &quot;Indicates if the fitness should for maximal
     * or minimal number of features.&quot;
     */
    public static final String PARAMETER_CRITERIA_LIST = "Performance masure name";
    public static final String PARAMETER_CRITERIA_NAMES = "Any text";

    private double[] performanceValues;

    
    private InputPort performanceInput = getInputPorts().createPort("performance vector");
    
    private OutputPort performanceOutput = getOutputPorts().createPort("performance vector");

    public ProvidePerformanceAsLogValue(OperatorDescription description) {
        super(description);

        getTransformer().addGenerationRule(performanceOutput, PerformanceVector.class);        
        this.performanceValues = new double[]{Double.NaN, Double.NaN, Double.NaN}; 
        
        addValue(new ValueDouble("performance1", "The value of performance") {
            @Override
            public double getDoubleValue() {
                return performanceValues[0];
            }
        });
        addValue(new ValueDouble("performance2", "The value of performance") {
            @Override
            public double getDoubleValue() {
                return performanceValues[1];
            }
        });
        addValue(new ValueDouble("performance3", "The value of performance") {
            @Override
            public double getDoubleValue() {
                return performanceValues[2];
            }
        });
    }

    @Override
    public void doWork() throws OperatorException {        
        PerformanceVector performance = performanceInput.getDataOrNull(PerformanceVector.class);

        String[] criteriaNames = performance.getCriteriaNames();
        Set<String> set = new HashSet<>(criteriaNames.length);
        for(String s : criteriaNames){
            set.add(s);
        }
        
        
        List<String[]> list = getParameterList(PARAMETER_CRITERIA_LIST);
        String[] criterias = new String[list.size()];
        this.performanceValues = new double[]{Double.NaN, Double.NaN, Double.NaN}; 
        int i = 0;
        for (String[] ss : list){
            String s = ss[0];
            if (!set.contains(s)){ //Check if criterion exists if not than error
                throw new OperatorException("Performance not found on the list");
            }
            this.performanceValues[i] = performance.getCriterion(s).getFitness();
            i++;
            if (i>this.performanceValues.length) { break; };
        }
        
        
        performanceOutput.deliver(performance);
    }
    
    @Override
    public List<ParameterType> getParameterTypes() {
        List<ParameterType> types = super.getParameterTypes();
        ParameterType tmp = null;
        ParameterType params = new ParameterTypeList(PARAMETER_CRITERIA_LIST, "Kust of criteria names which will be provided as log value", new ParameterTypeString(PARAMETER_CRITERIA_NAMES,"Put here any text or value. This value is ignored, but required."));
        types.add(params);
        return types;
    }
}
