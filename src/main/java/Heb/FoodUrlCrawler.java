package Heb;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class FoodUrlCrawler {

    private static final ExecutorService executor = Executors.newFixedThreadPool(5);

    public static void main(String[] args)
            throws IOException, InterruptedException, URISyntaxException, ExecutionException {
        String url = "https://www.heb.com.mx/saborizantes-para-leche?map=c";
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

            // System.out.println("获取url");
            driver.get(url);

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));

            Set<String> productDetailsUrls = new LinkedHashSet<>();
            JavascriptExecutor js = (JavascriptExecutor) driver;

            long lastHeight = (long) js.executeScript("return document.body.scrollHeight");
            int tryCount = 0;
            int maxTries = 5;

            while (tryCount < maxTries) {
                // 抓取当前页面商品链接
                for (WebElement product : driver.findElements(By.className("vtex-search-result-3-x-galleryItem"))) {
                    WebElement linkElement = product.findElement(By.cssSelector("a.vtex-product-summary-2-x-clearLink"));
                    String productLink = linkElement.getAttribute("href");
                    productDetailsUrls.add(productLink);
                }

                System.out.println("当前已收集商品链接数: " + productDetailsUrls.size());

                // 查找并滚动到标题元素
                // try {
                //     WebElement titleElement = driver.findElement(By.xpath("//div[@class='vtex-rich-text-0-x-wrapper vtex-rich-text-0-x-wrapper--search-seo-title']/p[@class='lh-copy vtex-rich-text-0-x-paragraph vtex-rich-text-0-x-paragraph--search-seo-title']"));
                //     js.executeScript("arguments[0].scrollIntoView(true);", titleElement);
                //     Thread.sleep(2000 + (int) (Math.random() * 1000)); // 等待滚动完成
                // } catch (NoSuchElementException e) {
                //     System.out.println("标题元素未找到");
                // }

                // 查找并点击“Mostrar más”按钮
                try {
                    // 增加等待时间，确保页面完全加载
                    WebDriverWait buttonWait = new WebDriverWait(driver, Duration.ofSeconds(10));
                    List<WebElement> showMoreButtons = driver.findElements(By.xpath("//button[contains(@class, 'vtex-button') and .//div[contains(text(), 'Mostrar más')]]"));

                    if (!showMoreButtons.isEmpty()) {
                        WebElement showMoreButton = showMoreButtons.get(0);
                        if (showMoreButton.isDisplayed()) {
                            // 滚动到按钮位置
                            js.executeScript("arguments[0].scrollIntoView(true);", showMoreButton);
                            Thread.sleep(1000); // 等待滚动完成
                            showMoreButton.click();
                            Thread.sleep(3000 + (int) (Math.random() * 1000)); // 等待页面加载
                        } else {
                            System.out.println("按钮存在但不可见");
                        }
                    } else {
                        System.out.println("按钮不存在");
                    }
                } catch (NoSuchElementException e) {
                    System.out.println("按钮未找到");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                long newHeight = (long) js.executeScript("return document.body.scrollHeight");

                if (newHeight == lastHeight) {
                    tryCount++;
                } else {
                    tryCount = 0;
                    lastHeight = newHeight;
                }
            }

            Thread.sleep(3000);  // 可调节
            System.out.println("全部收集完成，共获取商品链接: " + productDetailsUrls.size());

            // 创建 Excel 表格
            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("OzonProductLinks");

            int rowIndex = 0;
            for (String productDetailsUrl : productDetailsUrls) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(productDetailsUrl);
            }

            // 保存到本地 Excel 文件
            String excelOutputPath = "D:\\WorkSpaces\\javaWorkspace\\JavaCrawler\\Heb\\Heb_Urls_id29.xlsx";
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
