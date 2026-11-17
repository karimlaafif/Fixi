package ma.fixi.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import ma.fixi.config.ServiceConfig;
import ma.fixi.service.Services;
import ma.fixi.service.mock.MockStore;
import ma.fixi.util.Async;
import ma.fixi.util.Validators;
import ma.fixi.view.Navigator;
import ma.fixi.view.component.PasswordInput;

/** Écran de connexion. Appelle {@code AuthService.login}. */
public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordInput passwordField;
    @FXML private Button loginButton;
    @FXML private HBox errorBox;
    @FXML private Label errorLabel;
    @FXML private VBox demoBox;

    @FXML
    private void initialize() {
        boolean demo = ServiceConfig.isDemoMode();
        demoBox.setVisible(demo);
        demoBox.setManaged(demo);
        emailField.textProperty().addListener((o, a, b) -> clearError());
        passwordField.textProperty().addListener((o, a, b) -> clearError());
        javafx.application.Platform.runLater(emailField::requestFocus);
    }

    @FXML
    private void onLogin() {
        String email = emailField.getText() == null ? "" : emailField.getText().trim();
        String password = passwordField.getText();
        String error = Validators.email(email);
        if (error == null && (password == null || password.isEmpty())) {
            error = "Saisissez votre mot de passe.";
        }
        if (error != null) {
            showError(error);
            return;
        }
        setBusy(true);
        Async.run(() -> Services.auth().login(email, password),
                Navigator::showMain,
                e -> {
                    setBusy(false);
                    showError(Async.userMessage(e));
                    passwordField.setText("");
                    passwordField.activeField().requestFocus();
                });
    }

    @FXML
    private void onRegister() {
        Navigator.showRegister();
    }

    @FXML
    private void fillClient() {
        fill("client@fixi.ma");
    }

    @FXML
    private void fillArtisan() {
        fill("artisan@fixi.ma");
    }

    @FXML
    private void fillAdmin() {
        fill("admin@fixi.ma");
    }

    private void fill(String email) {
        emailField.setText(email);
        passwordField.setText(MockStore.DEMO_PASSWORD);
        loginButton.requestFocus();
    }

    private void setBusy(boolean busy) {
        loginButton.setDisable(busy);
        loginButton.setText(busy ? "Connexion…" : "Se connecter");
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorBox.setVisible(true);
        errorBox.setManaged(true);
    }

    private void clearError() {
        errorBox.setVisible(false);
        errorBox.setManaged(false);
    }
}
