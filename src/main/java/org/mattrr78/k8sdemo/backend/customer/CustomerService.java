package org.mattrr78.k8sdemo.backend.customer;

import org.redisson.Redisson;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.client.RedisConnectionException;
import org.redisson.config.Config;
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
import java.util.concurrent.TimeUnit;

@Service
public class CustomerService {

    @Value("${k8sdemo.integration.base-url}")
    private String integrationBaseUrl;

    @Value("${k8sdemo.redis.url}")
    private String redisUrl;

    private static final Logger LOG = LoggerFactory.getLogger(CustomerService.class);

    private final CustomerJpaRepo repo;

    private RedissonClient redisson;

    CustomerService(CustomerJpaRepo repo) {
        this.repo = repo;
    }

    public WeatherStatus findWeatherStatusByCustomerId(int customerId) {
        if (customerId <= 0) {
            throw new IllegalArgumentException("Invalid Customer id '" + customerId + "'.");
        }

        if (redisson == null)  {
            Config config = new Config();
            config.useSingleServer().setAddress(redisUrl);
            try {
                redisson = Redisson.create(config);
            } catch (RedisConnectionException e)  {
                LOG.error(redisUrl + " not running");
            }
        }

        RBucket<WeatherStatus> bucket = null;
        WeatherStatus weatherStatus = null;
        if (redisson != null) {
            try {
                bucket = redisson.getBucket("weather-status-" + customerId);
                weatherStatus = bucket.get();
            } catch (RedisConnectionException e)  {
                LOG.error(redisUrl + " not running");
                redisson = null;
                bucket = null;
            }
        }

        if (weatherStatus != null)  {
            LOG.info("Weather status fetched from Redis");
            return weatherStatus;
        }

        LOG.info(customerId + " weather status not cached in Redis");
        weatherStatus = new WeatherStatus();

        Optional<Customer> optionalCustomer = repo.findById(customerId);
        if (!optionalCustomer.isPresent()) {
            throw new IllegalArgumentException("Customer with id '" + customerId + "' not found.");
        }

        Customer customer = optionalCustomer.get();
        String city = customer.getCity();
        String state = customer.getState();
        String country = customer.getCountry();

        weatherStatus.setCity(city);
        weatherStatus.setState(state);
        weatherStatus.setCountry(country);

        Map weatherData = fetchWeatherData(city, state, country);
        if (!weatherData.isEmpty()) {
            weatherStatus.setDescription(weatherData.get("description").toString());
            weatherStatus.setTemperature(new BigDecimal(weatherData.get("temperature").toString()));
            weatherStatus.setHumidity(new BigDecimal(weatherData.get("humidity").toString()));
        }

        if (bucket != null) {
            bucket.set(weatherStatus, 5, TimeUnit.MINUTES);
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
