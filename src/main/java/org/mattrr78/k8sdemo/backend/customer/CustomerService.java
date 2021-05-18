package org.mattrr78.k8sdemo.backend.customer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@Service
public class CustomerService {

    @Value("${k8sdemo.integration.base-url}")
    private String integrationBaseUrl;

    private static final Logger LOG = LoggerFactory.getLogger(CustomerService.class);

    private final CustomerJpaRepo repo;

    CustomerService(CustomerJpaRepo repo) {
        this.repo = repo;
    }

    public WeatherStatus findWeatherStatusByCustomerId(int customerId) {
        if (customerId <= 0) {
            throw new IllegalArgumentException("Invalid Customer id '" + customerId + "'.");
        }

        Optional<Customer> optionalCustomer = repo.findById(customerId);
        if (optionalCustomer.isEmpty()) {
            throw new IllegalArgumentException("Customer with id '" + customerId + "' not found.");
        }

        Customer customer = optionalCustomer.get();
        String city = customer.getCity();
        String state = customer.getState();
        String country = customer.getCountry();

        WeatherStatus weatherStatus = new WeatherStatus();
        weatherStatus.setCity(city);
        weatherStatus.setState(state);
        weatherStatus.setCountry(country);

        Map weatherData = fetchWeatherData(city, state, country);
        if (!weatherData.isEmpty()) {
            weatherStatus.setDescription(weatherData.get("description").toString());
            weatherStatus.setTemperature(new BigDecimal(weatherData.get("temperature").toString()));
            weatherStatus.setHumidity(new BigDecimal(weatherData.get("humidity").toString()));
        }

        return weatherStatus;
    }

    private Map fetchWeatherData(String city, String state, String country)  {
        String url = integrationBaseUrl + "/weather?city=" + city + "&state=" + state + "&country=" + country;
        try {
            return new RestTemplate().getForObject(url, Map.class);
        } catch (RestClientException e)  {
            LOG.error("Cannot connect to " + integrationBaseUrl);
            return Collections.emptyMap();
        }
    }

}
