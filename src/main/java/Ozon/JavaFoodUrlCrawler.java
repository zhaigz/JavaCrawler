package Ozon;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class JavaFoodUrlCrawler {

    private static final ExecutorService executor = Executors.newFixedThreadPool(5);

    public static void main(String[] args)
            throws IOException, InterruptedException, URISyntaxException, ExecutionException {
        String url = "https://www.ozon.ru/category/konditerskie-izdeliya-9378/";
        String preString = "https://ir.ozone.ru/s3/";
        String endString = ".jpg";
        String outPutPath = "images/ozon/";
        String webDriverPath = "D:\\zgz\\HealthHope\\chromedriver-win64\\chromedriver.exe";

        System.setProperty("webdriver.chrome.driver", webDriverPath);
        ChromeOptions options = new ChromeOptions();

        // 添加反检测参数
        options.addArguments("user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36");
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.addArguments("--start-maximized");
        options.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});
        options.setExperimentalOption("useAutomationExtension", false);
        // 不使用无头模式：保持界面开启

        WebDriver driver = new ChromeDriver(options);

        try {
            // JS注入：去除 navigator.webdriver = true
            ((JavascriptExecutor) driver).executeScript(
                    "Object.defineProperty(navigator, 'webdriver', {get: () => undefined})"
            );

            System.out.println("获取url");
            driver.get(url);

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));


            // System.out.println("滚动完成后等待页面资源稳定...");
            Set<String> productDetailsUrls = new LinkedHashSet<>();
            JavascriptExecutor js = (JavascriptExecutor) driver;

            long lastHeight = (long) js.executeScript("return document.body.scrollHeight");
            int tryCount = 0;
            // 最大尝试次数 超过则认为页面不在加载新的食品
            int maxTries = 10;

            while (tryCount < maxTries) {
                // 抓取当前页面商品链接
                // List<WebElement> productsNow = driver.findElements(By.cssSelector("div.ju7_25 div.nj7_25.j8n_25.tile-root"));
                List<WebElement> productsNow = driver.findElements(By.cssSelector("div.u7j_25 div.j8n_25.jn9_25.tile-root"));

                for (WebElement product : productsNow) {
                    try {
                        WebElement link = product.findElement(By.cssSelector("a[href]"));
                        String href = link.getAttribute("href");
                        if (!href.startsWith("http")) {
                            href = "https://www.ozon.ru" + href;
                        }
                        productDetailsUrls.add(href);
                    } catch (NoSuchElementException e) {
                        // 跳过无效项
                    }
                }

                System.out.println("当前已收集商品链接数: " + productDetailsUrls.size());

                // 向下滚动
                js.executeScript("window.scrollTo(0, document.body.scrollHeight);");
                Thread.sleep(3000 + (int) (Math.random() * 1000));

                long newHeight = (long) js.executeScript("return document.body.scrollHeight");

                if (newHeight == lastHeight) {
                    tryCount++;
                } else {
                    tryCount = 0;  // 有新高度就重置
                    lastHeight = newHeight;
                }
            }


            // 加载完成后再获取 HTML
            Thread.sleep(5000);  // 可调节
            System.out.println("全部收集完成，共获取商品链接: " + productDetailsUrls.size());
            // for (String productDetailsUrl : productDetailsUrls) {
            //     System.out.println(productDetailsUrl);
            // }
            // 创建 Excel 表格
            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("OzonProductLinks");

            // 表头
            // Row header = sheet.createRow(0);
            // header.createCell(0).setCellValue("商品链接");

            int rowIndex = 0;
            for (String productDetailsUrl : productDetailsUrls) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(productDetailsUrl);
            }

            // 保存到本地 Excel 文件
            String excelOutputPath = "ozon_product_links_id4.xlsx";
            try (FileOutputStream fos = new FileOutputStream(excelOutputPath)) {
                workbook.write(fos);
            }
            workbook.close();
            System.out.println("✅ 商品链接已保存到 Excel：" + excelOutputPath);


            executor.shutdown();
            boolean finishedInTime = executor.awaitTermination(5, TimeUnit.MINUTES);
            if (!finishedInTime) {
                System.err.println("在规定时间内，有些任务未完成。");
            }
        } finally {
            driver.quit();
        }
    }
}
