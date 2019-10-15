/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.prules.rmtools.process.loop;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.OperatorChain;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;

/**
 *
 * @author Marcin
 */
public class LoopCombineDatasets extends OperatorChain{
    
    /**
     * Input port which delivers training ExampleSet
     */
    protected final InputPort inputPort = getInputPorts().createPort("Input");
    /**
     * Output port which delivers ExampleSet with constructed prototypes
     */
    protected final OutputPort outputPort = getOutputPorts().createPort("ExampleSet");
    /**
     * Output port which delivers the training ExampleSet
     */    

    public LoopCombineDatasets(OperatorDescription description) {
        super(description,"Loop model");
    }
    
    @Override
    public void doWork() throws OperatorException {
        ExampleSet trainingSet = inputPort.getData(ExampleSet.class);

        //ExampleSet outputSet = processExamples(trainingSet);

        outputPort.deliver(trainingSet);        
    }
    
    
    
}
