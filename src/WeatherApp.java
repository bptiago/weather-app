import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

public class WeatherApp {
    public static JSONObject getWeatherData(String locationName) {
        JSONArray locationData = getLocationData(locationName);
        // Exact location is the first item in JSONArray
        JSONObject location = (JSONObject) locationData.get(0);
        double latitude = (double) location.get("latitude");
        double longitude = (double) location.get("longitude");

        String urlString = "https://api.open-meteo.com/v1/forecast?" +
                "latitude=" + latitude + "&longitude=" + longitude +
                "&hourly=temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m&timezone=America%2FSao_Paulo";

        try {
            HttpURLConnection conn = fetchApiResponse(urlString);

            if(conn.getResponseCode() != 200) {

                System.out.println("Error: Could not connect to API");
                return null;
            } else {

                StringBuilder resultJson = new StringBuilder();

                // Scanner will read Json data
                Scanner scanner = new Scanner(conn.getInputStream());
                while(scanner.hasNext()) {
                    // Reads line and stores in resultJson
                    resultJson.append(scanner.nextLine());
                }

                scanner.close();

                conn.disconnect();

                JSONParser parser = new JSONParser();
                JSONObject resultJsonObj = (JSONObject) parser.parse(String.valueOf(resultJson));

                // Hourly key contains temperature, windspeed and relative humidity per hour
                JSONObject hourly = (JSONObject) resultJsonObj.get("hourly");

                JSONArray time = (JSONArray) hourly.get("time");
                int index = getIndexOfCurrentTime(time);

                JSONArray temperatureData = (JSONArray) hourly.get("temperature_2m");
                double currentTemperature = (double) temperatureData.get(index); // In celsius

                JSONArray humidityData = (JSONArray) hourly.get("relative_humidity_2m");
                long currentHumidity = (long) humidityData.get(index); // In percentage

                JSONArray windSpeedData = (JSONArray) hourly.get("wind_speed_10m");
                double currentWindSpeed = (double) windSpeedData.get(index); // In km/h

                JSONArray weatherCode = (JSONArray) hourly.get("weather_code");
                String weatherCondition = convertWeatherCode((long) weatherCode.get(index));

                // Compile refined data into a JSON object
                JSONObject refinedWeatherData = new JSONObject();
                refinedWeatherData.put("weather_condition", weatherCondition);
                refinedWeatherData.put("temperature", currentTemperature);
                refinedWeatherData.put("humidity", currentHumidity);
                refinedWeatherData.put("windspeed", currentWindSpeed);

                return refinedWeatherData;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public static JSONArray getLocationData(String locationName) {
        locationName = locationName.replaceAll(" ", "+");

        String urlString = "https://geocoding-api.open-meteo.com/v1/search?name="
                + locationName + "&count=10&language=en&format=json";

        try {
            HttpURLConnection conn = fetchApiResponse(urlString);

            // Response code: 200 (works), 400(bad request), 500(error)
            if(conn.getResponseCode() != 200) {

                System.out.println("Error: Could not connect to API");
                return null;
            } else {

                StringBuilder resultJson = new StringBuilder();

                // Scanner will read Json data
                Scanner scanner = new Scanner(conn.getInputStream());
                while(scanner.hasNext()) {
                    // Reads line and stores in resultJson
                    resultJson.append(scanner.nextLine());
                }

                scanner.close();

                conn.disconnect();

                JSONParser parser = new JSONParser();
                JSONObject resultJsonObj = (JSONObject) parser.parse(String.valueOf(resultJson));
                // Get only values within key "results"
                return (JSONArray) resultJsonObj.get("results");
            }


        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private static HttpURLConnection fetchApiResponse(String urlString) {
        try {
            // Try to create connection
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("GET");
            conn.connect();
            return conn;
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Could not make connection!");
        return null;
    }

    private static int getIndexOfCurrentTime(JSONArray timeArray) {
        String currentTime = getCurrentTime();

        for (int i = 0; i < timeArray.size(); i++) {
            String time = (String) timeArray.get(i);
            if (time.equalsIgnoreCase(currentTime)) {
                return i;
            }
        }

        return 0;
    }

    private static String getCurrentTime() {
        LocalDateTime currentDateTime = LocalDateTime.now();

        // API format is 2024-01-17T03:00
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH':00'");
        String formattedDateTime = currentDateTime.format(formatter);

        return formattedDateTime;
    }

    private static String convertWeatherCode(long weatherCode) {
        String weatherCondition = "";

        if (weatherCode == 0L) {
            weatherCondition = "Clear";
        } else if (weatherCode <= 3L) {
            weatherCondition = "Cloudy";
        } else if ((weatherCode >= 51L && weatherCode <= 67L) || (weatherCode >= 80L && weatherCode <= 99L)){
            weatherCondition = "Rainy";
        } else if (weatherCode >= 71L && weatherCode <= 77L) {
            weatherCondition = "Snow";
        }

        return weatherCondition;
    }
}