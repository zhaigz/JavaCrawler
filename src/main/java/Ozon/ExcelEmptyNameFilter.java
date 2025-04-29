package Ozon;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ExcelEmptyNameFilter {

    /**
     * 从Excel中筛选出"名称"字段为空的"数据网站链接"
     * @param excelFilePath Excel文件路径
     * @return 名称为空的链接列表
     * @throws IOException
     */
    public static List<String> findLinksWithEmptyName(String excelFilePath) throws IOException {
        List<String> emptyNameLinks = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(excelFilePath);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0); // 默认取第一个Sheet

            // 读取表头，确定“数据网站链接”和“名称”字段的列索引
            Row headerRow = sheet.getRow(0);
            int linkCol = -1;
            int nameCol = -1;
            for (Cell cell : headerRow) {
                String cellValue = cell.getStringCellValue().trim();
                if ("数据网站链接".equals(cellValue)) {
                    linkCol = cell.getColumnIndex();
                } else if ("名称".equals(cellValue)) {
                    nameCol = cell.getColumnIndex();
                }
            }

            if (linkCol == -1 || nameCol == -1) {
                throw new IllegalArgumentException("未找到“数据网站链接”或“名称”列，请检查表头！");
            }

            // 遍历数据行
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }

                Cell nameCell = row.getCell(nameCol);
                String nameValue = (nameCell != null) ? nameCell.toString().trim() : "";

                if (nameValue.isEmpty()) {
                    // 名称字段为空，取链接
                    Cell linkCell = row.getCell(linkCol);
                    if (linkCell != null) {
                        String linkValue = linkCell.toString().trim();
                        if (!linkValue.isEmpty()) {
                            emptyNameLinks.add(linkValue);
                        }
                    }
                }
            }
        }

        return emptyNameLinks;
    }

    /**
     * 把链接列表写到一个新的Excel文件
     * @param links 链接列表
     * @param outputPath 输出文件路径
     * @throws IOException
     */
    public static void writeLinksToExcel(List<String> links, String outputPath) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("EmptyNameLinks");

            // 写表头
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("数据网站链接");

            // 写数据
            for (int i = 0; i < links.size(); i++) {
                Row row = sheet.createRow(i + 1);
                row.createCell(0).setCellValue(links.get(i));
            }

            // 自动调整列宽
            sheet.autoSizeColumn(0);

            // 写入文件
            try (FileOutputStream fos = new FileOutputStream(outputPath)) {
                workbook.write(fos);
            }
        }
    }

    public static void main(String[] args) {
        try {
            String inputExcelPath = "D:\\WorkSpaces\\javaWorkspace\\JavaCrawler\\ozon\\ozon_food.xlsx";   // 填你的输入Excel
            String outputExcelPath = "D:\\WorkSpaces\\javaWorkspace\\JavaCrawler\\ozon\\name_is_null.xlsx";    // 填你的想要输出的文件名

            List<String> links = findLinksWithEmptyName(inputExcelPath);

            if (links.isEmpty()) {
                System.out.println("没有找到名称为空的数据！");
            } else {
                writeLinksToExcel(links, outputExcelPath);
                System.out.println("成功输出到Excel文件：" + outputExcelPath);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
