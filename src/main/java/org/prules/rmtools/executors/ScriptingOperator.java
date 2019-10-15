/**
 * Copyright (C) 2001-2015 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 * http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Affero General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see http://www.gnu.org/licenses/.
 */
package org.prules.rmtools.executors;

import com.rapidminer.operator.IOObject;
import groovy.lang.GroovyShell;
import groovy.lang.Script;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.InputPortExtender;
import com.rapidminer.operator.ports.OutputPortExtender;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeFile;
import com.rapidminer.tools.plugin.Plugin;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <p>
 * This operator can be used to execute arbitrary Groovy scripts. This basically
 * means that analysts can write their own operators directly within the process
 * by specifiying Java code and / or a Groovy script which will be interpreted
 * and executed during process runtime. For a complete reference of Groovy
 * scripting please refer to http://groovy.codehaus.org/.
 * </p>
 *
 * <p>
 * In addition to the usual scripting code elements from Groovy, the RapidMiner
 * scripting operator defines some special scripting elements:
 * </p>
 * <ul>
 * <li>If you use the standard <em>imports</em>, all important types like
 * Example, ExampleSet, Attribute, Operator etc. as well as the most important
 * Java types like collections etc. are automatically imported and can directly
 * be used within the script. Hence, there is no need for importing them in your
 * script. However, you can of course import any other class you want and use
 * this in your script.</li>
 * <li>The <em>current operator</em> (the scripting operator for which you
 * define the script) is referenced by <code>operator</code>.<br />
 * Example: <code>operator.log("text")</code></li>
 * <li>All <em>operator methods</em> like <code>log</code> (see above),
 * accessing the input or the complete process can directly be used by writing a
 * preceding <code>operator</code>.<br />
 * Example: <code>operator.getProcess()</code></li>
 * <li><em>Input of the operator</em> can be retrieved via the input method
 * getInput(Class) of the surrounding operator.<br />
 * Example:
 * <code>ExampleSet exampleSet = operator.getInput(ExampleSet.class)</code></li>
 * <li>You can <em>iterate over examples</em> with the following
 * construct:<br />
 * <code>for (Example example : exampleSet) { ... }</code></li>
 * <li>You can <em>retrieve example values</em> with the shortcut<br />
 * <code>String value = example[&quot;attribute_name&quot;];</code> or <br />
 * <code>double value = example[&quot;attribute_name&quot;];</code></li>
 * <li>You can <em>set example values</em> with
 * <code>example[&quot;attribute_name&quot;] = &quot;value&quot;;</code> or
 * <br />
 * <code>example[&quot;attribute_name&quot;] = 5.7;</code></li>
 * </ul>
 *
 * <p>
 * <em>Note:</em> Scripts written for this operator may access Java code.
 * Scripts may hence become incompatible in future releases of RapidMiner.
 * </p>
 *
 * @author Simon Fischer, Ingo Mierswa
 */
public class ScriptingOperator extends Operator {

    private final InputPortExtender inExtender = new InputPortExtender("input", getInputPorts());
    private final OutputPortExtender outExtender = new OutputPortExtender("output", getOutputPorts());

    public static final String PARAMETER_SCRIPT = "Script file name";

    public static final String PARAMETER_STANDARD_IMPORTS = "standard_imports";

    public ScriptingOperator(OperatorDescription description) {
        super(description);
        inExtender.start();
        outExtender.start();
    }

    @Override
    public void doWork() throws OperatorException {
        File scriptFile = getParameterAsFile(PARAMETER_SCRIPT);
        String script = "";
        if (!scriptFile.exists()) {

            if (getParameterAsBoolean(PARAMETER_STANDARD_IMPORTS)) {
                StringBuilder imports = new StringBuilder();
                imports.append("import com.rapidminer.example.*;\n");
                imports.append("import com.rapidminer.example.set.*;\n");
                imports.append("import com.rapidminer.example.table.*;\n");
                imports.append("import com.rapidminer.operator.*;\n");
                imports.append("import com.rapidminer.tools.Tools;\n");
                imports.append("import java.util.*;\n");

                script = imports.toString() + script;
            }
        } else {

        }
        try (Scanner sc = new Scanner(scriptFile)) {
            StringBuilder sb = new StringBuilder();
            sb.append(script);
            while (sc.hasNextLine()) {
                sb.append(sc.nextLine());
            }
            script = sb.toString();
        } catch (FileNotFoundException ex) {
            getLogger().log(Level.SEVERE, "File: {0} not found", scriptFile.getPath());
            throw new UserError(this, ex, 301);
        }
        Object result;
        try {
            GroovyShell shell = new GroovyShell(Plugin.getMajorClassLoader());
            // GroovyShell shell = new GroovyShell(ScriptingOperator.class.getClassLoader());
            List<IOObject> input = inExtender.getData(IOObject.class, false);
            shell.setVariable("input", input);
            shell.setVariable("operator", this);

            Script parsedScript = shell.parse(script);
            result = parsedScript.run();
        } catch (Throwable e) {
            throw new UserError(this, e, 945, "Groovy", e);
        }
        if (result instanceof Object[]) {
            outExtender.deliver(Arrays.asList((IOObject[]) result));
        } else if (result instanceof List) {
            List<IOObject> results = new LinkedList<IOObject>();
            for (Object single : (List) result) {
                if (single instanceof IOObject) {
                    results.add((IOObject) single);
                } else {
                    getLogger().log(Level.WARNING, "Unknown result type: {0}", single);
                }
            }
            outExtender.deliver(results);
        } else if (result != null) {
            if (result instanceof IOObject) {
                outExtender.deliver(Collections.singletonList((IOObject) result));
            } else {
                getLogger().log(Level.WARNING, "Unknown result: {0}: {1}", new Object[]{result.getClass(), result});
            }
        }
    }

    @Override
    public List<ParameterType> getParameterTypes() {
        List<ParameterType> types = super.getParameterTypes();
        ParameterType type = new ParameterTypeFile(PARAMETER_SCRIPT, PARAMETER_SCRIPT, "groovy", false);        
        type.setExpert(false);
        types.add(type);
        types.add(new ParameterTypeBoolean(PARAMETER_STANDARD_IMPORTS,
                "Indicates if standard imports for examples and attributes etc. should be automatically generated.", true));
        return types;
    }

    private String getHelp() {
        return "&lt;p&gt;This operator can be used to execute arbitrary Groovy\n"
                + "            scripts. This basically means that analysts can write their own\n"
                + "            operators directly within the process by specifiying Java code and /\n"
                + "            or a Groovy script which will be interpreted and executed during\n"
                + "            process runtime. For a complete reference of Groovy scripting please\n"
                + "            refer to http://groovy.codehaus.org/.&lt;/p&gt; &lt;p&gt;In addition\n"
                + "            to the usual scripting code elements from Groovy, the RapidMiner\n"
                + "            scripting operator defines some special scripting elements:&lt;/p&gt;\n"
                + "            &lt;ul&gt; &lt;li&gt;If you use the standard\n"
                + "            &lt;em&gt;imports&lt;/em&gt;, all important types like Example,\n"
                + "            ExampleSet, Attribute, Operator etc. as well as the most important\n"
                + "            Java types like collections etc. are automatically imported and can\n"
                + "            directly be used within the script. Hence, there is no need for\n"
                + "            importing them in your script. However, you can of course import any\n"
                + "            other class you want and use this in your script.&lt;/li&gt;\n"
                + "            &lt;li&gt;The &lt;em&gt;current operator&lt;/em&gt; (the scripting\n"
                + "            operator for which you define the script) is referenced by\n"
                + "            &lt;code&gt;operator&lt;/code&gt;.&lt;br /&gt; Example:\n"
                + "            &lt;code&gt;operator.log(\"text\")&lt;/code&gt;&lt;/li&gt;\n"
                + "            &lt;li&gt;All &lt;em&gt;operator methods&lt;/em&gt; like\n"
                + "            &lt;code&gt;log&lt;/code&gt; (see above), accessing the input or the\n"
                + "            complete process can directly be used by writing a preceding\n"
                + "            &lt;code&gt;operator&lt;/code&gt;.&lt;br /&gt; Example:\n"
                + "            &lt;code&gt;operator.getProcess()&lt;/code&gt;&lt;/li&gt;\n"
                + "            &lt;li&gt;&lt;em&gt;Input of the operator&lt;/em&gt; can be retrieved\n"
                + "            via the input method getInput(Class) of the surrounding\n"
                + "            operator.&lt;br /&gt; Example: &lt;code&gt;ExampleSet exampleSet =\n"
                + "            operator.getInput(ExampleSet.class)&lt;/code&gt;&lt;/li&gt;\n"
                + "            &lt;li&gt;You can &lt;em&gt;iterate over examples&lt;/em&gt; with the\n"
                + "            following construct:&lt;br /&gt; &lt;code&gt;for (Example example :\n"
                + "            exampleSet) { ... }&lt;/code&gt;&lt;/li&gt; &lt;li&gt;You can\n"
                + "            &lt;em&gt;retrieve example values&lt;/em&gt; with the shortcut&lt;br\n"
                + "            /&gt; &lt;code&gt;String value =\n"
                + "            example[&amp;quot;attribute_name&amp;quot;];&lt;/code&gt; or &lt;br\n"
                + "            /&gt; &lt;code&gt;double value =\n"
                + "            example[&amp;quot;attribute_name&amp;quot;];&lt;/code&gt;&lt;/li&gt;\n"
                + "            &lt;li&gt;You can &lt;em&gt;set example values&lt;/em&gt; with\n"
                + "            &lt;code&gt;example[&amp;quot;attribute_name&amp;quot;] =\n"
                + "            &amp;quot;value&amp;quot;;&lt;/code&gt; or &lt;br /&gt;\n"
                + "            &lt;code&gt;example[&amp;quot;attribute_name&amp;quot;] =\n"
                + "            5.7;&lt;/code&gt;&lt;/li&gt; &lt;/ul&gt;\n"
                + "            &lt;p&gt;&lt;em&gt;Note:&lt;/em&gt; Scripts written for this operator\n"
                + "            may access Java code. Scripts may hence become incompatible in future\n"
                + "            releases of RapidMiner.&lt;/p&gt;";
    }
}
