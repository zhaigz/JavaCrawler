package Ozon;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MultiSheetOzonScraper {
    public static void main(String[] args) throws IOException, InterruptedException {
        String excelPath = "ozon_category.xlsx";
        String outputPath = "ozon_info.xlsx";
        String chromeDriverPath = "D:\\zgz\\HealthHope\\chromedriver-win64\\chromedriver.exe";

        System.setProperty("webdriver.chrome.driver", chromeDriverPath);

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.addArguments("--start-maximized");
        options.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});
        options.setExperimentalOption("useAutomationExtension", false);

        FileInputStream fis = new FileInputStream(excelPath);
        Workbook workbook = new XSSFWorkbook(fis);
        // Workbook outputWorkbook = new XSSFWorkbook();
        Workbook outputWorkbook = loadOrCreateWorkbook(outputPath); // 追加方式加载
        CellStyle normalStyle = outputWorkbook.createCellStyle();
        Font font = outputWorkbook.createFont();
        font.setFontName("Arial");
        font.setFontHeightInPoints((short) 10);
        normalStyle.setFont(font);
        normalStyle.setWrapText(false);
        for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
            Sheet inputSheet = workbook.getSheetAt(sheetIndex);
            String sheetName = inputSheet.getSheetName();
            // 检查输出工作簿中是否已存在该 sheet 名
            Sheet outputSheet = outputWorkbook.getSheet(sheetName);
            if (outputSheet == null) {
                outputSheet = outputWorkbook.createSheet(sheetName);
                Row headerRow = outputSheet.createRow(0);
                String[] headers = {
                        "数据网站链接", "分类", "名称", "图片链接", "营养计算方式",
                        "蛋白质", "脂肪", "碳水化合物", "能量",
                        "描述", "储存条件", "配料表", "净含量"
                };
                for (int i = 0; i < headers.length; i++) {
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(headers[i]);
                    cell.setCellStyle(normalStyle);
                }
            }

            // 读取当前 sheet 所有链接
            List<String> links = new ArrayList<>();
            for (int i = 0; i <= inputSheet.getLastRowNum(); i++) {
                Row row = inputSheet.getRow(i);
                if (row != null && row.getCell(0) != null) {
                    links.add(row.getCell(0).getStringCellValue());
                }
            }

            // int rowIndex = 1;
            // 如果只有表头（第0行），则从第1行开始；如果已经写入数据了，就从最后一行 + 1 开始
            int rowIndex = outputSheet.getLastRowNum() + 1;
            if (rowIndex == 0 && outputSheet.getRow(0) != null && outputSheet.getRow(1) == null) {
                rowIndex = 1;
            } else {
                rowIndex += 1;
            }
            int linkCount = 0;
            List<Integer> processedRowIndexes = new ArrayList<>();
            int printRowIndex = 1;
            for (int i = 0; i < links.size(); i++) {
                String link = links.get(i);
                System.out.println("\n▶ 正在抓取第 " + inputSheet.getSheetName() + " 表中的第 " + printRowIndex  + " 条链接: " + link);
                WebDriver driver = new ChromeDriver(options);
                try {
                    // ...【省略：网页抓取逻辑不变】...
                    ((JavascriptExecutor) driver).executeScript("Object.defineProperty(navigator, 'webdriver', {get: () => undefined})");
                    // driver.get(link);
                    if (link != null && !link.trim().isEmpty()) {
                        driver.get(link);
                    } else {
                        System.out.println("跳过无效链接: " + link);
                        continue;  // 或 return / break，取决于你的逻辑
                    }
                    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
                    Thread.sleep(3000);

                    String category = "", name = "", desc = "", nutrition100 = "";
                    String storageConditions = "", ingredients = "", weight = "", weight1 = "", pic = "", calculationMethod = "";

                    // 名称
                    WebElement nameElement = driver.findElement(By.xpath("//div[@data-widget='webProductHeading']//h1[contains(@class, 'tsHeadline550Medium')]"));
                    name = nameElement.getText().trim();

                    //
                    Map<String, String> productDetails = new LinkedHashMap<>();
                    WebElement productElement = driver.findElement(By.xpath("//div[@data-widget='webShortCharacteristics']/div[2]"));
                    List<WebElement> detailBlocks = productElement.findElements(By.xpath("./div"));
                    for (WebElement block : detailBlocks) {
                        WebElement labelElement = block.findElement(By.xpath(".//span[contains(@class, 'tsBodyM')]"));
                        String label = labelElement.getText().trim();
                        WebElement valueContainer = block.findElement(By.xpath("./div[2]"));
                        String value;
                        List<WebElement> linksInBlock = valueContainer.findElements(By.tagName("a"));
                        if (!linksInBlock.isEmpty()) {
                            List<String> values = new ArrayList<>();
                            for (WebElement linkEl : linksInBlock) {
                                values.add(linkEl.getText().trim());
                            }
                            value = String.join(", ", values);
                        } else {
                            WebElement valueElement = valueContainer.findElement(By.xpath(".//span[contains(@class, 'tsBody400Small')] | .//span[contains(@class, 'm7t_28')]"));
                            value = valueElement.getText().trim();
                        }
                        productDetails.put(label, value);
                    }
                    category = productDetails.getOrDefault("Тип", "");
                    weight = productDetails.getOrDefault("Вес товара, г", "");

                    // 每100g营养成分
                    WebDriverWait waitNutrition = new WebDriverWait(driver, Duration.ofSeconds(5));
                    // 等待营养信息模块出现
                    WebElement nutritionElement = waitNutrition.until(
                            ExpectedConditions.presenceOfElementLocated(By.xpath("//div[@data-widget='webNutritionInfo']"))
                    );
                    ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", nutritionElement);
                    Thread.sleep(1000); // 给予一点加载时间
                    calculationMethod = nutritionElement.findElement(By.xpath("./div[1]")).getText().trim();
                    WebElement data = nutritionElement.findElement(By.xpath("./div[2]"));
                    List<WebElement> dataItems = data.findElements(By.xpath("./div"));
                    List<String> nutritionList = new ArrayList<>();
                    for (WebElement item : dataItems) {
                        String val = item.findElement(By.xpath("./div[1]")).getText().trim();
                        String n = item.findElement(By.xpath("./div[2]")).getText().trim();
                        nutritionList.add(val + n);
                    }
                    nutrition100 = String.join(", ", nutritionList);
                    // 描述
                    List<WebElement> descElements = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(
                            By.xpath(".//div[@data-widget='webPdpGrid']//div[@data-widget='webDescription']")
                    ));
                    ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", descElements.get(0));
                    Thread.sleep(500);
                    WebElement content = descElements.get(0).findElement(By.xpath(".//div[@id='section-description']//div[contains(@class, 'RA-a1')]"));
                    desc = content.getText().trim();

                    // 存储条件、配料表
                    WebElement storageIngredient = descElements.get(1).findElement(By.xpath(".//div[@id='section-description']//div[contains(@class, 'RA-a1')]"));
                    List<WebElement> titles = storageIngredient.findElements(By.tagName("h3"));
                    List<WebElement> paras = storageIngredient.findElements(By.tagName("p"));
                    for (int j = 0; j < Math.min(titles.size(), paras.size()); j++) {
                        String titleText = titles.get(j).getText().trim();
                        String paraText = paras.get(j).getText().trim();
                        if (titleText.contains("Состав")) {
                            ingredients = paraText;
                        } else if (titleText.contains("Условия хранения")) {
                            storageConditions = paraText;
                        }
                    }

                    // 图片
                    List<String> imageUrls = new ArrayList<>();
                    WebElement gallery = driver.findElement(By.xpath(".//div[@data-widget='webPdpGrid']//div[@data-widget='webStickyColumn']//div[@data-widget='webGallery']"));
                    List<WebElement> imgElements = wait.until(d -> gallery.findElements(By.xpath(".//img")));
                    for (WebElement img : imgElements) {
                        String srcset = img.getAttribute("srcset");
                        if (srcset != null && !srcset.isEmpty()) {
                            String[] sources = srcset.split(", ");
                            if (sources.length > 0) {
                                String highResUrl = sources[sources.length - 1].split(" ")[0];
                                imageUrls.add(highResUrl);
                            }
                        }
                        String src = img.getAttribute("src");
                        if (src != null && !src.isEmpty() && !imageUrls.contains(src)) {
                            imageUrls.add(src);
                        }
                    }
                    pic = String.join(";", imageUrls);
                    // 写入 Excel 输出文件
                    // Row outputRow = outputSheet.createRow(rowIndex++);
                    // outputRow.createCell(0).setCellValue(link);
                    Row outRow = outputSheet.createRow(rowIndex++);
                    outRow.createCell(0).setCellValue(link);
                    outRow.createCell(1).setCellValue(category);
                    outRow.createCell(2).setCellValue(name);
                    outRow.createCell(3).setCellValue(pic);
                    outRow.createCell(4).setCellValue(calculationMethod);

                    // 营养成分
                    String protein = "", fat = "", carb = "", kcal = "";
                        String[] nutritionParts = nutrition100.split(",");
                        for (String part : nutritionParts) {
                            part = part.trim();
                            if (part.contains("белки")) {
                                protein = part.replaceAll("[^0-9.,]", "").replace(",", ".");
                            } else if (part.contains("жиры")) {
                                fat = part.replaceAll("[^0-9.,]", "").replace(",", ".");
                            } else if (part.contains("углеводы")) {
                                carb = part.replaceAll("[^0-9.,]", "").replace(",", ".");
                            } else if (part.contains("ккал")) {
                                kcal = part.replaceAll("[^0-9.,]", "").replace(",", ".");
                            }
                        }

                    // 单独获取重量
                    if (weight.equals("") || weight == null) {
                        // WebElement webCharacteristics = driver.findElement(By.xpath("//div[@data-widget='webPdpGrid']//div[@data-widget='webCharacteristics']"));
                        WebElement characteristics = driver.findElement(By.xpath("//div[@data-widget='webCharacteristics']//div[@id='section-characteristics']"));
                        // 获取所有 dl 元素（基于特性容器）
                        List<WebElement> dlList = characteristics.findElements(By.xpath(".//dl"));
                        // 遍历每个 dl 元素
                        for (WebElement dl : dlList) {
                            try {
                                // 获取 dt 中的文本（特性名称）
                                WebElement dtElement = dl.findElement(By.tagName("dt"));
                                String dtText = dtElement.getText().trim();
                                // 判断是否为目标特性
                                if ("Вес товара, г".equals(dtText)) {
                                    // 获取对应的 dd 值
                                    WebElement ddElement = dl.findElement(By.tagName("dd"));
                                    weight1 = ddElement.getText().trim();
                                    break; // 找到后退出循环
                                }
                            } catch (Exception e) {
                                // 忽略异常（如元素不存在）
                            }
                        }

                    }
                    outRow.createCell(5).setCellValue(protein);
                    outRow.createCell(6).setCellValue(fat);
                    outRow.createCell(7).setCellValue(carb);
                    outRow.createCell(8).setCellValue(kcal);

                    outRow.createCell(9).setCellValue(desc);
                    outRow.createCell(10).setCellValue(storageConditions);
                    outRow.createCell(11).setCellValue(ingredients);
                    outRow.createCell(12).setCellValue(weight.isEmpty() ? weight1 : weight);
                    for (int k = 0; k <= 12; k++) {
                        outRow.getCell(k).setCellStyle(normalStyle);
                    }

                    processedRowIndexes.add(i);  // 记录已处理的行索引

                    linkCount++;
                    printRowIndex++;
                    if (linkCount % 5 == 0 || linkCount == links.size()) {
                        // 1. 保存数据到输出文件
                        try (FileOutputStream fos = new FileOutputStream(outputPath)) {
                            outputWorkbook.write(fos);
                            System.out.println("📁 已写入 " + linkCount + " 条数据到输出文件");
                        }

                        // 2. 删除已处理链接行
                        removeProcessedRows(inputSheet, processedRowIndexes);
                        try (FileOutputStream categoryFos = new FileOutputStream(excelPath)) {
                            workbook.write(categoryFos);
                            System.out.println("🧹 已清理已爬取链接并保存回分类文件: " + inputSheet.getSheetName());
                        }
                        processedRowIndexes.clear();  // 清空已处理行记录
                    }

                } catch (Exception e) {
                    System.out.println("❌ 抓取数据不完整: " + e.getMessage());
                } finally {
                    driver.quit();
                }
            }
            // 1. 保存数据到输出文件
            try (FileOutputStream fos = new FileOutputStream(outputPath)) {
                outputWorkbook.write(fos);
                System.out.println("📁 已写入 " + linkCount + " 条数据到输出文件");
            }

            // 2. 删除已处理链接行
            removeProcessedRows(inputSheet, processedRowIndexes);
            try (FileOutputStream categoryFos = new FileOutputStream(excelPath)) {
                workbook.write(categoryFos);
                System.out.println("🧹 已清理已爬取链接并保存回分类文件: " + inputSheet.getSheetName());
            }
            processedRowIndexes.clear();  // 清空已处理行记录
        }
        // 最终关闭资源
        try {
            workbook.close();
            outputWorkbook.close();
            fis.close();
        } catch (IOException e) {
            System.out.println("⚠️ 资源关闭时出错: " + e.getMessage());
        }

        System.out.println("🎉 所有工作完成！");

    }
    // 加载或创建工作簿（追加方式）
    public static Workbook loadOrCreateWorkbook(String filePath) throws IOException {
        File file = new File(filePath);
        if (file.exists()) {
            FileInputStream fis = new FileInputStream(file);
            return new XSSFWorkbook(fis); // 加载现有工作簿
        } else {
            return new XSSFWorkbook(); // 创建新工作簿
        }
    }

    public static void removeProcessedRows(Sheet sheet, List<Integer> rowIndexes) {
        // 倒序删除，避免索引错位
        rowIndexes.sort((a, b) -> b - a);
        for (int rowIndex : rowIndexes) {
            Row row = sheet.getRow(rowIndex);
            if (row != null) {
                sheet.removeRow(row);
                // 可选：移动下方所有行向上
                if (rowIndex < sheet.getLastRowNum()) {
                    sheet.shiftRows(rowIndex + 1, sheet.getLastRowNum(), -1);
                }
            }
        }
    }

}