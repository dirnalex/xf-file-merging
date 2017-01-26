package com.dirnalex.xf.filemerging.validators;

import java.io.File;

/**
 * Created by dyrnaale on 26. 1. 2017.
 */
public class ArgumentsValidatorImpl implements ArgumentsValidator {

    @Override
    public void validate(String[] args) throws IllegalArgumentException {
        validateLength(args, 3);
        validateFileExist(args[0]);
        validateFileExist(args[1]);
    }

    private void validateLength(String[] args, int length) {
        if (args.length != length) {
            throw new IllegalArgumentException("Wrong amount of arguments. There should be " + length + " arguments.");
        }
    }

    private void validateFileExist(String path) {
        File file = new File(path);
        if (!file.exists() || !file.isFile()) {
            throw new IllegalArgumentException(file.getAbsolutePath() + " doesn't represent an existing file.");
        }
    }
}
