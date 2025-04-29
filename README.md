# 使用Jsoup+Selenium实现多线程商品数据抓取
## 一、项目背景
从海外超市官网抓取商品详细信息，包括商品名称、图片、分类、成分表、营养数据等。网站采用动态渲染技术，且存在反爬机制，因此选择Selenium+Jsoup组合方案实现数据抓取。
[GitHub链接](https://github.com/zhaigz/JavaCrawler)

---

## 二、技术选型
### 核心组件
- **Selenium 4.x**：用于模拟浏览器操作，解决动态渲染问题
- **Jsoup**：辅助HTML解析，快速提取DOM元素
- **Apache POI 5.x**：处理Excel文件读写
- **Java并发库**：实现多线程加速抓取

### 方案优势
- 多线程并发处理提升10倍+效率
- Headless模式节省资源
- 自动化Excel记录和断点续爬
- 完善的异常处理和日志记录

---

## 三、 环境配置

### 1. 依赖管理(Maven)
```xml
<dependencies>
    <!-- Selenium -->
    <dependency>
        <groupId>org.seleniumhq.selenium</groupId>
        <artifactId>selenium-java</artifactId>
        <version>4.14.1</version>
    </dependency>
    
    <!-- Apache POI -->
    <dependency>
        <groupId>org.apache.poi</groupId>
        <artifactId>poi-ooxml</artifactId>
        <version>5.2.3</version>
    </dependency>
    
    <!-- Jsoup -->
    <dependency>
        <groupId>org.jsoup</groupId>
        <artifactId>jsoup</artifactId>
        <version>1.16.1</version>
    </dependency>
</dependencies>
```
## 2、ChromeDriver配置
下载对应Chrome版本的驱动：[ChromeDriver官网](https://www.google.cn/intl/en_ca/chrome/dev/)
设置系统属性：
```java
System.setProperty("webdriver.chrome.driver", "your/chromedriver/path");
```
## 3、流程示意图：
![在这里插入图片描述](https://i-blog.csdnimg.cn/direct/df061bcb798545308efaa6b84f28f2d0.png)
## 4、关键模块实现
```java
1. 多线程任务分发
ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
CountDownLatch latch = new CountDownLatch(links.size());

for (String link : links) {
    executor.submit(() -> {
        WebDriver driver = new ChromeDriver(service, options);
        try {
            // 执行抓取逻辑...
        } finally {
            driver.close();
            latch.countDown();
        }
    });
}
latch.await();
2. 反爬绕过策略
ChromeOptions options = new ChromeOptions();
options.addArguments("--disable-blink-features=AutomationControlled");
options.setExperimentalOption("excludeSwitches", 
    new String[]{"enable-automation"});
options.setExperimentalOption("useAutomationExtension", false);

// 修改navigator.webdriver属性
((JavascriptExecutor)driver).executeScript(
    "Object.defineProperty(navigator, 'webdriver', {get: () => undefined})");
3. 数据解析示例
// 商品名称提取
WebElement nameElement = driver.findElement(
    By.xpath("//div[@class='vtex-flex-layout-0-x-flexRow']//h1//span"));
String name = nameElement.getText().trim();

// 营养成分表解析
List<WebElement> rows = driver.findElements(
    By.xpath("//table[@class='table table-hover']//tr"));
for (WebElement row : rows) {
    String key = row.findElement(By.tagName("th")).getText();
    String value = row.findElement(By.tagName("td")).getText();
    // 字段匹配逻辑...
}
```
## 完整代码如下：
```java
package Heb;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HebScraperMultiThread {
    // 每10个任务保存一次数据，可根据需要调整
    private static final int SAVE_THRESHOLD = 1000;
    // 线程池线程数，根据机器性能调整
    private static final int THREAD_COUNT = 8;

    public static void main(String[] args) throws IOException, InterruptedException {
        String excelPath = "D:\\WorkSpaces\\javaWorkspace\\JavaCrawler\\Heb\\heb.xlsx";
        String outputPath = "D:\\WorkSpaces\\javaWorkspace\\JavaCrawler\\Heb\\output\\heb-4-28-1.xlsx";
        String chromeDriverPath = "D:\\zgz\\HealthHope\\chromedriver-win64\\chromedriver.exe";
        System.setProperty("webdriver.chrome.driver", chromeDriverPath);

        // 配置 ChromeOptions（多线程时，每个任务自行创建 WebDriver 实例时均需使用此配置）
        ChromeOptions options = new ChromeOptions();
        // 全局关闭 Selenium 的日志
        Logger.getLogger("org.openqa.selenium").setLevel(Level.OFF);
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.addArguments("--start-maximized");
        options.addArguments("--headless"); // 或 "--headless=new"
        options.addArguments("--disable-gpu"); // 避免某些图形依赖失败
        options.addArguments("--no-sandbox"); // 某些服务器上必加
        options.addArguments("--disable-dev-shm-usage"); // 防止 /dev/shm 满
        options.addArguments("--window-size=1920,1080"); // 有些布局和屏幕大小有关
        options.addArguments("user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64)..."); // 模拟正常浏览器
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
                        "数据网站链接", "名称", "图片链接", "分类", "描述", "净含量", "存储警告", "原料", "已声明的过敏原", "营养益处",
                        "建议食用量", "每100克能量", "每100克总蛋白质", "每100克总脂肪", "每100克总碳水化合物", "每100克钠",
                        "每份能量", "每份总蛋白质", "每份总脂肪", "每份总碳水化合物", "每份钠含量"
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
                        System.out.println("▶ [" + sheetName + "] 正在抓取第 " + (linkIndex + 1) + " 条链接: ");


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

                        // 可根据实际情况调整等待时长
                        // Thread.sleep(3000);


                        try {
                            // 初始化各字段
                            String name = "", pic = "", category = "", desc = "", weight = "", storageConditions = "", ingredients = "", AlérgenosDeclarados = "", BeneficialNutrition = "";
                            String porciónSugerida = "", energy100 = "", protein100 = "", grasas100 = "", carbohydrate100 = "", sodio100 = "";
                            String energy = "", protein = "", grasas = "", carbohydrate = "", sodio = "";

                            // 名称抓取
                            try {
                                WebElement nameElement = driver.findElement(By.xpath("//div[@class='vtex-flex-layout-0-x-flexRow']//h1//span"));
                                name = nameElement.getText().trim();
                            } catch (Exception ignore) {
                            }

                            // 图片抓取
                            try {
                                List<String> imageUrls = new ArrayList<>();
                                WebElement gallery = driver.findElement(By.xpath("//div[@class='swiper-wrapper']"));
                                List<WebElement> imgElements = gallery.findElements(By.xpath(".//img"));
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


                            // 解析详细信息表格
                            try {
                                List<WebElement> productDetailsElements = driver.findElements(By.xpath("//div[@id='content-vtex']//table[@class='table table-hover']"));
                                if (productDetailsElements.size() == 0) {
                                    try {
                                        // 获取 description 内容
                                        WebElement tabWrapper = driver.findElement(By.cssSelector("div.tab_wrapper"));
                                        // 拿到里面的文字
                                        desc = tabWrapper.getText().trim();
                                    } catch (Exception ignore) {
                                    }
                                }
                                List<WebElement> rowsDesc = productDetailsElements.get(0).findElements(By.xpath(".//tr"));
                                for (WebElement row : rowsDesc) {
                                    WebElement th = row.findElement(By.xpath(".//th"));
                                    WebElement td = row.findElement(By.xpath(".//td"));
                                    String key = th.getText().trim();
                                    String value = td.getText().trim();
                                    if (key.contains("Tipo de producto")) {
                                        category = value;
                                    } else if (key.contains("Contenido neto")) {
                                        weight = value;
                                    } else if (key.contains("Advertencias de almacenamiento")) {
                                        storageConditions = value;
                                    } else if (key.contains("Ingredientes")) {
                                        ingredients = value;
                                    } else if (key.contains("Alérgenos Declarados")) {
                                        AlérgenosDeclarados = value;
                                    } else if (key.contains("Beneficios nutricionales")) {
                                        BeneficialNutrition = value;
                                    }
                                }

                                List<WebElement> rowsNutrition = productDetailsElements.get(1).findElements(By.xpath(".//tr"));
                                for (WebElement row : rowsNutrition) {
                                    WebElement th = row.findElement(By.xpath(".//th"));
                                    WebElement td = row.findElement(By.xpath(".//td"));
                                    String key = th.getText().trim();
                                    String value = td.getText().trim();
                                    if (key.contains("Porción")) {
                                        porciónSugerida = value;
                                    } else if (key.contains("Energía por 100 g")) {
                                        energy100 = value;
                                    } else if (key.contains("Proteínas totales por 100 g")) {
                                        protein100 = value;
                                    } else if (key.contains("Grasas totales por 100 g")) {
                                        grasas100 = value;
                                    } else if (key.contains("Carbohidratos totales por 100 g")) {
                                        carbohydrate100 = value;
                                    } else if (key.contains("Sodio por 100 g")) {
                                        sodio100 = value;
                                    } else if (key.contains("Energía por porción")) {
                                        energy = value;
                                    } else if (key.contains("Proteínas totales por porción")) {
                                        protein = value;
                                    } else if (key.contains("Grasas totales por porción")) {
                                        grasas = value;
                                    } else if (key.contains("Carbohidratos totales por porción")) {
                                        carbohydrate = value;
                                    } else if (key.contains("Sodio por porción")) {
                                        sodio = value;
                                    }
                                }
                            } catch (Exception ignore) {
                            }

                            // 写入 Excel 时加同步锁保证线程安全
                            synchronized (outputWorkbook) {
                                Row outRow1 = finalOutputSheet.createRow(outputRowIndex.getAndIncrement());
                                String[] values = {
                                        link, name, pic, category, desc, weight, storageConditions, ingredients, AlérgenosDeclarados,
                                        BeneficialNutrition, porciónSugerida, energy100, protein100, grasas100, carbohydrate100,
                                        sodio100, energy, protein, grasas, carbohydrate, sodio
                                };

                                for (int k = 0; k < values.length; k++) {
                                    Cell cell = outRow1.createCell(k);
                                    cell.setCellValue(values[k]);
                                    cell.setCellStyle(normalStyle);
                                }

                                // 有效记录该链接在 inputSheet 中的行号
                                processedRowIndexes.add(linkRowIndices.get(linkIndex));
                                System.out.println("✔️ 有效数据累计：" + processedRowIndexes.size() + "条.");
                                if (processedRowIndexes.size() % SAVE_THRESHOLD == 0 || linkIndex == links.size() - 1) {
                                    try (FileOutputStream fos = new FileOutputStream(outputPath)) {
                                        outputWorkbook.write(fos);
                                        System.out.println("📁 已写入 " + processedRowIndexes.size() + " 条数据到 " + outputPath);
                                    } catch (Exception e) {
                                        System.out.println("❌ 保存输出文件失败: " + e.getMessage());
                                    }
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

}
```
## 缺点
采用Driver的方式需要频繁后台访问页面，速度比较慢，而且准确率受网速影响很大。
另一种方式：通过访问url的方式拿到相应数据的JSON进行解析。
