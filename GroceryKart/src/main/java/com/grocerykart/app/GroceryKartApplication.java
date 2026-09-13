package com.grocerykart.app;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public final class GroceryKartApplication extends Application {
  private MainView view;

  @Override
  public void start(Stage stage) {
    view = new MainView();
    var scene = new Scene(view.root(), 1100, 700);
    var stylesheet = getClass().getResource("/styles/app.css");
    if (stylesheet != null) {
      scene.getStylesheets().add(stylesheet.toExternalForm());
    }
    stage.setTitle("GroceryKart");
    stage.setScene(scene);
    stage.show();
  }

  @Override
  public void stop() {
    if (view != null) {
      view.close();
    }
  }

  public static void main(String[] args) {
    launch(args);
  }
}
