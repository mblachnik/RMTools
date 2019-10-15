/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.prules.rmtools.util;

import com.rapidminer.operator.Annotations;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ValueString;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeStringCategory;
import java.util.List;

/**
 *
 * @author Marcin
 */
public class Annotation2LogValue extends Operator {

        private InputPort inputPort = getInputPorts().createPort("input", IOObject.class);
	private OutputPort outputPort = getOutputPorts().createPort("output");
	public static final String PARAMETER_NAME = "annotation_name";

	private String currentValue = null;	

	public Annotation2LogValue(OperatorDescription description) {
		super(description);
                
		getTransformer().addPassThroughRule(inputPort, outputPort);

		addValue(new ValueString("annotationValue", "The value from the macro which should be logged.") {
			@Override
			public String getStringValue() {
				return currentValue;
			}
		});
	}

	@Override
	public void doWork() throws OperatorException {
                IOObject data = inputPort.getData(IOObject.class);
                Annotations annotations = data.getAnnotations();
		this.currentValue = annotations.get(getParameterAsString(PARAMETER_NAME));
		outputPort.deliver(data);
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeStringCategory(PARAMETER_NAME, "The name of the annotation", Annotations.ALL_KEYS_IOOBJECT, Annotations.ALL_KEYS_IOOBJECT[0]));
		return types;
	}
}
    
