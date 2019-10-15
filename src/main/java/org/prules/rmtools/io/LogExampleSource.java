/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.prules.rmtools.io;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.example.table.DoubleArrayDataRow;
import com.rapidminer.example.table.MemoryExampleTable;
import com.rapidminer.example.table.PolynominalMapping;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.io.AbstractReader;
import com.rapidminer.operator.nio.file.FileInputPortHandler;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.Port;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.PortProvider;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.rapidminer.tools.Ontology;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import static org.prules.rmtools.io.LogExampleSource.LogValue.UNKNOWN_TYPE;

/**
 *
 * @author Marcin
 */
public class LogExampleSource extends AbstractExampleSource {

    public static final String PARAMETER_LOG_FILE = "";
    private final InputPort fileInputPort = getInputPorts().createPort("file");
    private final FileInputPortHandler filePortHandler = new FileInputPortHandler(this, fileInputPort, PARAMETER_LOG_FILE);

    static {
        AbstractReader.registerReaderDescription(new AbstractReader.ReaderDescription("log", KeelExampleSource.class, PARAMETER_LOG_FILE));
    }
    List<List<LogValue>> logValues = new ArrayList<>(); //Data avaliable in the log file. Log values are converted into LogValue class which contain ifnormation of type of value and its value.
    List<Integer> logAttributeTypes = new ArrayList<>(); //Type of attributes Numeric vs String 
    List<LogValue> logHeader; //attribute names contained in the log file

    public LogExampleSource(OperatorDescription description) {
        super(description);        
    }
    
    @Override
    public boolean supportsEncoding(){
        return false;
    }

    @Override
    public ExampleSet createExampleSet() throws OperatorException { 
        logValues.clear();
        logAttributeTypes.clear();        
        File logFile = this.filePortHandler.getSelectedFile();
        //File logFile = getParameterAsFile(PARAMETER_LOG_FILE);
        if (logFile.isFile()) {
            try (Scanner sc = new Scanner(logFile)) {
                int lineId = 0;
                //The loop below is responsible for loading entry log file into logValues variable.
                //Afterwards it checks type of attributes
                while (sc.hasNextLine()) {
                    switch (lineId) {
                        case 0: //First row which is a comment
                            sc.nextLine();
                            break;
                        case 1: //Second row with column names
                            logHeader = processLine(sc.nextLine().substring(2),false);
                            break;
                        default: //remining rows
                            logValues.add(processLine(sc.nextLine(),true));
                            break;
                    }
                    lineId++;
                }
                return convertLogValues();
            } catch (FileNotFoundException ex) {
                Logger.getLogger(LogExampleSource.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return null;
    }

    /**
     * Process single line of log file
     *
     * @param line - line of log file to be processed
     * @param processAttributeTypes - if true then it checks attribute types. If false then for that line it is not checked. By default it should be checked, but for the row with attribute names it can't be enebled
     * @return
     */
    protected List<LogValue> processLine(String line, boolean processAttributeTypes) {
        List<LogValue> row = new ArrayList<>();
        try (Scanner sc = new Scanner(line)) {
            sc.useDelimiter("\\t"); //Skeep each TAB to next column
            int i = 0;
            while (sc.hasNext()) {
                //logAttributeTypes is a variable which stores attribute types and number of attributes. So first we check if we create new attribute the number of attributes is sufficient
                if (logAttributeTypes.size() <= i) { //If new column then initialize it to Numeric
                    logAttributeTypes.add(Ontology.NUMERICAL); 
                }                 
                if (sc.hasNextDouble()) { //Check if the value is numeric
                    row.add(new LogValue(Ontology.NUMERICAL, sc.nextDouble()));
                } else { //If not numeric then read it as String and set attribute type to NOMINAL (by default attributes are numeric)
                    String strVal = sc.next();
                    if ("null".equals(strVal)){
                        row.add(new LogValue(UNKNOWN_TYPE, strVal));                    
                    } else {
                        row.add(new LogValue(Ontology.NOMINAL, strVal));                    
                        if (processAttributeTypes){
                            logAttributeTypes.set(i, Ontology.NOMINAL);
                        }
                    }
                }
                i++;
            }
        }
        return row;
    }

    /**
     * This is internal method called after loading log file, which converts log values into ExampleSet
     * @return 
     */
    protected ExampleSet convertLogValues() {
        List<Attribute> attributeList = new LinkedList<Attribute>();
        //Okre�lamy dla ka�dej kolumny jej nazw� i typ danych
        Iterator<LogValue> headerIterator = logHeader.iterator();
        Iterator<Integer> attributeTypeIterator = logAttributeTypes.iterator();
        List<Map<String,Integer>> nominalAttrobutesValuesList = new ArrayList<>(); //List of maps of nominal values for nominal attributes. Each map contains pair of nominal value and corresponding double value for each nominal attributes. Number of nominal attributes is equal to the size of the list.
        if (logHeader.size() != logAttributeTypes.size()){
            this.logWarning("header of log don't match number of attributes");
        }
        while (headerIterator.hasNext() && attributeTypeIterator.hasNext()) {
            int type = attributeTypeIterator.next();
            attributeList.add(AttributeFactory.createAttribute(headerIterator.next().getValueAsString(), type));
            if (type == Ontology.NOMINAL){
                nominalAttrobutesValuesList.add(new HashMap<String,Integer>());
            }
        }        
        MemoryExampleTable table = new MemoryExampleTable(attributeList);
        Attribute[]  attributes = table.getAttributes();
        //int iter = -1;
        for (List<LogValue> row : logValues) {
            double[] data = new double[attributes.length];
            int attributeId = 0;
            int nominalAttributeId = 0;            
            for (LogValue val : row) {
                switch (logAttributeTypes.get(attributeId)) {
                    case Ontology.NUMERICAL:
                        data[attributeId] = val.getValueAsDouble();
                        break;                    
                    default:
                        //iter++;
                        //System.out.println("Iteration=" + iter + "  NomId="+nominalAttributeId);
                        Map<String,Integer> map = nominalAttrobutesValuesList.get(nominalAttributeId);
                        String curStringValue = val.getValueAsString();
                        if (map.containsKey(curStringValue)){
                            data[attributeId] = map.get(curStringValue);
                        } else {
                            int curNumericValue = map.size();
                            map.put(curStringValue,curNumericValue);
                            data[attributeId] = curNumericValue;
                        }
                        nominalAttributeId ++;
                        break;
                }
                attributeId++;
            }
            table.addDataRow(new DoubleArrayDataRow(data));
        }
        int stringAttributeId = 0;
        for (Attribute a : attributes){
            //if (a.getValueType()==Ontology.NOMINAL){                
            if(a.isNominal()){
                Map<String,Integer> map = nominalAttrobutesValuesList.get(stringAttributeId);
                Map<Integer,String> nominalMap = new HashMap<>();                
                for (Entry<String,Integer> keyValue : map.entrySet()){
                    nominalMap.put(keyValue.getValue(),keyValue.getKey());
                }
                a.setMapping(new PolynominalMapping(nominalMap));
                stringAttributeId++;                
            }
        }        
        return table.createExampleSet();
    }

    /**
     * Creates configuration parameters - here the name of the log file
     * @return 
     */
     @Override
    public List<ParameterType> getParameterTypes() {
        List<ParameterType> types = new LinkedList<ParameterType>();
        types.add(FileInputPortHandler.makeFileParameterType(this, PARAMETER_LOG_FILE, "Name of the Keel data file to read the data from.", "log", new PortProvider() {
            @Override
            public Port getPort() {
                return fileInputPort;
            }
        }));
        types.addAll(super.getParameterTypes());
        //types.addAll(StrictDecimalFormat.getParameterTypes(this));
        return types;
    }
    
    /**
     * Internal class used to store log values. Ech instance is allowed to store value of log entry and type of log entry.
     * To types are recognized Numeric and String
     */
    static class LogValue {
        public static final int UNKNOWN_TYPE = -1;
        int valueType = -1; //Type of value storeg by log. We use here Ontology.NUMERICAL / Ontology.NOMINAL
        Object value; //Value represented as Object, it is Double or NOMINAL
        LogValue(int valueType, Object value) {
            this.valueType = valueType;
            this.value = value;
        }

        /**
         * Set log value as double (automatically sets value type to NUMERICAL)
         * @param value 
         */
        public void setValueAsDouble(double value) {
            valueType = Ontology.NUMERICAL;
            this.value = value;
        }

        /**
         * Set log value as string (automatically sets value type to String)
         * @param value 
         */
        public void setValueAsString(String value) {
            valueType = Ontology.NOMINAL;
            this.value = value;
        }

        /**
         * Returns true if stored value is double, false otherwise
         * @return 
         */
        public boolean isNumeric() {
            return valueType == Ontology.NUMERICAL;
        }

        public boolean isString() {
            return valueType == Ontology.NOMINAL;
        }

        /**
         * Returns value as double, but only if it is numeric type, otherwise it throws an exception NumberFormatException
         * @return 
         */
        public double getValueAsDouble() {
            if (valueType == Ontology.NUMERICAL) {
                return (Double) value;
            } else {
                return Double.NaN;
            }
        }

        /**
         * Returns value as string
         * @return 
         */
        public String getValueAsString() {
            return value.toString();
        }
    }

}
