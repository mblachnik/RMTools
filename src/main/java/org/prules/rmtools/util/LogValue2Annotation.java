/*
 *  RapidMiner
 *
 *  Copyright (C) 2001-2013 by Rapid-I and the contributors
 *
 *  Complete list of developers available at our web site:
 *
 *       http://rapid-i.com
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see http://www.gnu.org/licenses/.
 */

package org.prules.rmtools.util;

import java.util.List;

import com.rapidminer.operator.Annotations;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.Value;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeList;
import com.rapidminer.parameter.ParameterTypeStringCategory;
import com.rapidminer.parameter.ParameterTypeValue;
import com.rapidminer.parameter.UndefinedParameterError;


/**
 * @author Marius Helf
 *
 */
public class LogValue2Annotation extends Operator {

	private InputPort inputPort = getInputPorts().createPort("input", IOObject.class);
	private OutputPort outputPort = getOutputPorts().createPort("output");
	
	public static final String[] DUPLICATE_HANDLING_LIST = {"overwrite", "ignore", "error"};
	public static final int OVERWRITE_DUPLICATES = 0;
	public static final int IGNORE_DUPLICATES = 1;
	public static final int ERROR_ON_DUPLICATES = 2;
	
	public static final String PARAMETER_DUPLICATE_HANDLING = "duplicate_annotations";
	public static final String PARAMETER_ANNOTATIONS = "annotations";
	public static final String PARAMETER_NAME = "annotation_name";
	public static final String PARAMETER_VALUE = "annotation_value";
	
	
	/**
	 * @param description
	 */
	public LogValue2Annotation(OperatorDescription description) {
		super(description);
		getTransformer().addPassThroughRule(inputPort, outputPort);
	}

        private String fetchValue(ParameterTypeValue.OperatorValueSelection selection) throws UndefinedParameterError {
		Operator operator = lookupOperator(selection.getOperator());
		if (operator != null) {
			if (selection.isValue()) {
				Value value = operator.getValue(selection.getValueName());
				if (value == null) {
					getLogger().warning("No such value in '" + selection + "'");
					return "NaN";
				}
				if (value.isNominal()) {
					Object actualValue = value.getValue();
					if (actualValue != null) {
						return value.getValue().toString();						
					} else {
						return "NaN";
					}
				} else {
					return value.getValue().toString();
				}

			} else {
				ParameterType parameterType = operator.getParameterType(selection.getParameterName());
				if (parameterType == null) {
					logWarning("No such parameter in '" + selection + "'");
					return "NaN";
				} else {
					if (parameterType.isNumerical()) { // numerical
						try {
							return operator.getParameter(selection.getParameterName()).toString();
						} catch (NumberFormatException e) {
							logWarning("Cannot parse parameter value of '" + selection + "'");
						}
					} else { // nominal
						return parameterType.toString(operator.getParameter(selection.getParameterName()));						
					}
				}
			}
		} else {
			logWarning("Unknown operator '" + selection.getOperator() + "' in '" + selection + "'");
		}
		return "NaN";
	}             
	
	@Override
	public void doWork() throws OperatorException {
		IOObject data = inputPort.getData(IOObject.class);
		Annotations annotations = data.getAnnotations();
		
		
		// get index of action for duplicate annotations
		String duplicateActionString = getParameterAsString(PARAMETER_DUPLICATE_HANDLING);
		int duplicateAction = ERROR_ON_DUPLICATES;
		for (int i = 0; i < DUPLICATE_HANDLING_LIST.length; ++i) {
			if (DUPLICATE_HANDLING_LIST[i].equals(duplicateActionString)) {
				duplicateAction = i;
				break;
			}
		}
		

		// just set annotations without any checks
		List<String[]> parameterList = getParameterList(PARAMETER_ANNOTATIONS);
		for (String[] nameValuePair : parameterList) {
			String key = nameValuePair[0];			
                        ParameterTypeValue.OperatorValueSelection parameterValue = ParameterTypeValue.transformString2OperatorValueSelection(nameValuePair[1]);
                        String value = fetchValue(parameterValue) ;
			if (value == null || "".equals(value)) {
				annotations.removeAnnotation(key);
			} else if (annotations.containsKey(key)) {
				switch (duplicateAction) {
					case OVERWRITE_DUPLICATES:
						// overwrite annotations
						annotations.setAnnotation(key, value);
						break;
					case IGNORE_DUPLICATES:
						// do nothing
						break;
					case ERROR_ON_DUPLICATES:
						// throw user error
						throw new UserError(this, "annotate.duplicate_annotation", key);
				}
			} else {
				annotations.setAnnotation(key, value);
			}
		}
		outputPort.deliver(data);
	}
	
	
	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		
		types.add(new ParameterTypeList(PARAMETER_ANNOTATIONS, "Defines the pairs of annotation names and annotation values. Click the button, select or type an annotation name into the left input field and enter its value into the right field. You can specify an arbitrary amount of annotations here. Please note that it is not possible to create empty annotations.",
				new ParameterTypeStringCategory(PARAMETER_NAME, "The name of the annotation", Annotations.ALL_KEYS_IOOBJECT, Annotations.ALL_KEYS_IOOBJECT[0]),
				new ParameterTypeValue(PARAMETER_VALUE, "operator.OPERATORNAME.[value|parameter].VALUE_NAME"),
				false)
				);
		
		types.add(new ParameterTypeStringCategory(PARAMETER_DUPLICATE_HANDLING, "Indicates what should happen if duplicate annotation names are specified.", DUPLICATE_HANDLING_LIST, DUPLICATE_HANDLING_LIST[OVERWRITE_DUPLICATES], false));                                
		
		return types;
	}
}
