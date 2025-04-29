package Ozon;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MultiThread {
    // 每10个任务保存一次数据，可根据需要调整
    private static final int SAVE_THRESHOLD = 100;
    // 线程池线程数，根据机器性能调整
    private static final int THREAD_COUNT = 8;

    public static void main(String[] args) throws IOException, InterruptedException {
        String excelPath = "D:\\WorkSpaces\\javaWorkspace\\JavaCrawler\\ozon\\name_is_null.xlsx";
        String outputPath = "D:\\WorkSpaces\\javaWorkspace\\JavaCrawler\\ozon\\output\\name_is_null.xlsx";
        String chromeDriverPath = "D:\\zgz\\HealthHope\\chromedriver-win64\\chromedriver.exe";
        System.setProperty("webdriver.chrome.driver", chromeDriverPath);

        // 配置 ChromeOptions（多线程时，每个任务自行创建 WebDriver 实例时均需使用此配置）
        ChromeOptions options = new ChromeOptions();
        // 全局关闭 Selenium 的日志
        Logger.getLogger("org.openqa.selenium").setLevel(Level.OFF);
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.addArguments("--start-maximized");
       // options.addArguments("--headless"); // 或 "--headless=new"
       // options.addArguments("--disable-gpu"); // 避免某些图形依赖失败
       // options.addArguments("--no-sandbox"); // 某些服务器上必加
       // options.addArguments("--disable-dev-shm-usage"); // 防止 /dev/shm 满
       // options.addArguments("--window-size=1920,1080"); // 有些布局和屏幕大小有关
       // options.addArguments("user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64)..."); // 模拟正常浏览器
        options.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});
        options.setExperimentalOption("useAutomationExtension", false);

        ChromeDriverService service = new ChromeDriverService.Builder()
                .usingDriverExecutable(new File(chromeDriverPath))
                .withSilent(true) // 静默启动，关闭日志
                .build();

        // 加载分类文件和输出文件（追加方式）
        FileInputStream fis = new FileInputStream(excelPath);
        Workbook workbook = new XSSFWorkbook(fis);
        Workbook outputWorkbook = loadOrCreateWorkbook(outputPath);

        // 创建统一的 CellStyle
        CellStyle normalStyle = outputWorkbook.createCellStyle();
        Font font = outputWorkbook.createFont();
        font.setFontName("Arial");
        font.setFontHeightInPoints((short) 10);
        normalStyle.setFont(font);
        normalStyle.setWrapText(false);

        // 对每个 sheet 进行处理
        for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
            Sheet inputSheet = workbook.getSheetAt(sheetIndex);
            final String sheetName = inputSheet.getSheetName();
            // 检查输出工作簿中是否已存在该 sheet
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

            // 读取当前 sheet 中所有链接，并记录其所在行索引（便于后续清除）
            List<String> links = new ArrayList<>();
            List<Integer> linkRowIndices = new ArrayList<>();
            for (int i = 0; i <= inputSheet.getLastRowNum(); i++) {
                Row row = inputSheet.getRow(i);
                if (row != null && row.getCell(0) != null) {
                    links.add(row.getCell(0).getStringCellValue());
                    linkRowIndices.add(i);
                }
            }

            // 确定输出表起始行（已经写入的数据数）
            int rowIndex = outputSheet.getLastRowNum();
            if (rowIndex == 0 && outputSheet.getRow(0) != null && outputSheet.getRow(1) == null) {
                rowIndex = 1;
            } else {
                rowIndex += 1;
            }
            // AtomicInteger 用于统一控制输出 sheet 的行索引
            AtomicInteger outputRowIndex = new AtomicInteger(rowIndex);
            // 线程安全的 List 用于记录当前 sheet 中已处理的 inputSheet 行索引
            List<Integer> processedRowIndexes = Collections.synchronizedList(new ArrayList<>());
            // 记录要删除的行
            List<Integer> removeRowIndexes = Collections.synchronizedList(new ArrayList<>());

            // 创建线程池
            ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
            // 使用 CountDownLatch 确保所有任务完成
            CountDownLatch latch = new CountDownLatch(links.size());

            // 为每个链接提交抓取任务
            for (int i = 0; i < links.size(); i++) {
                final int linkIndex = i;  // 对应 linkRowIndices 中的行号
                final String link = links.get(i);
                Sheet finalOutputSheet = outputSheet;
                executor.submit(() -> {
                    // 每个任务独立创建 WebDriver 实例
                    WebDriver driver = null;
                    try {
                        driver = new ChromeDriver(service, options);
                        // System.out.println("\n▶ [" + sheetName + "] 正在抓取第 " + (linkIndex + 1) + " 条链接: " + link);
                        System.out.println("\n▶ [" + sheetName + "] 正在抓取第 " + (linkIndex + 1) + " 条链接: ");


                        // 隐藏特性
                        ((JavascriptExecutor) driver).executeScript("Object.defineProperty(navigator, 'webdriver', {get: () => undefined})");
                        if (link != null && !link.trim().isEmpty()) {
                            driver.get(link);
                            removeRowIndexes.add(linkRowIndices.get(linkIndex)); // ✅ 标记为待删除
                        } else {
                            System.out.println("跳过无效链接: " + link);
                            latch.countDown();
                            return;
                        }

                        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
                        // 可根据实际情况调整等待时长
                        Thread.sleep(3000);

                        // 初始化各字段
                        String category = "", name = "", desc = "", nutrition100 = "";
                        String storageConditions = "", ingredients = "", weight = "", weight1 = "", pic = "", calculationMethod = "";

                        // 名称抓取
                        // 名称
                        // try {
                            WebElement nameElement = driver.findElement(By.xpath("//div[@data-widget='webProductHeading']//h1[contains(@class, 'tsHeadline550Medium')]"));
                            name = nameElement.getText().trim();
                        // } catch (Exception ignore) {
                        // }

                        //
                        try {
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
                        } catch (Exception ignore) {
                        }

                        // 每100g营养成分
                        // try {
                            WebDriverWait waitNutrition = new WebDriverWait(driver, Duration.ofSeconds(8));
                            // 等待营养信息模块出现
                            WebElement nutritionElement = waitNutrition.until(
                                    ExpectedConditions.presenceOfElementLocated(By.xpath("//div[@data-widget='webNutritionInfo']"))
                            );
                            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", nutritionElement);
                            Thread.sleep(2000); // 给予一点加载时间
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
                        // } catch (Exception ignore) {
                        // }

                        // 描述
                        try {
                            WebDriverWait waitDesc = new WebDriverWait(driver, Duration.ofSeconds(5));
                            List<WebElement> descElements = waitDesc.until(ExpectedConditions.presenceOfAllElementsLocatedBy(
                                    By.xpath(".//div[@data-widget='webPdpGrid']//div[@data-widget='webDescription']")
                            ));
                            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", descElements.get(0));
                            Thread.sleep(1000);
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
                        } catch (Exception ignore) {
                        }

                        // 图片
                        try {
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
                        } catch (Exception ignore) {
                        }

                        // 营养成分
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
                        // 单独获取重量
                        try {
                            if (weight.equals("") || weight == null) {
                                WebElement characteristics = driver.findElement(By.xpath("//div[@data-widget='webCharacteristics']//div[@id='section-characteristics']"));
                                List<WebElement> dlList = characteristics.findElements(By.xpath(".//dl"));
                                for (WebElement dl : dlList) {
                                    try {
                                        WebElement dtElement = dl.findElement(By.tagName("dt"));
                                        String dtText = dtElement.getText().trim();
                                        if ("Вес товара, г".equals(dtText)) {
                                            WebElement ddElement = dl.findElement(By.tagName("dd"));
                                            weight1 = ddElement.getText().trim();
                                            break;
                                        }
                                    } catch (Exception e) {
                                    }
                                }
                            }
                        } catch (Exception ignore) {
                        }

                        // 写入 Excel 时加同步锁保证线程安全
                        synchronized (outputWorkbook) {
                            Row outRow1 = finalOutputSheet.createRow(outputRowIndex.getAndIncrement());
                            outRow1.createCell(0).setCellValue(link);
                            outRow1.createCell(1).setCellValue(category);
                            outRow1.createCell(2).setCellValue(name);
                            outRow1.createCell(3).setCellValue(pic);
                            outRow1.createCell(4).setCellValue(calculationMethod);
                            outRow1.createCell(5).setCellValue(protein);
                            outRow1.createCell(6).setCellValue(fat);
                            outRow1.createCell(7).setCellValue(carb);
                            outRow1.createCell(8).setCellValue(kcal);
                            outRow1.createCell(9).setCellValue(desc);
                            outRow1.createCell(10).setCellValue(storageConditions);
                            outRow1.createCell(11).setCellValue(ingredients);
                            outRow1.createCell(12).setCellValue((weight == null || weight.isEmpty()) ? weight1 : weight);
                            for (int k = 0; k <= 12; k++) {
                                outRow1.getCell(k).setCellStyle(normalStyle);
                            }
                            // 有效记录该链接在 inputSheet 中的行号
                            processedRowIndexes.add(linkRowIndices.get(linkIndex));
                            System.out.println("✔️ 有效数据累计：" + processedRowIndexes.size() + "条.");
                            if (processedRowIndexes.size() % SAVE_THRESHOLD == 0 || linkIndex == links.size() - 1) {
                                // String outputPathNew = outputPath+"_"+ linkIndex+".xlsx";
                                try (FileOutputStream fos = new FileOutputStream(outputPath)) {
                                    outputWorkbook.write(fos);
                                    System.out.println("📁 已写入 " + processedRowIndexes.size() + " 条数据到" );
                                } catch (Exception e) {
                                    System.out.println("❌ 保存输出文件失败: " + e.getMessage());
                                }
                                // removeProcessedRows(inputSheet, processedRowIndexes);
                                // try (FileOutputStream categoryFos = new FileOutputStream(excelPath)) {
                                //     workbook.write(categoryFos);
                                //     System.out.println("🧹 已清理已爬取链接并保存回分类文件: " + sheetName);
                                // }
                                processedRowIndexes.clear();
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        if (driver != null) {
                            driver.close();
                        }
                        latch.countDown();
                    }
                });
            }
            // 等待该 sheet 所有任务执行完毕
            latch.await();
            executor.shutdown();


            // 手动触发一次保存操作，确保最后一个链接的数据被保存
            synchronized (outputWorkbook) {
                if (!processedRowIndexes.isEmpty()) {
                    try (FileOutputStream fos = new FileOutputStream(outputPath)) {
                        outputWorkbook.write(fos);
                        System.out.println("📁 已写入 " + processedRowIndexes.size() + " 条数据到 " + outputPath);
                    } catch (Exception e) {
                        System.out.println("❌ 保存输出文件失败: " + e.getMessage());
                    }
                    // removeProcessedRows(inputSheet, processedRowIndexes);
                    // try (FileOutputStream categoryFos = new FileOutputStream(excelPath)) {
                    //     workbook.write(categoryFos);
                    //     System.out.println("🧹 已清理已爬取链接并保存回分类文件: " + sheetName);
                    // }
                    processedRowIndexes.clear();
                }
            }
        }

        // 关闭所有资源
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
