package ma.fixi.service.mock;

import java.util.Comparator;
import java.util.List;
import ma.fixi.model.City;
import ma.fixi.service.CityService;

/** Lecture fictive des villes. */
public class MockCityService implements CityService {

    private final MockStore store;

    public MockCityService(MockStore store) {
        this.store = store;
    }

    @Override
    public synchronized List<City> findActive() {
        return store.cities.stream()
                .filter(City::isActive)
                .sorted(Comparator.comparing(City::getNameFr))
                .toList();
    }
}
