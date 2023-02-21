package com.datavalidator;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.data.ValidationRes;
import com.utility.SystemConfiguration;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

public class DataValidator {
    public static void main (String args[]) throws IOException {
        SystemConfiguration sysConf = SystemConfiguration.getDefault();
        final BigDecimal rangeMin = new BigDecimal(sysConf.getParameter("rates",
                "range_min" , "0.99967"));
        final BigDecimal rangeMax = new BigDecimal(sysConf.getParameter("rates",
                "range_max" , "1.0067"));
        final BigDecimal conversionRate = new BigDecimal(sysConf.getParameter("rates",
                "conversion_rate" , "1.0063"));
        final BigDecimal threshold = new BigDecimal(sysConf.getParameter("rates",
                "threshold" , "0.2"));
        SystemConfiguration.getListOfInputFiles().forEach(
                file -> {
                    try {
                        Long invalidCounter = 0L, zeroCounter = 0L;
                        FileInputStream fis = new FileInputStream(file);
                        Workbook workbook = WorkbookFactory.create(fis);
                        Sheet sheet = workbook.getSheetAt(0);
                        Row firstRow = sheet.getRow(0);
                        FormulaEvaluator formulaEvaluator = workbook.getCreationHelper().createFormulaEvaluator();
                        List<BigDecimal> lastValid = new ArrayList<>();

                        for (int k=0; k<firstRow.getLastCellNum(); k++)
                        {
                            switch (formulaEvaluator.evaluateInCell(firstRow.getCell(k)).getCellType()) {
                                case Cell.CELL_TYPE_NUMERIC:
                                    lastValid.add(BigDecimal.valueOf(firstRow.getCell(k).getNumericCellValue()));
                                    break;
                                case Cell.CELL_TYPE_STRING:
                                    lastValid.add(new BigDecimal(firstRow.getCell(k).getStringCellValue()));
                                    break;
                                default:
                                    break;
                            }
                        }
                        for (int i=1; i<=sheet.getLastRowNum(); i++){
                            Row row = sheet.getRow(i);
                            for (int j=0; j<row.getLastCellNum(); j++)
                            {
                                Cell cell = row.getCell(j);
                                ValidationRes valRes = null;
                                switch (formulaEvaluator.evaluateInCell(cell).getCellType()) {
                                    case Cell.CELL_TYPE_BLANK:
                                    case Cell.CELL_TYPE_ERROR:
                                    case Cell.CELL_TYPE_BOOLEAN:
                                    case Cell.CELL_TYPE_FORMULA:
                                        valRes = SystemConfiguration.validateValue(null, lastValid.get(j),
                                                rangeMin,rangeMax,conversionRate);
                                        cell.setCellValue(valRes.getValue().doubleValue());
                                        lastValid.set(j, valRes.getValue());
                                        break;
                                    case Cell.CELL_TYPE_STRING:
                                        if(SystemConfiguration.isNumeric(cell.getStringCellValue())){
                                            valRes = SystemConfiguration.validateValue(
                                                    new BigDecimal(cell.getStringCellValue()), lastValid.get(j),
                                                    rangeMin,rangeMax,conversionRate);
                                            cell.setCellValue(valRes.getValue().doubleValue());
                                            if(valRes.getValue().compareTo(BigDecimal.ZERO) !=0){
                                                lastValid.set(j, valRes.getValue());
                                            }
                                        }else{
                                            valRes = SystemConfiguration.validateValue(null, lastValid.get(j),
                                                    rangeMin,rangeMax,conversionRate);
                                            cell.setCellValue(valRes.getValue().doubleValue());
                                            lastValid.set(j, valRes.getValue());
                                        }
                                        break;
                                    case Cell.CELL_TYPE_NUMERIC:
                                         valRes = SystemConfiguration.validateValue(
                                                 BigDecimal.valueOf(cell.getNumericCellValue()), lastValid.get(j),
                                                rangeMin,rangeMax,conversionRate);
                                        cell.setCellValue(valRes.getValue().doubleValue());
                                        if(valRes.getValue().compareTo(BigDecimal.ZERO) !=0){
                                            lastValid.set(j, valRes.getValue());
                                        }
                                        break;
                                    default:
                                        break;
                                }
                                if(Optional.ofNullable(valRes).map(ValidationRes::isInvalid).orElse(Boolean.FALSE)){
                                    invalidCounter++;
                                }
                                if(Optional.ofNullable(valRes).map(ValidationRes::isZero).orElse(Boolean.FALSE)){
                                    zeroCounter++;
                                }
                            }
                        }

                        fis.close();
                        String fileOffset;
                        double errRate = (double)invalidCounter / ((double)( firstRow.getLastCellNum()
                                * (sheet.getLastRowNum()+1) - zeroCounter));
                        if(threshold.compareTo(BigDecimal.valueOf(errRate)) > 0){
                            fileOffset = "_valid";
                        }else{
                            fileOffset = "_invalid";
                        }

                        Row lastRow = sheet.createRow(sheet.getLastRowNum()+1);
                        Cell resultCell = lastRow.createCell(0);
                        resultCell.setCellValue("invalid data percentage is: " + errRate);
                        FileOutputStream outputStream = new FileOutputStream(
                                file.getName().replaceFirst("[.][^.]+$", "")+
                                        SystemConfiguration.OUTPUT_FILE_SUFFIX + fileOffset+ ".xlsx");
                        workbook.write(outputStream);
                        workbook.close();
                        outputStream.close();

                    } catch (FileNotFoundException e) {
                        throw new RuntimeException(e);
                    } catch (IOException | InvalidFormatException e) {
                        throw new RuntimeException(e);
                    }
                }
        );
    }
}