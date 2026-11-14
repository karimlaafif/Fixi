package ma.fixi.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import ma.fixi.dto.RegistrationForm;
import ma.fixi.model.ClientType;
import ma.fixi.model.Role;
import ma.fixi.service.Services;
import ma.fixi.util.Async;
import ma.fixi.util.Validators;
import ma.fixi.view.Navigator;
import ma.fixi.view.component.FieldError;
import ma.fixi.view.component.PasswordInput;

/** Écran « Créer un compte ». Valide le formulaire puis appelle {@code AuthService.register}. */
public class RegisterController {

    @FXML private ToggleGroup accountType;
    @FXML private ToggleButton particulierToggle;
    @FXML private ToggleButton syndicToggle;
    @FXML private ToggleButton artisanToggle;
    @FXML private TextField nameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private PasswordInput passwordField;
    @FXML private PasswordInput confirmField;
    @FXML private Label nameError;
    @FXML private Label emailError;
    @FXML private Label phoneError;
    @FXML private Label passwordError;
    @FXML private Label confirmError;
    @FXML private HBox errorBox;
    @FXML private Label errorLabel;
    @FXML private Button submitButton;

    @FXML
    private void initialize() {
        // Toujours un type sélectionné : un clic sur le bouton actif ne le désélectionne pas.
        accountType.selectedToggleProperty().addListener((obs, old, now) -> {
            if (now == null) {
                accountType.selectToggle(old);
            }
        });
        javafx.application.Platform.runLater(nameField::requestFocus);
    }

    @FXML
    private void onSubmit() {
        hideFormError();
        int errors = 0;
        if (FieldError.set(nameField, nameError, Validators.required(nameField.getText(), "Le nom"))) errors++;
        if (FieldError.set(emailField, emailError, Validators.email(emailField.getText()))) errors++;
        if (FieldError.set(phoneField, phoneError, Validators.optionalMoroccanPhone(phoneField.getText()))) errors++;
        if (FieldError.set(passwordField, passwordError, Validators.password(passwordField.getText()))) errors++;
        if (FieldError.set(confirmField, confirmError,
                Validators.passwordConfirmation(passwordField.getText(), confirmField.getText()))) errors++;
        if (errors > 0) {
            return;
        }

        Role role = artisanToggle.isSelected() ? Role.ARTISAN : Role.CLIENT;
        ClientType clientType = role == Role.ARTISAN ? null
                : syndicToggle.isSelected() ? ClientType.SYNDIC : ClientType.PARTICULIER;
        String phone = phoneField.getText() == null || phoneField.getText().isBlank()
                ? null : Validators.normalizePhone(phoneField.getText());
        RegistrationForm form = new RegistrationForm(nameField.getText().trim(), emailField.getText().trim(), phone,
                passwordField.getText(), role, clientType);

        submitButton.setDisable(true);
        submitButton.setText("Création du compte…");
        Async.run(() -> Services.auth().register(form),
                Navigator::showMain,
                e -> {
                    submitButton.setDisable(false);
                    submitButton.setText("Créer mon compte");
                    errorLabel.setText(Async.userMessage(e));
                    errorBox.setVisible(true);
                    errorBox.setManaged(true);
                });
    }

    @FXML
    private void onBackToLogin() {
        Navigator.showLogin();
    }

    private void hideFormError() {
        errorBox.setVisible(false);
        errorBox.setManaged(false);
    }
}
