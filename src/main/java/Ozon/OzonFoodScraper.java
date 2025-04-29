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

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// 单个excel的数据爬取
public class OzonFoodScraper {

    public static void main(String[] args) throws IOException {
        String excelPath = "ozon_product_links_id4_100.xlsx";
        String outputPath = "ozon_product_info_id1.xlsx";
        String chromeDriverPath = "D:\\zgz\\HealthHope\\chromedriver-win64\\chromedriver.exe";

        System.setProperty("webdriver.chrome.driver", chromeDriverPath);

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.addArguments("--start-maximized");
        options.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});
        options.setExperimentalOption("useAutomationExtension", false);

        List<String> links = new ArrayList<>();
        FileInputStream fis = new FileInputStream(excelPath);
        Workbook workbook = new XSSFWorkbook(fis);
        Sheet sheet = workbook.getSheetAt(0);
        // 创建统一单元格样式
        CellStyle normalStyle = workbook.createCellStyle();
        normalStyle.setWrapText(false);  // 关闭自动换行
        Font font = workbook.createFont();
        font.setFontName("Arial");       // 设置字体
        font.setFontHeightInPoints((short)10); // 字体大小 10pt
        normalStyle.setFont(font);


        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row != null && row.getCell(0) != null) {
                links.add(row.getCell(0).getStringCellValue());
            }
        }
        fis.close();

        Row headerRow = sheet.getRow(0);
        if (headerRow == null) {
            headerRow = sheet.createRow(0);
        }
        String[] headers = {
                "数据网站链接", "分类", "名称", "图片链接", "营养计算方式",
                "蛋白质", "脂肪", "碳水化合物", "能量",
                "描述", "储存条件", "配料表", "净含量"
        };

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.getCell(i);
            if (cell == null) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }
        }

        int rowIndex = 1;
        for (String baseUrl : links) {
            System.out.println("\n▶ 正在抓取第 " + rowIndex + " 条链接: " + baseUrl);
            WebDriver driver = new ChromeDriver(options);
            try {
                ((JavascriptExecutor) driver).executeScript("Object.defineProperty(navigator, 'webdriver', {get: () => undefined})");
                driver.get(baseUrl);
                WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(18));
                Thread.sleep(8000);

                String category = "", name = "", desc = "", nutrition100 = "";
                String storageConditions = "", ingredients = "", weight = "", weight1 = "", pic = "";
                String calculationMethod = "";
                try {
                    // 名称
                    // WebElement nameElement = driver.findElement(By.xpath("//div[@data-widget='webProductHeading']//h1[contains(@class, 'tsHeadline550Medium')]"));
                    WebElement nameElement = driver.findElement(By.xpath("//div[@data-widget='webProductHeading']//h1[contains(@class, 'tsHeadline550Medium')]"));
                    // WebElement nameElement = driver.findElement(By.xpath("//div[@data-widget='webProductHeading']//h1"));
                    name = nameElement.getText().trim();

                    // 产品字段
                    // Map<String, String> productDetails = new LinkedHashMap<>();
                    // WebElement productElement = driver.findElement(By.xpath("//div[@data-widget='webShortCharacteristics']"));
                    // WebElement detailsContainer = productElement.findElement(By.xpath(".//div[contains(@class, 't8m_28')]"));
                    // List<WebElement> detailBlocks = detailsContainer.findElements(By.xpath("./div[contains(@class, 't8m_28')]"));
                    // List<WebElement> detailBlocks = productElement.findElements(By.xpath("./div"));
                    // WebElement aboutProduct = detailBlocks.get(1);
                    // 提取标签和值
                    // String categoryLabel = aboutProduct.findElement(By.xpath("./*[1]//span[contains(@class, 'tsBodyM')]")).getText().trim();
                    // WebElement test = aboutProduct.findElement(By.xpath("./*[1]"));
                    // String rowHtml = test.getAttribute("outerHTML");
                    // System.out.println(rowHtml);
                    // String categoryValue = aboutProduct.findElement(By.xpath("./*[1]/*[2]/a/div")).getText().trim();
                    // weight = aboutProduct.findElement(By.xpath("./*[1]//span[contains(@class, 'tsBodyM')]")).getText().trim();
                    // 定位所有属性块容器
                    Map<String, String> productDetails = new LinkedHashMap<>();
                    WebElement productElement = driver.findElement(By.xpath("//div[@data-widget='webShortCharacteristics']"));
                    WebElement detailsContainer = productElement.findElement(By.xpath("./div[2]"));
                    // String rowHtml = detailsContainer.getAttribute("outerHTML");
                    // System.out.println(rowHtml);
                    List<WebElement> detailBlocks = detailsContainer.findElements(By.xpath("./div"));
                    for (WebElement block : detailBlocks) {
                        WebElement labelElement = block.findElement(By.xpath(".//span[contains(@class, 'tsBodyM')]"));
                        String label = labelElement.getText().trim();

                        WebElement valueContainer = block.findElement(By.xpath("./div[2]"));
                        String value;
                        List<WebElement> linksInBlock = valueContainer.findElements(By.tagName("a"));
                        if (!linksInBlock.isEmpty()) {
                            List<String> values = new ArrayList<>();
                            for (WebElement link : linksInBlock) {
                                values.add(link.getText().trim());
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
                    WebDriverWait waitNutrition = new WebDriverWait(driver, Duration.ofSeconds(10));
                    // 等待营养信息模块出现
                    WebElement nutritionElement = waitNutrition.until(
                            ExpectedConditions.presenceOfElementLocated(By.xpath("//div[@data-widget='webNutritionInfo']"))
                    );
                    ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", nutritionElement);
                    Thread.sleep(3000); // 给予一点加载时间

                    // WebElement nutritionElement = driver.findElement(By.xpath("//div[@data-widget='webNutritionInfo']"));
                    // WebElement nutrition = nutritionElement.findElement(By.xpath("./div[1]"));
                    // String rowHtml = nutrition.getAttribute("outerHTML");
                    // System.out.println(rowHtml);
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
                    // 声明描述元素列表
                    List<WebElement> descElements = new ArrayList<>();
                    WebDriverWait waitDesc = new WebDriverWait(driver, Duration.ofSeconds(10));

                    // 等待至少一个 webDescription 元素加载出来
                    descElements = waitDesc.until(ExpectedConditions.presenceOfAllElementsLocatedBy(
                            By.xpath(".//div[@data-widget='webPdpGrid']//div[@data-widget='webDescription']")
                    ));

                    // 如果你还想滚动到它再提取内容
                    ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", descElements.get(0));
                    Thread.sleep(1000); // 稳定性提升
                    // List<WebElement> descElements = driver.findElements(By.xpath(".//div[@data-widget='webPdpGrid']//div[@data-widget='webDescription']"));

                    // 尝试提取主描述信息
                    try {
                        WebElement content = descElements.get(0).findElement(
                                By.xpath(".//div[@id='section-description']//div[contains(@class, 'RA-a1')]")
                        );
                        desc = content.getText().trim();
                    } catch (Exception e) {
                        System.out.println("主描述信息未找到，跳过该部分继续处理其他信息。");
                    }
                    // WebElement content = descElements.get(0).findElement(By.xpath(".//div[@id='section-description']//div[contains(@class, 'RA-a1')]"));
                    // // String rowHtml = content.getAttribute("outerHTML");
                    // // System.out.println(rowHtml);
                    // desc = content.getText().trim();
                    // 存储条件和配料表
                    // 提取存储条件与配料信息
                    try {
                        WebElement storageIngredient = descElements.get(1).findElement(
                                By.xpath(".//div[@id='section-description']//div[contains(@class, 'RA-a1')]")
                        );

                        List<WebElement> titles = storageIngredient.findElements(By.tagName("h3"));
                        List<WebElement> paras = storageIngredient.findElements(By.tagName("p"));

                        for (int i = 0; i < Math.min(titles.size(), paras.size()); i++) {
                            String titleText = titles.get(i).getText().trim();
                            String paraText = paras.get(i).getText().trim();

                            if (titleText.contains("Состав")) {
                                ingredients = paraText;
                            } else if (titleText.contains("Условия хранения")) {
                                storageConditions = paraText;
                            }
                        }
                    } catch (Exception e) {
                        System.out.println("存储条件或配料信息未找到。");
                    }


                    // 单独获取重量
                    // 定位特性容器
                    if (weight.equals("") || weight == null) {
                        WebElement webCharacteristics = driver.findElement(By.xpath("//div[@data-widget='webPdpGrid']//div[@data-widget='webCharacteristics']"));
                        WebElement characteristics = webCharacteristics.findElement(By.xpath("//div[@id='section-characteristics']"));
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


                    // 获取图片链接
                    List<String> imageUrls = new ArrayList<>();
                    WebElement gallery = driver.findElement(By.xpath(".//div[@data-widget='webPdpGrid']//div[@data-widget='webStickyColumn']//div[@data-widget='webGallery']"));
                    // String rowHtml = gallery.getAttribute("outerHTML");
                    // System.out.println(rowHtml);
                    List<WebElement> imgElements = wait.until(d ->
                            gallery.findElements(By.xpath(".//img"))
                    );

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

                } catch (Exception e) {
                    System.out.println("❌ 抓取失败: " + e.getMessage());
                }

                // 写入 Excel 当前行
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    row = sheet.createRow(rowIndex);
                }
                // 数据网站链接
                Cell cell0 = row.createCell(0);
                cell0.setCellValue(baseUrl);
                cell0.setCellStyle(normalStyle);
                // 分类
                Cell cell1 = row.createCell(1);
                cell1.setCellValue(category);
                cell1.setCellStyle(normalStyle);
                // 名称
                Cell cell2 = row.createCell(2);
                cell2.setCellValue(name);
                cell2.setCellStyle(normalStyle);
                // 图片链接
                Cell cell3 = row.createCell(3);
                cell3.setCellValue(pic);
                cell3.setCellStyle(normalStyle);
                // 营养计算
                String protein = "", fat = "", carb = "", kcal = "";
                try {
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
                } catch (Exception e) {
                    System.out.println("⚠️ 营养成分解析失败: " + nutrition100);
                }
                // 营养计算方式
                Cell cellCalculationMethod = row.createCell(4);
                cellCalculationMethod.setCellValue(calculationMethod);
                cellCalculationMethod.setCellStyle(normalStyle);
                // 蛋白质
                Cell cellProtein = row.createCell(5);
                cellProtein.setCellValue(protein);
                cellProtein.setCellStyle(normalStyle);
                // 脂肪
                Cell cellFat = row.createCell(6);
                cellFat.setCellValue(fat);
                cellFat.setCellStyle(normalStyle);
                // 碳水化合物
                Cell cellCarb = row.createCell(7);
                cellCarb.setCellValue(carb);
                cellCarb.setCellStyle(normalStyle);
                // 能量
                Cell cellKcal = row.createCell(8);
                cellKcal.setCellValue(kcal);
                cellKcal.setCellStyle(normalStyle);
                // 描述
                Cell cell5 = row.createCell(9);
                cell5.setCellValue(desc);
                cell5.setCellStyle(normalStyle);
                // 存储条件
                Cell cell6 = row.createCell(10);
                cell6.setCellValue(storageConditions);
                cell6.setCellStyle(normalStyle);
                // 配料表
                Cell cell7 = row.createCell(11);
                cell7.setCellValue(ingredients);
                cell7.setCellStyle(normalStyle);
                // 重量
                Cell cell8 = row.createCell(12);
                cell8.setCellValue(weight.isEmpty() ? weight1 : weight); // 优先用主字段抓到的重量，否则用后备字段
                cell8.setCellStyle(normalStyle);




            } catch (Exception e) {
                System.err.println("❌ 处理失败: " + baseUrl + "，错误信息: " + e.getMessage());
            } finally {
                driver.quit();
            }
            rowIndex++;
        }

        try (FileOutputStream fos = new FileOutputStream(outputPath)) {
            // 自动调整列宽以适应内容
            // for (int i = 0; i < headers.length; i++) {
            //     sheet.autoSizeColumn(i);
            // }
            workbook.write(fos);
        }
        workbook.close();
        System.out.println("\n✅ 所有商品信息已写入 Excel: " + outputPath);
    }
}
