package serialization;

import com.google.gson.Gson;
import serialization.dto.Configuration;

public class Json {

    private static Gson gson = new Gson();

    public static <T> String getJsonString(final T dto) {
        return gson.toJson(dto);
    }

    public static <T> T fromJson(final String jsonString, Class<T> objectClass) {
        return gson.fromJson(jsonString, objectClass);
    }
}
