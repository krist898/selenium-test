package com.ejercicio;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.nio.file.*;
import java.time.Duration;
import java.util.List;

/**
 * Navega a Google, busca "Documentación de selenium", toma capturas,
 * abre la documentación oficial y recorre los ítems del menú lateral.
 * Toda la sesión se graba en video con WebDriverManager (navegador en Docker).
 */
public class SeleniumDocumentationTest {

    private static final Path SCREENSHOTS = Paths.get("evidencias", "screenshots");
    private static final Path VIDEOS = Paths.get("evidencias", "videos");

    /** Ítems remarcados del menú lateral y su ruta en selenium.dev. */
    private static final String[][] MENU = {
            {"Overview", "/documentation/overview/"},
            {"WebDriver", "/documentation/webdriver/"},
            {"Selenium Manager", "/documentation/selenium_manager/"},
            {"Grid", "/documentation/grid/"},
            {"IE Driver Server", "/documentation/ie_driver_server/"},
            {"IDE", "/documentation/ide/"},
            {"Test Practices", "/documentation/test_practices/"},
            {"Legacy", "/documentation/legacy/"},
            {"About", "/documentation/about/"}
    };

    private WebDriverManager wdm;
    private WebDriver driver;
    private WebDriverWait wait;

    @BeforeClass
    public void setUp() throws Exception {
        Files.createDirectories(SCREENSHOTS);
        Files.createDirectories(VIDEOS);

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--lang=es", "--window-size=1600,900");

        wdm = WebDriverManager.chromedriver()
                .capabilities(options)
                .browserInDocker()
                .enableVnc()
                .enableRecording()
                .recordingOutput(VIDEOS.toAbsolutePath());
        driver = wdm.create();
        driver.manage().window().maximize();
        wait = new WebDriverWait(driver, Duration.ofSeconds(20));
    }

    @Test
    public void navegarDocumentacionSelenium() throws Exception {
        // 1. Página principal de Google
        driver.get("https://www.google.com");
        aceptarCookiesGoogle();

        // 2. Búsqueda textual
        WebElement q = wait.until(ExpectedConditions.elementToBeClickable(By.name("q")));
        q.sendKeys("Documentación de selenium");
        q.sendKeys(Keys.ENTER);
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("search")));

        // 3. Captura de los resultados
        screenshot("01_resultados_google");

        // 4. Seleccionar el enlace a la documentación de Selenium
        WebElement link = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//div[@id='search']//a[contains(@href,'selenium.dev/documentation')]")));
        link.click();
        wait.until(ExpectedConditions.urlContains("selenium.dev/documentation"));

        // 5. Captura de la documentación
        screenshot("02_documentacion_selenium");

        // 6. Navegar por cada ítem del menú lateral
        int i = 3;
        for (String[] item : MENU) {
            By locator = By.cssSelector("#td-sidebar-menu a[href$='" + item[1] + "']");
            WebElement menuLink = wait.until(ExpectedConditions.presenceOfElementLocated(locator));
            ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].scrollIntoView({block:'center'});", menuLink);
            try {
                menuLink.click();
            } catch (WebDriverException e) {
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", menuLink);
            }
            wait.until(ExpectedConditions.urlContains(item[1]));
            Thread.sleep(1500); // pausa para que se aprecie en el video
            screenshot(String.format("%02d_menu_%s", i++, item[0].replace(' ', '_')));
            System.out.println("Visitado: " + item[0] + " -> " + driver.getCurrentUrl());
            Assert.assertTrue(driver.getCurrentUrl().contains(item[1]));
        }
    }

    private void aceptarCookiesGoogle() {
        List<WebElement> botones = driver.findElements(By.xpath(
                "//button[.//div[contains(.,'Aceptar todo') or contains(.,'Accept all')]]"));
        if (!botones.isEmpty()) {
            botones.get(0).click();
        }
    }

    private void screenshot(String nombre) throws Exception {
        File src = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
        Path dest = SCREENSHOTS.resolve(nombre + ".png");
        Files.copy(src.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);
        System.out.println("Captura: " + dest.toAbsolutePath());
    }

    @AfterClass(alwaysRun = true)
    public void tearDown() {
        if (wdm != null) {
            wdm.quit(); // detiene el contenedor y guarda el video .mp4
        }
    }
}
