package ma.fixi.service.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import ma.fixi.model.City;
import ma.fixi.service.CityService;
import ma.fixi.service.ServiceException;
import ma.fixi.util.DBConnection;

/** Lecture des villes stockées dans la table MySQL {@code city}. */
public class JdbcCityService implements CityService {

    private static final String SELECT_ACTIVE =
            "SELECT id, name_fr, name_ar, latitude, longitude, is_active FROM city WHERE is_active = TRUE ORDER BY name_fr";

    @Override
    public List<City> findActive() {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT_ACTIVE);
             ResultSet rs = ps.executeQuery()) {
            List<City> list = new ArrayList<>();
            while (rs.next()) {
                City city = new City();
                city.setId(rs.getString("id"));
                city.setNameFr(rs.getString("name_fr"));
                city.setNameAr(rs.getString("name_ar"));
                city.setLatitude(rs.getBigDecimal("latitude"));
                city.setLongitude(rs.getBigDecimal("longitude"));
                city.setActive(rs.getBoolean("is_active"));
                list.add(city);
            }
            return list;
        } catch (SQLException e) {
            throw new ServiceException("Impossible de charger les villes. Vérifiez la connexion à la base.", e);
        }
    }
}
