/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.prules.rmtools.util;

import com.rapidminer.example.AttributeRole;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ValueString;
import com.rapidminer.operator.error.ParameterError;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.ExampleSetPassThroughRule;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.ParameterTypeStringCategory;
import com.rapidminer.parameter.UndefinedParameterError;
import java.util.List;

/**
 *
 * @author Marcin
 */
public class ExtractAttributeNameByRoleOperator extends Operator {

    private InputPort inputPort = getInputPorts().createPort("input", ExampleSet.class);
    private OutputPort outputPort = getOutputPorts().createPort("output");
    private static final String[] TARGET_ROLES = new String[]{Attributes.LABEL_NAME, Attributes.ID_NAME,
        Attributes.PREDICTION_NAME, Attributes.CLUSTER_NAME, Attributes.WEIGHT_NAME, Attributes.BATCH_NAME};
    public static final String PARAMETER_TARGET_ROLE = "target_role";
    public static final String PARAMETER_MACRO_NAME = "macro_name";
    String attributeName;

    public ExtractAttributeNameByRoleOperator(OperatorDescription description) {
        super(description);
        this.addValue(new ValueString("Attribute_name", "Name of the attribute which hold given rule") {
            @Override
            public String getStringValue() {
                return attributeName;
            }
        });
        getTransformer().addRule(new ExampleSetPassThroughRule(inputPort, outputPort, SetRelation.EQUAL));
    }

    @Override
    public void doWork() throws OperatorException {
        ExampleSet data = inputPort.getData(ExampleSet.class);
        String role = getParameter(PARAMETER_TARGET_ROLE);
        if (role == null) {
            throw new UndefinedParameterError(PARAMETER_TARGET_ROLE, this);
        }
        AttributeRole attributeRole = data.getAttributes().findRoleBySpecialName(role);
        if (attributeRole != null) {
            attributeName = attributeRole.getAttribute().getName();
            String macro = getParameterAsString(PARAMETER_MACRO_NAME);
            if (role == null) {
                throw new UndefinedParameterError(PARAMETER_MACRO_NAME, this);
            }
            this.getProcess().getMacroHandler().addMacro(macro, attributeName);
        }        
        outputPort.deliver(data);
    }

    @Override
    public List<ParameterType> getParameterTypes() {

        List<ParameterType> types = super.getParameterTypes();

        ParameterType type;
        type = new ParameterTypeString(PARAMETER_MACRO_NAME, "The name of the macro which would hold the attribute name", "attribute_name");
        types.add(type);

        type = new ParameterTypeStringCategory(PARAMETER_TARGET_ROLE,
                "The target role of the attribute.", TARGET_ROLES, TARGET_ROLES[0]);
        types.add(type);
        return types;
    }
}
