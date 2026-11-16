package ma.fixi.service;

import java.util.List;
import ma.fixi.model.City;

/** Lecture des villes. Table : {@code city}. */
public interface CityService {

    /** Villes actives, triées par nom français. */
    List<City> findActive();
}
