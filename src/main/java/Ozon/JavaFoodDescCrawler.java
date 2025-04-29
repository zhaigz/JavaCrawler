package Ozon;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openqa.selenium.*;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.FileOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.*;
// 单个网址的数据爬取
public class JavaFoodDescCrawler {
    public static void main(String[] args) throws IOException {
        String baseUrl = "https://www.ozon.ru/product/lavash-vkusno-i-prosto-armyanskiy-tonkiy-listovoy-360-g-148481540/?at=PjtJzBywXcP25kq8ilElMo3HxE99rBCyqDpjJcPKXpzY&ectx=1&miniapp=supermarket";  // 这里是你想抓取的单个商品链接
        String excelPath = "ozon_food_links.xlsx";
        String chromeDriverPath = "D:\\zgz\\HealthHope\\chromedriver-win64\\chromedriver.exe";  // 请确保路径正确

        System.setProperty("webdriver.chrome.driver", chromeDriverPath);

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.addArguments("--start-maximized");
        options.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});
        options.setExperimentalOption("useAutomationExtension", false);

        WebDriver driver = new ChromeDriver(options);
        JavascriptExecutor js = (JavascriptExecutor) driver;

        try {
            // JS注入：去除 navigator.webdriver = true
            ((JavascriptExecutor) driver).executeScript("Object.defineProperty(navigator, 'webdriver', {get: () => undefined})");

            // 打开baseUrl商品页面
            driver.get(baseUrl);
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

            // 创建 Excel 表格
            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("FoodLinks");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("链接");
            header.createCell(1).setCellValue("分类");

            // 访问 URL 并获取分类信息
            System.out.println("正在处理商品链接: " + baseUrl);
            WebDriver detailDriver = new ChromeDriver(options);
            try {
                ((JavascriptExecutor) detailDriver).executeScript("Object.defineProperty(navigator, 'webdriver', {get: () => undefined})");
                detailDriver.get(baseUrl);
                Thread.sleep(3000);  // 等待页面加载

                // 分类
                String category = "";
                // 食物名称
                String name = "";
                // 食物图片
                String pic = "";
                // 描述
                String desc = "";
                // 营养素
                // String nutrient = "";
                // 每100克营养成分
                String nutrition100 = "";
                // 存储条件
                String storageConditions = "";
                // 配料表
                String ingredients = "";
                // 净含量
                String weight = "";
                // 保质期
                // String period = "";
                try {
                    // 获取分类的元素并提取文本
                    // 分类
                    // WebElement categoryElement = detailDriver.findElement(By.cssSelector("div.ms3_28 div.m3s_28.r9m_28 div.ms_28.s0m_28 span.m1s_28.tsBody400Small"));
                    // // // WebElement categoryElement = detailDriver.findElement(By.xpath("//div[contains(@class, 'm3s_28')]//div[contains(@class, 's0m_28')]//span[contains(@class, 'tsBody400Small')]"));
                    // category = categoryElement.getText().trim();
                    // System.out.println("分类："+category);
                    // 名称
                    // WebElement nameElement = detailDriver.findElement(By.cssSelector("div.m9m_28 h1.m8m_28.tsHeadline550Medium"));
                    // name = nameElement.getText().trim();
                    WebElement nameElement = detailDriver.findElement(By.xpath("//div[@data-widget='webProductHeading']//h1"));
                    name = nameElement.getText().trim();
                    System.out.println("名称："+name);

                    // 获取字段名
                    List<String> productInfo = new ArrayList<>();
                    // ---------------关于产品
                    // 定位所有属性块容器
                    WebElement productElement = detailDriver.findElement(By.xpath("//div[@data-widget='webShortCharacteristics']"));
                    // 获取包含所有字段的mt8_28容器
                    WebElement detailsContainer = productElement.findElement(By.xpath(".//div[contains(@class, 'mt8_28')]"));

                    // 获取所有字段块（每个m8t_28对应一个字段）
                    List<WebElement> detailBlocks = detailsContainer.findElements(By.xpath("./div[contains(@class, 'm8t_28')]"));

                    Map<String, String> productDetails = new LinkedHashMap<>();

                    for (WebElement block : detailBlocks) {
                        try {
                            // 提取字段名称
                            WebElement labelElement = block.findElement(By.xpath(".//span[contains(@class, 'tsBodyM')]"));
                            String label = labelElement.getText().trim().replace("，%", ""); // 处理中文标点

                            // 提取字段值
                            WebElement valueContainer = block.findElement(By.xpath(".//div[contains(@class, 't5m_28')]"));
                            String value = "";

                            // 处理不同值类型（链接或纯文本）
                            List<WebElement> links = valueContainer.findElements(By.tagName("a"));
                            if (!links.isEmpty()) {
                                // 处理多个链接的情况（如原产国）
                                List<String> values = new ArrayList<>();
                                for (WebElement link : links) {
                                    values.add(link.getText().trim());
                                }
                                value = String.join(", ", values);
                            } else {
                                // 处理纯文本情况
                                WebElement valueElement = valueContainer.findElement(By.xpath(".//span[contains(@class, 'tsBody400Small')] | .//span[contains(@class, 'm6t_28')]"));
                                value = valueElement.getText().trim();
                            }

                            productDetails.put(label, value);
                        } catch (Exception e) {
                            System.out.println("Error extracting block: " + e.getMessage());
                        }
                    }

                    // 打印结果
                    productDetails.forEach((k, v) -> System.out.println(k + ": " + v));
                    category = productDetails.getOrDefault("Тип", "");
                    weight = productDetails.getOrDefault("Вес товара, г", "");
                    // 提取所有的图片链接
                    // 定位图片库容器
                    WebElement gallery = wait.until(ExpectedConditions.presenceOfElementLocated(
                            By.xpath("//div[@data-widget='webGallery']")
                    ));

                    //---------------- 提取所有图片元素（包含延迟加载处理）
                    List<String> imageUrls = new ArrayList<>();
                    List<WebElement> imgElements = wait.until(d ->
                            gallery.findElements(By.xpath(".//img[contains(@class, 'k8m_28') or contains(@class, 'km0_28')]"))
                    );

                    // 提取有效图片链接
                    for (WebElement img : imgElements) {
                        // 处理srcset属性（提取最高分辨率图片）
                        String srcset = img.getAttribute("srcset");
                        if (srcset != null && !srcset.isEmpty()) {
                            // 示例格式："url1 2x, url2 3x"
                            String[] sources = srcset.split(", ");
                            if (sources.length > 0) {
                                // 取最后一个最高分辨率
                                String highResUrl = sources[sources.length-1].split(" ")[0];
                                imageUrls.add(highResUrl);
                            }
                        }

                        // 处理普通src属性
                        String src = img.getAttribute("src");
                        if (src != null && !src.isEmpty() && !imageUrls.contains(src)) {
                            imageUrls.add(src);
                        }
                    }

                    // ---------------每100克营养成分
                    // 定位营养信息外层容器
                    WebElement nutritionElement = driver.findElement(By.xpath("//div[@data-widget='webNutritionInfo']"));

                    // 提取主标题
                    String title = nutritionElement.findElement(By.xpath(".//div[contains(@class, 'wl7_28')]")).getText().trim();

                    // 定位数据项容器
                    WebElement dataContainer = nutritionElement.findElement(By.xpath(".//div[contains(@class, 'lw8_28')]"));

                    // 提取所有数据项块
                    List<WebElement> dataItems = dataContainer.findElements(By.xpath("./div[contains(@class, 'l8w_28')]"));

                    List<String> nutritionDetails = new ArrayList<>();

                    for (WebElement item : dataItems) {
                        try {
                            // 提取数值部分
                            WebElement valueElement = item.findElement(By.xpath(".//div[contains(@class, 'wl8_28')]"));
                            String value = valueElement.getText().trim();

                            // 提取名称部分
                            WebElement nameElement1 = item.findElement(By.xpath(".//div[contains(@class, 'w8l_28')]"));
                            String name1 = nameElement1.getText().trim();

                            nutritionDetails.add(value + name1);
                        } catch (Exception e) {
                            System.out.println("提取营养项失败: " + e.getMessage());
                        }
                    }

                    // 构建最终结果
                    String detailsString = String.join(", ", nutritionDetails);
                    Map<String, String> result = new LinkedHashMap<>();
                    result.put(title, detailsString);

                    // 输出示例：每100克产品的营养价值：10蛋白质, 3脂肪, 70碳水化合物, 350千卡
                    result.forEach((k, v) -> System.out.println(k + v));
                    nutrition100 = result.getOrDefault("Пищевая ценность продукта на 100 г:","");
                    //--------------------------------描述
                    // 正确获取描述内容
                    WebElement descriptionElement = driver.findElement(By.xpath("//div[@data-widget='webPdpGrid']"));
                    List<WebElement> webDescription = descriptionElement.findElements(By.xpath("//div[@data-widget='webDescription']"));
                    WebElement desc1Element = webDescription.get(0); // 索引0是描述部分
                    // 定位描述正文容器
                    WebElement contentElement = desc1Element.findElement(
                            By.xpath(".//div[@class='l1m_28']/div[@class='RA-a1']")
                    );
                    desc = contentElement.getText().trim();
                    System.out.println("描述内容：" + desc); // 输出目标描述

                    // 如果需要处理储存条件和成分（假设从第二个元素获取）：
                    WebElement storageElement = webDescription.get(1);
                    WebElement storageContentElement = storageElement.findElement(
                            By.xpath(".//div[@class='l1m_28']")
                    );
                    // 获取所有 h3（标题）和 p（内容）
                    List<WebElement> titles = storageContentElement.findElements(By.tagName("h3"));
                    List<WebElement> paragraphs = storageContentElement.findElements(By.tagName("p"));

                    Map<String, String> descriptionMap = new LinkedHashMap<>();
                    for (int i = 0; i < Math.min(titles.size(), paragraphs.size()); i++) {
                        String title11 = titles.get(i).getText().trim();
                        String paragraph = paragraphs.get(i).getText().trim();
                        descriptionMap.put(title11, paragraph);
                    }

                    // 拿到储存条件和化合物
                    storageConditions = descriptionMap.getOrDefault("Условия хранения", "");
                    ingredients = descriptionMap.getOrDefault("Состав", "");

                    System.out.println("储存条件：" + storageConditions);
                    System.out.println("化合物：" + ingredients);

                } catch (NoSuchElementException e) {
                    System.err.println("未找到分类信息: " + baseUrl);
                }

                // 将链接和分类写入 Excel
                // 创建标题行
                Row headerRow = sheet.createRow(0);
                String[] headers = {"数据网站链接", "分类", "名称", "描述", "营养成分（每100克）", "储存条件", "配料表", "净含量（克）"};
                for (int i = 0; i < headers.length; i++) {
                    headerRow.createCell(i).setCellValue(headers[i]);
                }
                // 创建数据行
                Row row = sheet.createRow(1);
                row.createCell(0).setCellValue(baseUrl);           // 商品链接
                row.createCell(1).setCellValue(category);          // 分类
                row.createCell(2).setCellValue(name);              // 名称
                row.createCell(3).setCellValue(desc);              // 描述
                row.createCell(4).setCellValue(nutrition100);      // 每100克营养成分
                row.createCell(5).setCellValue(storageConditions); // 储存条件
                row.createCell(6).setCellValue(ingredients);       // 配料表
                row.createCell(7).setCellValue(weight);            // 净含量


            } catch (Exception e) {
                System.err.println("处理失败: " + baseUrl);
            } finally {
                detailDriver.quit();
            }

            // 将 Excel 数据保存到文件
            try (FileOutputStream fos = new FileOutputStream(excelPath)) {
                workbook.write(fos);
            }
            workbook.close();
            System.out.println("✅ Excel 已保存到: " + excelPath);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            driver.quit();
        }
    }
}
