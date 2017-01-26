package com.dirnalex.xf.filemerging.comparators;

import java.util.Comparator;

import static com.dirnalex.xf.filemerging.FileMerger.CSV_SEPARATOR;

/**
 * Comparator that compares strings by product ID that is located before the first comma in the string.
 */
public class PriceLineComparator implements Comparator<String> {
    @Override
    public int compare(String o1, String o2) {
        return o1.split(CSV_SEPARATOR)[0].compareTo(o2.split(CSV_SEPARATOR)[0]);
    }
}
