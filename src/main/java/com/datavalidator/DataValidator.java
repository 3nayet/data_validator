package com.datavalidator;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

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
        SystemConfiguration.getListOfInputFiles().forEach(
                file -> {
                    try {
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
                                BigDecimal res;
                                switch (formulaEvaluator.evaluateInCell(cell).getCellType()) {
                                    case Cell.CELL_TYPE_BLANK:
                                    case Cell.CELL_TYPE_ERROR:
                                    case Cell.CELL_TYPE_BOOLEAN:
                                    case Cell.CELL_TYPE_FORMULA:
                                         res = SystemConfiguration.validateValue(null, lastValid.get(j),
                                                rangeMin,rangeMax,conversionRate);
                                        cell.setCellValue(res.doubleValue());
                                        lastValid.set(j, res);
                                        break;
                                    case Cell.CELL_TYPE_STRING:
                                        if(SystemConfiguration.isNumeric(cell.getStringCellValue())){
                                            res = SystemConfiguration.validateValue(
                                                    new BigDecimal(cell.getStringCellValue()), lastValid.get(j),
                                                    rangeMin,rangeMax,conversionRate);
                                            cell.setCellValue(res.doubleValue());
                                            if(res.compareTo(BigDecimal.ZERO) !=0){
                                                lastValid.set(j, res);
                                            }
                                        }else{
                                            res = SystemConfiguration.validateValue(null, lastValid.get(j),
                                                    rangeMin,rangeMax,conversionRate);
                                            cell.setCellValue(res.doubleValue());
                                            lastValid.set(j, res);
                                        }
                                        break;
                                    case Cell.CELL_TYPE_NUMERIC:
                                         res = SystemConfiguration.validateValue(
                                                 BigDecimal.valueOf(cell.getNumericCellValue()), lastValid.get(j),
                                                rangeMin,rangeMax,conversionRate);
                                        cell.setCellValue(res.doubleValue());
                                        if(res.compareTo(BigDecimal.ZERO) !=0){
                                            lastValid.set(j, res);
                                        }
                                        break;
                                    default:
                                        break;
                                }
                            }
                        }

                        fis.close();
                        FileOutputStream outputStream = new FileOutputStream(
                                file.getName().replaceFirst("[.][^.]+$", "") + "_output.xlsx");
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