package Ozon;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class ImageLinkProcessor {

    public static void main(String[] args) {
        String inputFilePath = "D:\\WorkSpaces\\javaWorkspace\\JavaCrawler\\ozon\\output\\name_is_null.xlsx";
        String outputFilePath = "D:\\WorkSpaces\\javaWorkspace\\JavaCrawler\\ozon\\output\\name_is_null.xlsx";

        try {
            processExcel(inputFilePath, outputFilePath);
            System.out.println("处理完成！");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void processExcel(String inputFilePath, String outputFilePath) throws IOException {
        FileInputStream fis = new FileInputStream(inputFilePath);
        Workbook workbook = new XSSFWorkbook(fis);
        Sheet sheet = workbook.getSheetAt(0); // 假设只处理第一个Sheet

        // 找到表头对应的列索引
        Row headerRow = sheet.getRow(0);
        int firstImageCol = -1;
        int imagesCol = -1;

        for (Cell cell : headerRow) {
            String header = cell.getStringCellValue().trim();
            if ("首图链接".equals(header)) {
                firstImageCol = cell.getColumnIndex();
            } else if ("图片链接".equals(header)) {
                imagesCol = cell.getColumnIndex();
            }
        }

        if (firstImageCol == -1 || imagesCol == -1) {
            throw new IllegalArgumentException("未找到 '首图链接' 或 '图片链接' 列！");
        }

        // 遍历每一行处理
        for (int i = 1; i <= sheet.getLastRowNum(); i++) { // 从1开始跳过表头
            Row row = sheet.getRow(i);
            if (row == null) {
                continue;
            }

            Cell imagesCell = row.getCell(imagesCol);
            if (imagesCell == null) {
                continue;
            }

            String imagesText = imagesCell.getStringCellValue();
            if (imagesText == null || imagesText.trim().isEmpty()) {
                continue;
            }

            String[] imageLinks = imagesText.split(";");
            if (imageLinks.length > 0) {
                // 设置首图链接
                Cell firstImageCell = row.getCell(firstImageCol);
                if (firstImageCell == null) {
                    firstImageCell = row.createCell(firstImageCol);
                }
                firstImageCell.setCellValue(imageLinks[0].trim());

                // 剩下的链接重新组合
                if (imageLinks.length > 1) {
                    StringBuilder remaining = new StringBuilder();
                    for (int j = 1; j < imageLinks.length; j++) {
                        if (j > 1) {
                            remaining.append(";");
                        }
                        remaining.append(imageLinks[j].trim());
                    }
                    imagesCell.setCellValue(remaining.toString());
                } else {
                    imagesCell.setCellValue("");
                }
            }
        }

        fis.close();

        // 保存修改后的文件
        FileOutputStream fos = new FileOutputStream(outputFilePath);
        workbook.write(fos);
        fos.close();
        workbook.close();
    }
}
