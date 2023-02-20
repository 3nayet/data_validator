package com.utility;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.file.FileSystems;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class SystemConfiguration {

    private static final char SECTION_BEGIN = '[';
    private static final char SECTION_END = ']';
    private static final char COMMENT = '#';
    private static final char OPERATOR = '=';
        private static final String CONFIGURATION_FILE_NAME = "SystemConfiguration.ini";

    private static SystemConfiguration instance;

    private HashMap<String, HashMap<String, String>> configuration;
    private String currentContext = "";

    private SystemConfiguration () {
        configuration = new HashMap<>();
        loadSystemConfig();

    }

    private SystemConfiguration (String pContext) {
        currentContext = pContext;
        configuration = new HashMap<>();
        loadSystemConfig();

    }

    public static synchronized SystemConfiguration getDefault () {
        if (instance == null) {
            instance = new SystemConfiguration("DataValidator");
        }
        return instance;
    }

    private void loadSystemConfig () {
        try {
            loadConfiguration();
        } catch (IOException e) {
            //handle error
        }
    }

    private void loadConfiguration() throws IOException {

        String configurationFile = getCurrentWorkingDirectory() + getDefaultFileSeperator() + CONFIGURATION_FILE_NAME;
        try (InputStream configStream = new FileInputStream(configurationFile)) {
            readProperties(configStream);
        } catch (Exception e) {
            //handle error
        }

    }

    private void readProperties (InputStream pIs) throws IOException {
        BufferedReader source = new BufferedReader(new InputStreamReader(pIs));
        String currLine;
        String currentSection = null;
        while ((currLine = source.readLine()) != null) {
            if (currLine.trim().length() > 0) {
                switch (currLine.charAt(0)) {
                    case SECTION_BEGIN:
                        String sectionName = currLine.substring(1, currLine.indexOf(SECTION_END)).trim();
                        // create a new section in the configuration
                        HashMap<String, String> newSection = new HashMap<>();
                        if (!configuration.containsKey(sectionName))
                            configuration.put(sectionName, newSection);
                        // set the current section
                        currentSection = sectionName;
                        break;
                    case COMMENT:
                        // do nothing
                        break;
                    default:
                        // check if the line belongs to a section and has the correct syntax name=value
                        if (currLine.indexOf(OPERATOR) != -1 && currLine.length() > 0) {
                            String key = currLine.substring(0, currLine.indexOf(OPERATOR)).trim();
                            String value = currLine.substring(currLine.indexOf(OPERATOR) + 1, currLine.length()).trim();
                            // add the parameter to the current section
                            HashMap<String, String> aSection = configuration.get(currentSection);
                            aSection.put(key, value);
                            configuration.put(currentSection, aSection);
                        }
                }
            }
        }
        source.close();
    }

    public String getParameter(String sectionName, String parameter) {

        if (!configuration.containsKey(sectionName)) {
            return null;
        }

        HashMap<String, String> section = configuration.get(sectionName);
        if (!section.containsKey(parameter)) {
            return null;
        }

        return section.get(parameter).equalsIgnoreCase("null") ? "null" : section.get(parameter);
    }

    public String getParameter(String sectionName, String parameter, String def) {
        String out = getParameter(sectionName,parameter);
        if ( ValidatorUtility.isNullOrEmpty(out))
            return def;
        return out;
    }

    public int getIntParameter(String sectionName, String parameter,int defaultValue) {
        try {
            String val = getParameter(sectionName,parameter);
            if (!ValidatorUtility.isNullOrEmpty(val))
                return Integer.parseInt(val);
            return defaultValue;
        } catch ( Exception e ) {
            return defaultValue;
        }
    }

    public Set<String> getSectionParameters(String sectionName) {
        if (!configuration.containsKey(sectionName)) {
            return Collections.emptySet();
        }

        HashMap<String, String> section = configuration.get(sectionName);
        return section.keySet();
    }

    public boolean hasSection (String section) {
        return configuration.containsKey(section);
    }

    private static String getCurrentWorkingDirectory() {
        String workdir =  FileSystems.getDefault()
                .getPath("")
                .toAbsolutePath()
                .toString();
        return workdir;
    }

    private static String getDefaultFileSeperator() {
        return System.getProperty("file.separator");
    }

    public static List<File> getListOfInputFiles(){
        try{
            File folder = new File(getCurrentWorkingDirectory());
            File[] listOfFiles = folder.listFiles();
            return Arrays.stream(listOfFiles).filter(f -> f.isFile() && (f.getName().endsWith(".xls")
                    ||f.getName().endsWith(".XLS")||f.getName().endsWith(".XLSX")
                    ||f.getName().endsWith(".xlsx")) ).toList();
        }catch (Exception e){
            return Collections.emptyList();
        }
    }

    public static BigDecimal validateValue (BigDecimal value,  BigDecimal reference, BigDecimal minRange,
                                            BigDecimal maxRange, BigDecimal conversionRate){
        if(value != null &&(value.compareTo(BigDecimal.ZERO) ==0 ||
                (value.compareTo(reference.multiply(maxRange))<0 && value.compareTo(reference.multiply(minRange))>0))){
            return value;
        }else{
            return reference.multiply(conversionRate);
        }
    }

    public static boolean isNumeric(String strNum) {
        if (strNum == null) {
            return false;
        }
        try {
            double d = Double.parseDouble(strNum);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }

}
