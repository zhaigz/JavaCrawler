package Heb;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class ExcelMerger {

    public static void main(String[] args) {
        // 把你要合并的文件路径放这里
        List<String> inputFiles = new ArrayList<>();
        inputFiles.add("D:\\WorkSpaces\\javaWorkspace\\JavaCrawler\\Heb\\Heb_Urls_id1.xlsx");
        inputFiles.add("D:\\WorkSpaces\\javaWorkspace\\JavaCrawler\\Heb\\Heb_Urls_id2.xlsx");
        inputFiles.add("D:\\WorkSpaces\\javaWorkspace\\JavaCrawler\\Heb\\Heb_Urls_id3.xlsx");
        inputFiles.add("D:\\WorkSpaces\\javaWorkspace\\JavaCrawler\\Heb\\Heb_Urls_id4.xlsx");
        inputFiles.add("D:\\WorkSpaces\\javaWorkspace\\JavaCrawler\\Heb\\Heb_Urls_id5.xlsx");
        inputFiles.add("D:\\WorkSpaces\\javaWorkspace\\JavaCrawler\\Heb\\Heb_Urls_id6.xlsx");
        inputFiles.add("D:\\WorkSpaces\\javaWorkspace\\JavaCrawler\\Heb\\Heb_Urls_id7.xlsx");
        inputFiles.add("D:\\WorkSpaces\\javaWorkspace\\JavaCrawler\\Heb\\Heb_Urls_id8.xlsx");
        inputFiles.add("D:\\WorkSpaces\\javaWorkspace\\JavaCrawler\\Heb\\Heb_Urls_id9.xlsx");
        inputFiles.add("D:\\WorkSpaces\\javaWorkspace\\JavaCrawler\\Heb\\Heb_Urls_id10.xlsx");
        inputFiles.add("D:\\WorkSpaces\\javaWorkspace\\JavaCrawler\\Heb\\Heb_Urls_id11.xlsx");
        inputFiles.add("D:\\WorkSpaces\\javaWorkspace\\JavaCrawler\\Heb\\Heb_Urls_id12.xlsx");        inputFiles.add("D:\\WorkSpaces\\javaWorkspace\\JavaCrawler\\Heb\\Heb_Urls_id1.xlsx");
        inputFiles.add("D:\\WorkSpaces\\javaWorkspace\\JavaCrawler\\Heb\\Heb_Urls_id13.xlsx");
        inputFiles.add("D:\\WorkSpaces\\javaWorkspace\\JavaCrawler\\Heb\\Heb_Urls_id14.xlsx");
        inputFiles.add("D:\\WorkSpaces\\javaWorkspace\\JavaCrawler\\Heb\\Heb_Urls_id15.xlsx");
        inputFiles.add("D:\\WorkSpaces\\javaWorkspace\\JavaCrawler\\Heb\\Heb_Urls_id16.xlsx");
        inputFiles.add("D:\\WorkSpaces\\javaWorkspace\\JavaCrawler\\Heb\\Heb_Urls_id17.xlsx");
        inputFiles.add("D:\\WorkSpaces\\javaWorkspace\\JavaCrawler\\Heb\\Heb_Urls_id18.xlsx");
        inputFiles.add("D:\\WorkSpaces\\javaWorkspace\\JavaCrawler\\Heb\\Heb_Urls_id19.xlsx");
        inputFiles.add("D:\\WorkSpaces\\javaWorkspace\\JavaCrawler\\Heb\\Heb_Urls_id20.xlsx");
        inputFiles.add("D:\\WorkSpaces\\javaWorkspace\\JavaCrawler\\Heb\\Heb_Urls_id21.xlsx");
        inputFiles.add("D:\\WorkSpaces\\javaWorkspace\\JavaCrawler\\Heb\\Heb_Urls_id22.xlsx");
        inputFiles.add("D:\\WorkSpaces\\javaWorkspace\\JavaCrawler\\Heb\\Heb_Urls_id23.xlsx");        inputFiles.add("D:\\WorkSpaces\\javaWorkspace\\JavaCrawler\\Heb\\Heb_Urls_id1.xlsx");
        inputFiles.add("D:\\WorkSpaces\\javaWorkspace\\JavaCrawler\\Heb\\Heb_Urls_id24.xlsx");        inputFiles.add("D:\\WorkSpaces\\javaWorkspace\\JavaCrawler\\Heb\\Heb_Urls_id1.xlsx");
        inputFiles.add("D:\\WorkSpaces\\javaWorkspace\\JavaCrawler\\Heb\\Heb_Urls_id25.xlsx");
        inputFiles.add("D:\\WorkSpaces\\javaWorkspace\\JavaCrawler\\Heb\\Heb_Urls_id26.xlsx");
        inputFiles.add("D:\\WorkSpaces\\javaWorkspace\\JavaCrawler\\Heb\\Heb_Urls_id27.xlsx");
        inputFiles.add("D:\\WorkSpaces\\javaWorkspace\\JavaCrawler\\Heb\\Heb_Urls_id28.xlsx");
        inputFiles.add("D:\\WorkSpaces\\javaWorkspace\\JavaCrawler\\Heb\\Heb_Urls_id29.xlsx");
        inputFiles.add("D:\\WorkSpaces\\javaWorkspace\\JavaCrawler\\Heb\\Heb_Urls_id30.xlsx");        inputFiles.add("D:\\WorkSpaces\\javaWorkspace\\JavaCrawler\\Heb\\Heb_Urls_id1.xlsx");




        String outputFile = "合并后的总表.xlsx";

        try {
            mergeExcels(inputFiles, outputFile);
            System.out.println("合并完成！");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void mergeExcels(List<String> inputFilePaths, String outputFilePath) throws IOException {
        Workbook outputWorkbook = new XSSFWorkbook();
        Sheet outputSheet = outputWorkbook.createSheet("合并结果");

        int currentRowNum = 0;
        boolean isFirstFile = true;

        for (String filePath : inputFilePaths) {
            FileInputStream fis = new FileInputStream(filePath);
            Workbook inputWorkbook = new XSSFWorkbook(fis);
            Sheet inputSheet = inputWorkbook.getSheetAt(0);

            int firstRowNum = inputSheet.getFirstRowNum();
            int lastRowNum = inputSheet.getLastRowNum();

            for (int i = firstRowNum; i <= lastRowNum; i++) {
                Row inputRow = inputSheet.getRow(i);
                if (inputRow == null) {
                    continue;
                }

                // 第一份文件保留表头，其它文件跳过表头（第0行）
                if (!isFirstFile && i == firstRowNum) {
                    continue;
                }

                Row outputRow = outputSheet.createRow(currentRowNum++);

                for (int j = 0; j < inputRow.getLastCellNum(); j++) {
                    Cell inputCell = inputRow.getCell(j);
                    if (inputCell == null) {
                        continue;
                    }

                    Cell outputCell = outputRow.createCell(j);
                    copyCellValue(inputCell, outputCell);
                }
            }

            isFirstFile = false;
            fis.close();
            inputWorkbook.close();
        }

        // 写入到新文件
        FileOutputStream fos = new FileOutputStream(outputFilePath);
        outputWorkbook.write(fos);
        fos.close();
        outputWorkbook.close();
    }

    private static void copyCellValue(Cell fromCell, Cell toCell) {
        if (fromCell == null) {
            return;
        }

        switch (fromCell.getCellType()) {
            case STRING:
                toCell.setCellValue(fromCell.getStringCellValue());
                break;
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(fromCell)) {
                    toCell.setCellValue(fromCell.getDateCellValue());
                } else {
                    toCell.setCellValue(fromCell.getNumericCellValue());
                }
                break;
            case BOOLEAN:
                toCell.setCellValue(fromCell.getBooleanCellValue());
                break;
            case FORMULA:
                toCell.setCellFormula(fromCell.getCellFormula());
                break;
            case BLANK:
                toCell.setBlank();
                break;
            default:
                toCell.setCellValue(fromCell.toString());
                break;
        }
    }
}
