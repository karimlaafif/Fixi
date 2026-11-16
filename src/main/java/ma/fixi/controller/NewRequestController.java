package ma.fixi.controller;

import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import ma.fixi.dto.NewRequestForm;
import ma.fixi.model.Artisan;
import ma.fixi.model.City;
import ma.fixi.model.Trade;
import ma.fixi.model.Urgency;
import ma.fixi.service.Services;
import ma.fixi.util.Async;
import ma.fixi.util.Validators;
import ma.fixi.view.Navigator;
import ma.fixi.view.Page;
import ma.fixi.view.Route;
import ma.fixi.view.Session;
import ma.fixi.view.component.Combos;
import ma.fixi.view.component.FieldError;
import ma.fixi.view.component.Toast;
import ma.fixi.view.component.Ui;

/** Formulaire « Nouvelle demande ». Valide les champs puis appelle {@code RequestService.create}. */
public class NewRequestController implements Page {

    private static final int MAX_PHOTOS = 5;
    private static final long MAX_PHOTO_BYTES = 5L * 1024 * 1024;

    @FXML private HBox targetBox;
    @FXML private Label targetLabel;
    @FXML private TextField titleField;
    @FXML private ComboBox<Trade> tradeCombo;
    @FXML private TextArea descriptionArea;
    @FXML private Label descriptionCounter;
    @FXML private ComboBox<City> cityCombo;
    @FXML private TextField addressField;
    @FXML private DatePicker datePicker;
    @FXML private TextField budgetField;
    @FXML private ToggleGroup urgencyGroup;
    @FXML private ToggleButton urgentToggle;
    @FXML private FlowPane photosPane;
    @FXML private Button submitButton;
    @FXML private Label titleError;
    @FXML private Label tradeError;
    @FXML private Label descriptionError;
    @FXML private Label cityError;
    @FXML private Label dateError;
    @FXML private Label budgetError;
    @FXML private Label photoError;

    private final List<File> photos = new ArrayList<>();
    private Artisan target;

    @FXML
    private void initialize() {
        Combos.labels(tradeCombo, Trade::getNameFr, null);
        Combos.labels(cityCombo, City::getNameFr, null);
        datePicker.setValue(LocalDate.now().plusDays(1));
        datePicker.setDayCellFactory(dp -> new DateCell() {
            @Override
            public void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                setDisable(empty || item.isBefore(LocalDate.now()));
            }
        });
        descriptionArea.textProperty().addListener((o, a, b) -> updateCounter());
        urgencyGroup.selectedToggleProperty().addListener((obs, old, now) -> {
            if (now == null) urgencyGroup.selectToggle(old);
        });
        updateCounter();
    }

    @Override
    public void onShow(Route route, Object param) {
        if (param instanceof Artisan a) {
            target = a;
            targetLabel.setText("Cette demande sera envoyée uniquement à " + a.getDisplayName()
                    + " (" + a.getTrade().getNameFr() + ", " + a.getCity().getNameFr() + ").");
            targetBox.setVisible(true);
            targetBox.setManaged(true);
        }
        Async.run(() -> new Object[]{Services.trades().findActive(), Services.cities().findActive()}, lists -> {
            @SuppressWarnings("unchecked") List<Trade> trades = (List<Trade>) lists[0];
            @SuppressWarnings("unchecked") List<City> cities = (List<City>) lists[1];
            tradeCombo.getItems().setAll(trades);
            cityCombo.getItems().setAll(cities);
            if (target != null) {
                tradeCombo.setValue(target.getTrade());
                cityCombo.setValue(target.getCity());
            }
        }, e -> Toast.error(titleField, Async.userMessage(e)));
        javafx.application.Platform.runLater(titleField::requestFocus);
    }

    private void updateCounter() {
        int length = descriptionArea.getText() == null ? 0 : descriptionArea.getText().length();
        descriptionCounter.setText(length + " / " + Validators.MAX_DESCRIPTION_LENGTH);
        descriptionCounter.getStyleClass().remove("text-danger");
        if (length > Validators.MAX_DESCRIPTION_LENGTH) {
            descriptionCounter.getStyleClass().add("text-danger");
        }
    }

    @FXML
    private void onAddPhotos() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Ajouter des photos");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));
        List<File> chosen = chooser.showOpenMultipleDialog(Navigator.stage());
        if (chosen == null) {
            return;
        }
        String error = null;
        for (File f : chosen) {
            if (photos.size() >= MAX_PHOTOS) {
                error = "5 photos maximum.";
                break;
            }
            if (f.length() > MAX_PHOTO_BYTES) {
                error = f.getName() + " dépasse 5 Mo.";
                continue;
            }
            if (!photos.contains(f)) {
                photos.add(f);
            }
        }
        FieldError.set(null, photoError, error);
        renderPhotos();
    }

    private void renderPhotos() {
        photosPane.getChildren().clear();
        for (File f : photos) {
            ImageView view = new ImageView(new Image(f.toURI().toString(), 64, 64, false, true, true));
            view.setFitWidth(64);
            view.setFitHeight(64);
            Rectangle clip = new Rectangle(64, 64);
            clip.setArcWidth(16);
            clip.setArcHeight(16);
            view.setClip(clip);
            Button remove = new Button(null, Ui.icon("fth-x", 11));
            remove.getStyleClass().add("photo-remove");
            remove.setOnAction(e -> {
                photos.remove(f);
                renderPhotos();
            });
            StackPane thumb = new StackPane(view, remove);
            StackPane.setAlignment(remove, Pos.TOP_RIGHT);
            thumb.getStyleClass().add("photo-thumb");
            thumb.setMaxSize(64, 64);
            photosPane.getChildren().add(thumb);
        }
    }

    @FXML
    private void onSubmit() {
        int errors = 0;
        if (FieldError.set(titleField, titleError, Validators.required(titleField.getText(), "Le titre"))) errors++;
        if (FieldError.set(tradeCombo, tradeError, tradeCombo.getValue() == null ? "Choisissez un métier." : null)) errors++;
        String description = descriptionArea.getText();
        String descError = Validators.required(description, "La description");
        if (descError == null && description.length() > Validators.MAX_DESCRIPTION_LENGTH) {
            descError = "La description dépasse " + Validators.MAX_DESCRIPTION_LENGTH + " caractères.";
        }
        if (FieldError.set(descriptionArea, descriptionError, descError)) errors++;
        if (FieldError.set(cityCombo, cityError, cityCombo.getValue() == null ? "Choisissez une ville." : null)) errors++;
        LocalDate date = datePicker.getValue();
        String dateMsg = date == null ? "Choisissez une date." : date.isBefore(LocalDate.now())
                ? "Choisissez une date à partir d'aujourd'hui." : null;
        if (FieldError.set(datePicker, dateError, dateMsg)) errors++;
        if (FieldError.set(budgetField, budgetError, Validators.optionalAmount(budgetField.getText()))) errors++;
        if (errors > 0) {
            Toast.error(submitButton, errors == 1 ? "Un champ est à corriger." : errors + " champs sont à corriger.");
            return;
        }

        NewRequestForm form = new NewRequestForm(titleField.getText().trim(), tradeCombo.getValue(), description.trim(),
                cityCombo.getValue(), blankToNull(addressField.getText()), Validators.parseAmount(budgetField.getText()),
                date, urgentToggle.isSelected() ? Urgency.URGENT : Urgency.NORMAL, List.copyOf(photos),
                target == null ? null : target.getId());

        submitButton.setDisable(true);
        submitButton.setText("Envoi…");
        String clientId = Session.user().getId();
        Async.run(() -> Services.requests().create(clientId, form), created -> {
            Navigator.goTo(Route.REQUESTS, created.getId());
            Toast.success(Navigator.stage().getScene().getRoot(), "Demande " + created.getReference() + " envoyée.");
        }, e -> {
            submitButton.setDisable(false);
            submitButton.setText("Envoyer la demande");
            Toast.error(submitButton, Async.userMessage(e));
        });
    }

    @FXML
    private void onCancel() {
        Navigator.back();
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
