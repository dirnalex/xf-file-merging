package com.dirnalex.xf.filemerging;

import com.dirnalex.xf.filemerging.comparators.PriceLineComparator;
import com.dirnalex.xf.filemerging.comparators.ProductLineComparator;
import com.dirnalex.xf.filemerging.validators.ArgumentsValidator;
import com.dirnalex.xf.filemerging.validators.ArgumentsValidatorImpl;
import com.google.code.externalsorting.ExternalSort;

import java.io.*;
import java.util.Comparator;

/**
 * Class for merging two files into one that contains product information and all prices.
 * Uses 3rd party external sort library.
 */
public class FileMerger {
    public static final String CSV_SEPARATOR = ",";
    private static final String MERGE_RESULT_HEADER = "\"PRODUCT_ID\",\"PRODUCT_DESCRIPTION\",\"PRICE\"";

    private Comparator productLineComparator = new ProductLineComparator();
    private Comparator priceLineComparator = new PriceLineComparator();

    public static void main(String[] args) throws Exception {
        ArgumentsValidator argumentsValidator = new ArgumentsValidatorImpl();
        argumentsValidator.validate(args);

        File productsFile = new File(args[0]);
        File pricesFile = new File(args[1]);
        File mergeResultFile = new File(args[2]);

        FileMerger fileMerger = new FileMerger();
        fileMerger.mergeToFile(productsFile, pricesFile, mergeResultFile);
    }

    /**
     * Merges two input files into the mergeResultFile that will contain product id, description and its prices.
     * The external sort is used for both initial files and then the result is combined into one file.
     *
     * @param productsFile existing file of products with header "PRODUCT_ID","PRODUCT_DESCRIPTION"
     * @param pricesFile existing file of prices with header "PRODUCT_ID","DATE","PRICE"
     * @param mergeResultFile file, where the merge results will be written
     * @throws IOException thown in case of any problems with files
     */
    public void mergeToFile(File productsFile, File pricesFile, File mergeResultFile) throws IOException {
        File sortedProductsFile = sortFile(productsFile, productLineComparator, true);
        File sortedPricesFile = sortFile(pricesFile, priceLineComparator, false);

        mergeSorted(sortedProductsFile, sortedPricesFile, mergeResultFile);

        sortedProductsFile.delete();
        sortedPricesFile.delete();
    }

    protected File sortFile(File fileToSort, Comparator lineComparator, boolean removeDuplicates) throws IOException {
        File sortedFile = new File(fileToSort.getParent() + fileToSort.separatorChar + "sorted_" + fileToSort.getName());
        ExternalSort.mergeSortedFiles(
                ExternalSort.sortInBatch(
                        fileToSort,
                        lineComparator,
                        null,
                        removeDuplicates,
                        1
                ),
                sortedFile,
                lineComparator,
                removeDuplicates
        );
        return sortedFile;
    }

    protected void mergeSorted(File sortedProductsFile, File sortedPricesFile, File mergeResultFile) throws IOException {
        try (
                BufferedReader sortedProductsReader = new BufferedReader(new FileReader(sortedProductsFile));
                BufferedReader sortedPricesReader = new BufferedReader(new FileReader(sortedPricesFile));
                BufferedWriter mergeResultWriter = new BufferedWriter(new FileWriter(mergeResultFile))
        ) {
            String productLine;
            String priceLine;
            String[] priceLineArrBuf = null;
            //write header
            mergeResultWriter.write(MERGE_RESULT_HEADER);
            mergeResultWriter.newLine();
            //iterate through each product
            while ((productLine = sortedProductsReader.readLine()) != null) {
                String[] productLineArr = productLine.split(CSV_SEPARATOR);
                String productId = productLineArr[0];
                String productDesc = productLineArr[1];
                StringBuilder resultLineSb = new StringBuilder();
                resultLineSb.append(productId);
                resultLineSb.append(CSV_SEPARATOR);
                resultLineSb.append(productDesc);
                //check the saved price that didn't belong to the last product
                if (priceLineArrBuf != null) {
                    String productIdFromPrices = priceLineArrBuf[0];
                    String productPrice = priceLineArrBuf[2];
                    if (productIdFromPrices.equals(productId)) {
                        resultLineSb.append(CSV_SEPARATOR);
                        resultLineSb.append(productPrice);
                        priceLineArrBuf = null;
                    } else {
                        mergeResultWriter.write(resultLineSb.toString());
                        mergeResultWriter.newLine();
                        continue;
                    }
                }
                //continiously check prices for the current product
                //if the price doesn't correstpond to the current product - save it and move on to next product
                while ((priceLine = sortedPricesReader.readLine()) != null) {
                    String[] priceLineArr = priceLine.split(CSV_SEPARATOR);
                    String productIdFromPrices = priceLineArr[0];
                    String productPrice = priceLineArr[2];
                    if (productIdFromPrices.equals(productId)) {
                        resultLineSb.append(CSV_SEPARATOR);
                        resultLineSb.append(productPrice);
                    } else {
                        priceLineArrBuf = priceLineArr;
                        break;
                    }
                }
                mergeResultWriter.write(resultLineSb.toString());
                mergeResultWriter.newLine();
            }
        }
    }
}
