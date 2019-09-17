package bgu.spl181.net.json;

import bgu.spl181.net.dataholders.GenericUser;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class GenericUserDBInterface {

    public static final String filePath = "Database/example_GenericUsers.json";
    private static final String listName = "users";
    private static final Type t = new TypeToken<Map<String, List<GenericUser>>>() {}.getType();

    synchronized public static List<GenericUser> getList() {
        try {
            return JsonDBInterface.<GenericUser>getList(listName, filePath, t);
        } catch (IOException e) {
            return null;
        }

        // todo rethink exception
    }

    synchronized public static void writeList(List<GenericUser> userList) {
        try {
            JsonDBInterface.<GenericUser>writeList(listName, userList, filePath);
        } catch (IOException e) {
        }
        // todo rethink exception
    }

    synchronized public static boolean userExists(String username) {
        for (GenericUser gu : getList()) {
            if (gu.getUsername().equals(username)) {
                return true;
            }
        }

        return false;
    }

    synchronized public static void addUser(String name, String pw) {
        List<GenericUser> l = getList();
        l.add(new GenericUser(name, pw));
        writeList(l);
    }

    synchronized public static boolean userPWexists(String username, String pw) {
        for (GenericUser gu : getList()) {
            if (gu.getUsername().equals(username) &&gu.getPassword().equals(pw) ) {
                return true;
            }
        }

        return false;
    }
}
