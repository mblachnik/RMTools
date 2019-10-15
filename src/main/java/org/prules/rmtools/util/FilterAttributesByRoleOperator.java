/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.prules.rmtools.util;

import com.rapidminer.example.AttributeRole;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.ExampleSetPassThroughRule;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeRegexp;
import com.rapidminer.parameter.UndefinedParameterError;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author Marcin
 */
public class FilterAttributesByRoleOperator extends Operator {

    private InputPort inputPort = getInputPorts().createPort("input", ExampleSet.class);
    private OutputPort outputPort = getOutputPorts().createPort("output");
    private OutputPort oriOutputPort = getOutputPorts().createPort("original");
    //private static final String[] TARGET_ROLES = new String[]{Attributes.LABEL_NAME, Attributes.ID_NAME,
    //     Attributes.PREDICTION_NAME, Attributes.CLUSTER_NAME, Attributes.WEIGHT_NAME, Attributes.BATCH_NAME};
    public static final String PARAMETER_ROLE_REGEXP = "Role_regular_expr.";    
    String attributeName;

    public FilterAttributesByRoleOperator(OperatorDescription description) {
        super(description);        
        getTransformer().addRule(new ExampleSetPassThroughRule(inputPort, outputPort, SetRelation.EQUAL));
        getTransformer().addRule(new ExampleSetPassThroughRule(inputPort, oriOutputPort, SetRelation.EQUAL));
    }

    @Override
    public void doWork() throws OperatorException {
        ExampleSet data = inputPort.getData(ExampleSet.class);
        String roleExpression = getParameter(PARAMETER_ROLE_REGEXP);
        if (roleExpression == null) {
            throw new UndefinedParameterError(PARAMETER_ROLE_REGEXP, this);
        }
        ExampleSet newData = (ExampleSet)data.clone();
        Iterator<AttributeRole> iter = newData.getAttributes().allAttributeRoles();
                
        while (iter.hasNext()){
            AttributeRole a = iter.next();
            
            if (!(a.isSpecial() && a.getSpecialName().matches(roleExpression))){
                iter.remove();
            }            
        }                                
        oriOutputPort.deliver(data);
        outputPort.deliver(newData);
    }

    @Override
    public List<ParameterType> getParameterTypes() {

        List<ParameterType> types = super.getParameterTypes();

        ParameterType type;
        type = new ParameterTypeRegexp(PARAMETER_ROLE_REGEXP,"Regular expression used to filter attributes by role");
        types.add(type);
        
        return types;
    }
}
