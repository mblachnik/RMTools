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

import com.rapidminer.MacroHandler;
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
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.ParameterTypeStringCategory;
import com.rapidminer.parameter.ParameterTypeValue;
import com.rapidminer.parameter.UndefinedParameterError;

/**
 * @author Marius Helf
 *
 */
public class LogValue2Macro extends Operator {

    private InputPort inputPort = getInputPorts().createPort("input", IOObject.class);
    private OutputPort outputPort = getOutputPorts().createPort("output");

    public static final String PARAMETER_MACROS = "Macros";
    public static final String PARAMETER_NAME = "macro_name";
    public static final String PARAMETER_VALUE = "macro_value";

    /**
     * @param description
     */
    public LogValue2Macro(OperatorDescription description) {
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
                } else if (parameterType.isNumerical()) { // numerical
                    try {
                        return operator.getParameter(selection.getParameterName()).toString();
                    } catch (NumberFormatException e) {
                        logWarning("Cannot parse parameter value of '" + selection + "'");
                    }
                } else { // nominal
                    return parameterType.toString(operator.getParameter(selection.getParameterName()));
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
        MacroHandler macroHandler = this.getProcess().getMacroHandler();

        // just set annotations without any checks
        List<String[]> parameterList = getParameterList(PARAMETER_MACROS);
        for (String[] nameValuePair : parameterList) {
            String key = nameValuePair[0];
            ParameterTypeValue.OperatorValueSelection parameterValue = ParameterTypeValue.transformString2OperatorValueSelection(nameValuePair[1]);
            String value = fetchValue(parameterValue);
            if (value == null || "".equals(value)) {
                macroHandler.removeMacro(key);
            } else {
                macroHandler.addMacro(key, value);
            }
        }
        outputPort.deliver(data);
    }

    @Override
    public List<ParameterType> getParameterTypes() {
        List<ParameterType> types = super.getParameterTypes();

        types.add(new ParameterTypeList(PARAMETER_MACROS, "The list of macros defined by the user.",
                new ParameterTypeString("macro_name", "The macro name."),
                new ParameterTypeValue(PARAMETER_VALUE, "operator.OPERATORNAME.[value|parameter].VALUE_NAME"),
                false));
        return types;
    }
}
