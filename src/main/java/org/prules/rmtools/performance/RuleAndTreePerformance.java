/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.prules.rmtools.performance;

import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ValueDouble;
import com.rapidminer.operator.learner.PredictionModel;
import com.rapidminer.operator.learner.rules.Rule;
import com.rapidminer.operator.learner.rules.RuleModel;
import com.rapidminer.operator.learner.tree.Edge;
import com.rapidminer.operator.learner.tree.Tree;
import com.rapidminer.operator.learner.tree.TreeModel;
import com.rapidminer.operator.learner.weka.WekaClassifier;
import com.rapidminer.operator.performance.EstimatedPerformance;
import com.rapidminer.operator.performance.MDLCriterion;
import com.rapidminer.operator.performance.PerformanceVector;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeCategory;
import java.util.Iterator;
import java.util.List;
import weka.classifiers.Classifier;
import weka.classifiers.rules.DecisionTable;
import weka.classifiers.rules.JRip;
import weka.classifiers.rules.PART;
import weka.classifiers.trees.J48;
import weka.classifiers.trees.LMT;
import weka.classifiers.trees.M5P;
import weka.classifiers.trees.REPTree;

/**
 *
 * @author Marcin
 */
public class RuleAndTreePerformance extends Operator {

    /**
     * This class estimate performance of any rule or tree learner. If the input 
     * object is a tree the output is number of nodes in the tree 
     * The parameter name for &quot;Indicates if the fitness should for maximal
     * or minimal number of features.&quot;
     */
    public static final String PARAMETER_OPTIMIZATION_DIRECTION = "optimization_direction";

    private double treeSize = Double.NaN; //number of nodes in the tree
    private double treeLeaves = Double.NaN; //Number of leaves in the tree        
    private double rules = Double.NaN; //Number of rules       

    private InputPort classifierInput = getInputPorts().createPort("PredictionModel", PredictionModel.class);
    private InputPort performanceInput = getInputPorts().createPort("performance vector");

    private OutputPort classifierOutput = getOutputPorts().createPort("PredictionModel");
    private OutputPort performanceOutput = getOutputPorts().createPort("performance vector");

    public RuleAndTreePerformance(OperatorDescription description) {
        super(description);

        getTransformer().addGenerationRule(performanceOutput, PerformanceVector.class);
        getTransformer().addPassThroughRule(classifierInput, classifierOutput);

        addValue(new ValueDouble("tree_size", "Number of nodes in the tree") {
            @Override
            public double getDoubleValue() {
                return treeSize;
            }
        });
        addValue(new ValueDouble("tree_leaves", "Number of leaves in the tree") {
            @Override
            public double getDoubleValue() {
                return treeLeaves;
            }
        });
        addValue(new ValueDouble("rules", "Number of rules") {
            @Override
            public double getDoubleValue() {
                return rules;
            }
        });
    }

    @Override
    public void doWork() throws OperatorException {
        PredictionModel classifier = classifierInput.getData(PredictionModel.class);

        PerformanceVector inputPerformance = performanceInput.getDataOrNull(PerformanceVector.class);

        PerformanceVector performance = getPerformance(classifier, inputPerformance);

        classifierOutput.deliver(classifier);
        performanceOutput.deliver(performance);
    }

    /**
     * Creates a new performance vector if the given one is null. Adds a new
     * estimated criterion. If the criterion was already part of the performance
     * vector before it will be overwritten.
     */
    private PerformanceVector getPerformance(PredictionModel classifier, PerformanceVector performanceCriteria) throws OperatorException {
        if (performanceCriteria == null) {
            performanceCriteria = new PerformanceVector();
        }
        if (classifier instanceof WekaClassifier) {
            Classifier wekaClassifier = ((WekaClassifier) classifier).getClassifier();
            if (wekaClassifier instanceof J48) {
                J48 tree = (J48) wekaClassifier;
                treeSize = tree.measureTreeSize();
                treeLeaves = tree.measureNumLeaves();
                rules = tree.measureNumRules();
            } else if (wekaClassifier instanceof LMT) {
                LMT tree = (LMT) wekaClassifier;
                treeSize = tree.measureTreeSize();
                treeLeaves = tree.measureNumLeaves();
            } else if (wekaClassifier instanceof REPTree) {
                REPTree tree = (REPTree) wekaClassifier;
                treeSize = tree.numNodes();
            } else if (wekaClassifier instanceof M5P) {
                M5P tree = (M5P) wekaClassifier;
                rules = tree.measureNumRules();
            } else if (wekaClassifier instanceof JRip){
                JRip ruleClassifier = (JRip) wekaClassifier;
                rules = ruleClassifier.getMeasure("measureNumRules");
            } else if (wekaClassifier instanceof DecisionTable){
                DecisionTable ruleClassifier = (DecisionTable) wekaClassifier;
                rules = ruleClassifier.measureNumRules();
            } else if (wekaClassifier instanceof PART){
                PART ruleClassifier = (PART) wekaClassifier;
                rules = ruleClassifier.measureNumRules();
            } else {
                throw new UserError(this, "Incorect input");
            }
        } else if (classifier instanceof TreeModel) {
            Tree tree = ((TreeModel)classifier).getRoot();
            if (!tree.isLeaf()) {
                treeLeaves = 0;            
                treeSize = 1;            
                processRMTree(tree);
            } else {
                treeLeaves = 1;            
                treeSize = 0;            
            }                        
            rules = treeLeaves;
        } else if (classifier instanceof RuleModel) {
            List<Rule>  ruleList = ((RuleModel)classifier).getRules();
            rules = ruleList.size();            
        } else {
            throw new UserError(this, "Incorect input");
        }
        EstimatedPerformance treeSizeCriterion = new EstimatedPerformance("treeSize", treeSize, 1, getParameterAsInt(PARAMETER_OPTIMIZATION_DIRECTION) == MDLCriterion.MINIMIZATION);
        EstimatedPerformance treeLeavesCriterion = new EstimatedPerformance("treeLeaves", treeLeaves, 1, getParameterAsInt(PARAMETER_OPTIMIZATION_DIRECTION) == MDLCriterion.MINIMIZATION);
        EstimatedPerformance treeRulesCriterion = new EstimatedPerformance("Rules", rules, 1, getParameterAsInt(PARAMETER_OPTIMIZATION_DIRECTION) == MDLCriterion.MINIMIZATION);
        performanceCriteria.addCriterion(treeSizeCriterion);
        performanceCriteria.addCriterion(treeLeavesCriterion);
        performanceCriteria.addCriterion(treeRulesCriterion);
        return performanceCriteria;
    }

    private void processRMTree(Tree tree) {
        Iterator<Edge> childIterator = tree.childIterator();
        while (childIterator.hasNext()) {
            Edge edge = childIterator.next();
            Tree child = edge.getChild();
            if (child.isLeaf()) {
                treeLeaves++;
            } else {
                treeSize++;
                processRMTree(child);
            }
        }
    }

    @Override
    public List<ParameterType> getParameterTypes() {
        List<ParameterType> types = super.getParameterTypes();
        types.add(new ParameterTypeCategory(PARAMETER_OPTIMIZATION_DIRECTION, "Indicates if the fitness should be maximal for the maximal or the minimal number of the performance.", MDLCriterion.DIRECTIONS, MDLCriterion.MINIMIZATION));
        return types;
    }
}
