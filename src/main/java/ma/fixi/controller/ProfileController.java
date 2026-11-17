package ma.fixi.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import ma.fixi.model.ClientType;
import ma.fixi.model.Role;
import ma.fixi.model.User;
import ma.fixi.service.Services;
import ma.fixi.util.Async;
import ma.fixi.util.Validators;
import ma.fixi.view.Navigator;
import ma.fixi.view.Page;
import ma.fixi.view.Route;
import ma.fixi.view.Session;
import ma.fixi.view.component.FieldError;
import ma.fixi.view.component.Toast;
import ma.fixi.view.component.Ui;

/** Mon profil : modification du nom, de l'e-mail, du téléphone et du type de client. */
public class ProfileController implements Page {

    @FXML private StackPane avatarBox;
    @FXML private Label nameTitle;
    @FXML private Label roleLabel;
    @FXML private VBox clientTypeBox;
    @FXML private ToggleGroup clientTypeGroup;
    @FXML private ToggleButton particulierToggle;
    @FXML private ToggleButton syndicToggle;
    @FXML private TextField nameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private Label nameError;
    @FXML private Label emailError;
    @FXML private Label phoneError;
    @FXML private Button saveButton;

    @FXML
    private void initialize() {
        clientTypeGroup.selectedToggleProperty().addListener((obs, old, now) -> {
            if (now == null && old != null) clientTypeGroup.selectToggle(old);
        });
    }

    @Override
    public void onShow(Route route, Object param) {
        fill(Session.user());
    }

    private void fill(User u) {
        avatarBox.getChildren().setAll(Ui.avatar(u.getFullName(), 72, false));
        nameTitle.setText(u.getFullName());
        roleLabel.setText(u.getPrimaryRole().getLabel() + (u.getClientType() != null
                ? " · " + u.getClientType().getLabel() : "") + " · " + u.getEmail());
        boolean client = u.getPrimaryRole() == Role.CLIENT;
        clientTypeBox.setVisible(client);
        clientTypeBox.setManaged(client);
        (u.getClientType() == ClientType.SYNDIC ? syndicToggle : particulierToggle).setSelected(true);
        nameField.setText(u.getFullName());
        emailField.setText(u.getEmail());
        phoneField.setText(u.getPhone());
        FieldError.set(nameField, nameError, null);
        FieldError.set(emailField, emailError, null);
        FieldError.set(phoneField, phoneError, null);
    }

    @FXML
    private void onSave() {
        int errors = 0;
        if (FieldError.set(nameField, nameError, Validators.required(nameField.getText(), "Le nom"))) errors++;
        if (FieldError.set(emailField, emailError, Validators.email(emailField.getText()))) errors++;
        if (FieldError.set(phoneField, phoneError, Validators.optionalMoroccanPhone(phoneField.getText()))) errors++;
        if (errors > 0) {
            return;
        }
        User current = Session.user();
        User updated = new User();
        updated.setId(current.getId());
        updated.setRoles(current.getRoles());
        updated.setAccountStatus(current.getAccountStatus());
        updated.setFullName(nameField.getText().trim());
        updated.setEmail(emailField.getText().trim().toLowerCase());
        String phone = phoneField.getText();
        updated.setPhone(phone == null || phone.isBlank() ? null : Validators.normalizePhone(phone));
        if (current.getPrimaryRole() == Role.CLIENT) {
            updated.setClientType(syndicToggle.isSelected() ? ClientType.SYNDIC : ClientType.PARTICULIER);
        }
        saveButton.setDisable(true);
        Async.run(() -> Services.auth().updateProfile(updated), saved -> {
            saveButton.setDisable(false);
            Session.start(saved);
            Navigator.refreshUser();
            fill(saved);
            Toast.success(saveButton, "Profil enregistré.");
        }, e -> {
            saveButton.setDisable(false);
            Toast.error(saveButton, Async.userMessage(e));
        });
    }

    @FXML
    private void onReset() {
        fill(Session.user());
    }
}
