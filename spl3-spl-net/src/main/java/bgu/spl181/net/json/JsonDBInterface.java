package bgu.spl181.net.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JsonDBInterface {


    synchronized public static <T> List<T> getList(String listName, String filePath, Type t) throws IOException {
        Gson gson = new Gson();

        FileReader fileReader = new FileReader(filePath);
        Map<String, List<T>> jMap = gson.fromJson(fileReader, t);
        fileReader.close();
        return jMap.get(listName);
    }

    synchronized public static <T> void writeList(String listName, List<T> list, String filePath) throws IOException {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        Map<String, List<T>> mapToWrite = new HashMap<>();
        mapToWrite.put(listName, list);

        FileWriter fileWriter = new FileWriter(filePath);
        gson.toJson(mapToWrite, fileWriter);
        fileWriter.close();

    }

    synchronized public static <T> List<T> getListFromBuffer(List<T> buf, String listName, String filePath, Type t) {
        try {

            // if read first time (probably) - save a buffer list
            if (buf == null) {
                buf = JsonDBInterface.getList(listName, filePath, t);

            }

            return buf;

        } catch (IOException e) {
            return null;
        }
    }
}
