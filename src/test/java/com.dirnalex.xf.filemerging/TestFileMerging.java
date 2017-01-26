package com.dirnalex.xf.filemerging;

import javafx.util.Pair;
import org.junit.*;
import org.junit.rules.TemporaryFolder;

import java.io.*;
import java.util.*;

/**
 * Test general behavior of merging. Temporary files are used.
 */
public class TestFileMerging {
    private static final String PRODUCTS_HEADER = "\"PRODUCT_ID\",\"PRODUCT_DESCRIPTION\"";
    private static final String PRICES_HEADER = "\"PRODUCT_ID\",\"DATE\",\"PRICE\"";
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();
    private FileMerger fileMerger;
    private File productsFile;
    private File pricesFile;
    private File mergeResultFile;
    private BufferedWriter productsFileWriter;
    private BufferedWriter pricesFileWriter;

    @Before
    public void init() throws IOException {
        fileMerger = new FileMerger();

        productsFile = temporaryFolder.newFile("products.csv");
        productsFileWriter = new BufferedWriter(new FileWriter(productsFile));
        productsFileWriter.write(PRODUCTS_HEADER);
        productsFileWriter.newLine();

        pricesFile = temporaryFolder.newFile("prices_by_date.csv");
        pricesFileWriter = new BufferedWriter(new FileWriter(pricesFile));
        pricesFileWriter.write(PRICES_HEADER);
        pricesFileWriter.newLine();

        mergeResultFile = temporaryFolder.newFile("all_prices.csv");
    }

    @Test
    public void testNormalBehavior() throws IOException {
        Map<String, Pair<String, List<String>>> productsPrices = new HashMap<>();
        productsPrices.put("\"1\"", new Pair<String, List<String>>("\"Product1\"", Arrays.asList(new String[]{"\"1.1\"", "\"1.2\""})));
        productsPrices.put("\"2\"", new Pair<String, List<String>>("\"Product2\"", Arrays.asList(new String[]{"\"2.1\"", "\"2.2\"", "\"2.3\""})));
        productsPrices.put("\"3\"", new Pair<String, List<String>>("\"Product3\"", Arrays.asList(new String[]{"\"3.1\"", "\"3.2\""})));

        writeShuffledProducts(productsPrices, productsFileWriter);
        productsFileWriter.close();

        writeShuffledPrices(productsPrices, pricesFileWriter);
        pricesFileWriter.close();

        fileMerger.mergeToFile(productsFile, pricesFile, mergeResultFile);

        checkResultFile(productsPrices, mergeResultFile);
    }

    @Test(expected = FileNotFoundException.class)
    public void testProductsFileNotFound() throws IOException {
        productsFileWriter.close();
        productsFile.delete();

        fileMerger.mergeToFile(productsFile, pricesFile, mergeResultFile);
    }

    @Test(expected = FileNotFoundException.class)
    public void testPricesFileNotFound() throws IOException {
        pricesFileWriter.close();
        pricesFile.delete();

        fileMerger.mergeToFile(productsFile, pricesFile, mergeResultFile);
    }

    @Test
    public void testPricesEmpty() throws IOException {
        Map<String, Pair<String, List<String>>> productsPrices = new HashMap<>();
        productsPrices.put("\"1\"", new Pair<String, List<String>>("\"Product1\"", Arrays.asList(new String[]{})));
        productsPrices.put("\"2\"", new Pair<String, List<String>>("\"Product2\"", Arrays.asList(new String[]{})));
        productsPrices.put("\"3\"", new Pair<String, List<String>>("\"Product3\"", Arrays.asList(new String[]{})));

        writeShuffledProducts(productsPrices, productsFileWriter);
        productsFileWriter.close();

        pricesFileWriter.close();

        fileMerger.mergeToFile(productsFile, pricesFile, mergeResultFile);

        checkResultFile(productsPrices, mergeResultFile);
    }

    @Test
    public void testProductsEmpty() throws IOException {
        Map<String, Pair<String, List<String>>> productsPrices = new HashMap<>();
        productsPrices.put("\"1\"", new Pair<String, List<String>>("\"Product1\"", Arrays.asList(new String[]{"\"1.1\"", "\"1.2\""})));
        productsPrices.put("\"2\"", new Pair<String, List<String>>("\"Product2\"", Arrays.asList(new String[]{"\"2.1\"", "\"2.2\"", "\"2.3\""})));
        productsPrices.put("\"3\"", new Pair<String, List<String>>("\"Product3\"", Arrays.asList(new String[]{"\"3.1\"", "\"3.2\""})));

        productsFileWriter.close();

        writeShuffledPrices(productsPrices, pricesFileWriter);
        pricesFileWriter.close();

        fileMerger.mergeToFile(productsFile, pricesFile, mergeResultFile);

        checkResultFile(new HashMap<>(), mergeResultFile);
    }

    private void writeShuffledProducts(Map<String, Pair<String, List<String>>> productsPrices, BufferedWriter productsFileWriter) throws IOException {
        List<String> products = new ArrayList<>();
        for (String productID : productsPrices.keySet()) {
            products.add(productID + "," + productsPrices.get(productID).getKey());
        }
        Collections.shuffle(products);
        for (String productLine : products) {
            productsFileWriter.write(productLine);
            productsFileWriter.newLine();
        }
    }

    private void writeShuffledPrices(Map<String, Pair<String, List<String>>> productsPrices, BufferedWriter pricesFileWriter) throws IOException {
        List<String> prices = new ArrayList<>();
        for (String productID : productsPrices.keySet()) {
            for (String price : productsPrices.get(productID).getValue()) {
                prices.add(productID + ",\"2000-01-01\"," + price);
            }
        }
        Collections.shuffle(prices);
        for (String priceLine : prices) {
            pricesFileWriter.write(priceLine);
            pricesFileWriter.newLine();
        }
    }

    private void checkResultFile(Map<String, Pair<String, List<String>>> productsPrices, File mergeResultFile) throws IOException {
        try (BufferedReader mergeResultsReader = new BufferedReader(new FileReader(mergeResultFile))) {
            String resultLine;
            int resultSize = 0;
            mergeResultsReader.readLine(); //skip the header
            while ((resultLine = mergeResultsReader.readLine()) != null) {
                String[] resultLineArr = resultLine.split(FileMerger.CSV_SEPARATOR, 3);
                Assert.assertTrue(resultLineArr.length == 2 || resultLineArr.length == 3);

                String productID = resultLineArr[0];
                Assert.assertTrue(productsPrices.get(productID) != null);

                String productDescription = resultLineArr[1];
                Assert.assertEquals(productsPrices.get(productID).getKey(), productDescription);

                String[] prices = resultLineArr.length == 3 ?
                        resultLineArr[2].split(FileMerger.CSV_SEPARATOR) :
                        new String[]{};
                Arrays.sort(prices);
                String[] expectedPrices = (String[]) productsPrices.get(productID).getValue().toArray();
                Arrays.sort(expectedPrices);
                Assert.assertArrayEquals(expectedPrices, prices);

                resultSize++;
            }
            Assert.assertEquals(productsPrices.size(), resultSize);
        }
    }

    @After
    public void close() {
        try {
            productsFileWriter.close();
        } catch (IOException e) {
        }
        try {
            pricesFileWriter.close();
        } catch (IOException e) {
        }
    }
}
