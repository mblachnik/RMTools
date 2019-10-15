/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.prules.rmtools.util;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Marcin
 */
public enum OperationTypes {
    MAX(new Operation() {
        @Override
        public int[] operation(ExampleSet es, Attribute attribute, boolean onlyFirst) {
            List<Integer> bestList = new ArrayList<>();
            double bestValue = Double.NEGATIVE_INFINITY;
            int i = 0;
            for (Example ex : es) {
                double curValue = ex.getValue(attribute);
                if (curValue > bestValue) {
                    bestList.clear();
                    bestValue = curValue;
                    bestList.add(i);
                } else if (curValue == bestValue && !onlyFirst) {
                    bestList.add(i);
                }
                i++;
            }
            //Convert selected values from list to array
            int out[] = new int[bestList.size()];
            i = 0;
            for (int val : bestList) {
                out[i] = val;
                i++;
            }
            return out;
        }
    }),
    MIN(new Operation() {
        @Override
        public int[] operation(ExampleSet es, Attribute attribute, boolean onlyFirst) {
            List<Integer> bestList = new ArrayList<>();
            double bestValue = Double.POSITIVE_INFINITY;
            int i = 0;
            for (Example ex : es) {
                double curValue = ex.getValue(attribute);
                if (curValue < bestValue) {
                    bestList.clear();
                    bestValue = curValue;
                    bestList.add(i);
                } else if (curValue == bestValue && !onlyFirst) {
                    bestList.add(i);
                }
                i++;
            }
            //Convert selected values from list to array            
            int out[] = new int[bestList.size()];
            i = 0;
            for (int val : bestList) {
                out[i] = val;
                i++;
            }
            return out;
        }
    }),
    MEDIAN(new Operation() {
        @Override
        public int[] operation(ExampleSet es, Attribute attribute, boolean onlyFirst) {
            double[] values = new double[es.size()];
            int i = 0;
            //Read values of given attribute
            for (Example ex : es) {
                values[i] = ex.getValue(attribute);
                i++;
            }
            //Sort values
            Arrays.sort(values);
            //Find median index and median value
            int medianIdx = values.length % 2 == 0 ? values.length / 2 : values.length / 2 + 1;
            double median = values[medianIdx];
            //Find all median values (it can be more then one value)
            List<Integer> bestList = new ArrayList<>();
            i = 0;
            for (Example ex : es) {
                if (ex.getValue(attribute) == median) {
                    bestList.add(i);
                }
                i++;
            }
            //Convert selected values from list to array
            int[] out;
            if (onlyFirst) {
                out = new int[]{bestList.get(0)};
            } else {
                out = new int[bestList.size()];
                i = 0;
                for (int val : bestList) {
                    out[i] = val;
                    i++;
                }
            }
            return out;
        }
    }),
    MODE(new Operation() {
        @Override
        public int[] operation(ExampleSet es, Attribute attribute, boolean onlyFirst) {
            Map<Double, List<Integer>> map = new HashMap<>();
            int i = 0;
            for (Example ex : es) {
                double curValue = ex.getValue(attribute);
                if (map.containsKey(curValue)) {
                    List<Integer> list = map.get(curValue);
                    list.add(i);
                    map.put(curValue, list);
                } else {
                    List<Integer> list = new ArrayList<>();
                    list.add(i);
                    map.put(curValue, list);
                }
                i++;
            }
            List<Integer> bestList = new ArrayList<>();
            for (List<Integer> li : map.values()) {
                if (li.size() > bestList.size()) {
                    bestList = li;
                }
            }

            int[] out;
            if (onlyFirst) {
                out = new int[]{bestList.get(0)};
            } else {
                out = new int[bestList.size()];
                i = 0;
                for (int val : bestList) {
                    out[i] = val;
                    i++;
                }
            }
            return out;
        }
    });

    private Operation operationImplementation;
    private static final String valueNames[];

    //Static code to extract operation names
    static {
        OperationTypes[] values = OperationTypes.values();
        valueNames = new String[values.length];
        int i = 0;
        for (OperationTypes val : values) {
            valueNames[i] = val.name();
            i++;
        }
    }

    /**
     * Enume constructor - requires implementation of given operation
     *
     * @param operationImplementation
     */
    OperationTypes(Operation operationImplementation) {
        this.operationImplementation = operationImplementation;
    }

    /**
     * Performs calculations to find matching examples
     *
     * @param es - input example set
     * @param attribute attribute to analyze
     * @param onlyFirst if true than keep only first matching value, otherwise
     * all values will be be keept
     * @return
     */
    public int[] getExampleIndexesMatchingCondition(ExampleSet es, Attribute attribute, boolean onlyFirst) {
        return operationImplementation.operation(es, attribute, onlyFirst);
    }

    /**
     * Performs calculations to find matching examples
     *
     * @param es - input example set
     * @param attributeName attribute name to analyze
     * @param onlyFirst if true than keep only first matching value, otherwise
     * all values will be be keept
     * @return
     */
    public int[] getExampleIndexesMatchingCondition(ExampleSet es, String attributeName, boolean onlyFirst) {
        return operationImplementation.operation(es, es.getAttributes().get(attributeName), onlyFirst);
    }

    /**
     * Returns array of strings representing names of enum values
     *
     * @return
     */
    public static String[] getValueNames() {
        return valueNames;
    }

    /**
     * Returns enum associated to given index (see getValueNames)
     *
     * @param index
     * @return
     */
    public static OperationTypes valueOf(int index) {
        return OperationTypes.valueOf(valueNames[index]);
    }

    interface Operation {

        int[] operation(ExampleSet es, Attribute attribute, boolean onlyFirst);
    };
}
