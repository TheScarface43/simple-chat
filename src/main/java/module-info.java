module com.example.simplechat {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires commons.validator;

    opens com.example.simplechat to javafx.fxml;
    exports com.example.simplechat;
}