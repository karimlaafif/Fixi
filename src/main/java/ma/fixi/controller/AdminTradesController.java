package ma.fixi.controller;

import java.util.List;
import java.util.Locale;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.NodeOrientation;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import ma.fixi.model.Trade;
import ma.fixi.service.Services;
import ma.fixi.util.Async;
import ma.fixi.util.Validators;
import ma.fixi.view.Page;
import ma.fixi.view.Route;
import ma.fixi.view.component.Dialogs;
import ma.fixi.view.component.FieldError;
import ma.fixi.view.component.Toast;
import ma.fixi.view.component.Ui;

/**
 * CRUD des métiers : tableau à gauche, formulaire à droite.
 * Sélectionner une ligne la charge dans le formulaire ; « Vider » repasse en mode ajout.
 */
public class AdminTradesController implements Page {

    @FXML private TextField filterField;
    @FXML private Label countLabel;
    @FXML private TableView<Trade> table;
    @FXML private TableColumn<Trade, String> nameFrColumn;
    @FXML private TableColumn<Trade, String> nameArColumn;
    @FXML private TableColumn<Trade, Boolean> activeColumn;
    @FXML private Label formTitle;
    @FXML private Label formHint;
    @FXML private TextField nameFrField;
    @FXML private TextField nameArField;
    @FXML private Label nameFrError;
    @FXML private Label nameArError;
    @FXML private CheckBox activeCheck;
    @FXML private Button saveButton;
    @FXML private Button deleteButton;

    private final ObservableList<Trade> trades = FXCollections.observableArrayList();
    private final FilteredList<Trade> filtered = new FilteredList<>(trades);
    private Trade editing;

    @FXML
    private void initialize() {
        nameFrColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getNameFr()));
        nameArColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getNameAr()));
        nameArColumn.setCellFactory(col -> {
            TableCell<Trade, String> cell = new TableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty ? null : item);
                }
            };
            cell.setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);
            return cell;
        });
        activeColumn.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().isActive()));
        activeColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Boolean active, boolean empty) {
                super.updateItem(active, empty);
                setText(null);
                setGraphic(empty || active == null ? null
                        : active ? Ui.badge("Actif", "badge-success") : Ui.badge("Inactif", "badge-neutral"));
            }
        });
        table.setItems(filtered);
        table.setPlaceholder(new Label("Aucun métier. Ajoutez-en un avec le formulaire."));
        table.getSelectionModel().selectedItemProperty().addListener((obs, old, now) -> {
            if (now != null) {
                edit(now);
            }
        });
        filterField.textProperty().addListener((o, a, b) -> applyFilter());
        clearForm();
    }

    @Override
    public void onShow(Route route, Object param) {
        reload(null);
    }

    private void reload(String selectId) {
        Async.run(() -> Services.trades().findAll(), list -> {
            trades.setAll(list);
            applyFilter();
            if (selectId != null) {
                trades.stream().filter(t -> t.getId().equals(selectId)).findFirst()
                        .ifPresent(t -> table.getSelectionModel().select(t));
            }
        }, e -> Toast.error(table, Async.userMessage(e)));
    }

    private void applyFilter() {
        String q = filterField.getText() == null ? "" : filterField.getText().trim().toLowerCase(Locale.ROOT);
        filtered.setPredicate(t -> q.isEmpty() || t.getNameFr().toLowerCase(Locale.ROOT).contains(q)
                || t.getNameAr().contains(q));
        long active = trades.stream().filter(Trade::isActive).count();
        countLabel.setText(trades.size() + " métiers · " + active + " actifs");
    }

    private void edit(Trade t) {
        editing = t;
        formTitle.setText("Modifier le métier");
        formHint.setText("Les changements sont visibles immédiatement par les clients.");
        nameFrField.setText(t.getNameFr());
        nameArField.setText(t.getNameAr());
        activeCheck.setSelected(t.isActive());
        saveButton.setText("Mettre à jour");
        saveButton.setGraphic(Ui.icon("fth-check", 15));
        deleteButton.setVisible(true);
        deleteButton.setManaged(true);
        clearErrors();
    }

    private void clearForm() {
        editing = null;
        table.getSelectionModel().clearSelection();
        formTitle.setText("Ajouter un métier");
        formHint.setText("Le métier apparaîtra dans la recherche et dans le formulaire de demande.");
        nameFrField.clear();
        nameArField.clear();
        activeCheck.setSelected(true);
        saveButton.setText("Ajouter");
        saveButton.setGraphic(Ui.icon("fth-plus", 15));
        deleteButton.setVisible(false);
        deleteButton.setManaged(false);
        clearErrors();
    }

    private void clearErrors() {
        FieldError.set(nameFrField, nameFrError, null);
        FieldError.set(nameArField, nameArError, null);
    }

    @FXML
    private void onSave() {
        boolean bad = FieldError.set(nameFrField, nameFrError, Validators.required(nameFrField.getText(), "Le nom en français"));
        bad |= FieldError.set(nameArField, nameArError, Validators.required(nameArField.getText(), "Le nom en arabe"));
        if (bad) {
            return;
        }
        Trade t = new Trade(editing == null ? null : editing.getId(), nameFrField.getText().trim(),
                nameArField.getText().trim(), activeCheck.isSelected());
        boolean creating = editing == null;
        saveButton.setDisable(true);
        Async.run(() -> creating ? Services.trades().create(t) : Services.trades().update(t), saved -> {
            saveButton.setDisable(false);
            Toast.success(table, creating ? "Métier « " + saved.getNameFr() + " » ajouté." : "Métier mis à jour.");
            if (creating) {
                clearForm();
                reload(null);
            } else {
                reload(saved.getId());
            }
        }, e -> {
            saveButton.setDisable(false);
            Toast.error(table, Async.userMessage(e));
        });
    }

    @FXML
    private void onDelete() {
        if (editing == null) {
            return;
        }
        Trade t = editing;
        Dialogs.confirm(table, "Supprimer « " + t.getNameFr() + " » ?",
                "Le métier sera retiré définitivement. S'il est déjà utilisé, désactivez-le plutôt.",
                "Supprimer", true, () -> Async.run(() -> Services.trades().delete(t.getId()), () -> {
                    Toast.success(table, "Métier supprimé.");
                    clearForm();
                    reload(null);
                }, e -> Toast.error(table, Async.userMessage(e))));
    }

    @FXML
    private void onClear() {
        clearForm();
    }

    /** Pour les tests : liste affichée. */
    List<Trade> visibleTrades() {
        return List.copyOf(filtered);
    }
}
