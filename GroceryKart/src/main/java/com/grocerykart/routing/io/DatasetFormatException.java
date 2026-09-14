package com.grocerykart.routing.io;

import java.io.IOException;

public class DatasetFormatException extends IOException {
  public DatasetFormatException(String file, int row, int column, String message) {
    super(file + ":" + row + ":" + column + ": " + message);
  }
}
