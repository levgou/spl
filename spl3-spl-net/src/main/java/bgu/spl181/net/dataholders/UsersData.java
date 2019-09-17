package bgu.spl181.net.dataholders;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UsersData {

    private Map<Integer, Boolean> loginStatuses;
    private Map<String, Boolean> userStatuses;
    private Map<Integer, String> clientUserMap;


    public UsersData() {
        loginStatuses = new ConcurrentHashMap<>();
        userStatuses = new ConcurrentHashMap<>();
        clientUserMap = new ConcurrentHashMap<>();
    }

    synchronized public Boolean isClientLogged(Integer id) {
        return loginStatuses.containsKey(id) && loginStatuses.get(id);
    }

    synchronized public Boolean containsClient(Integer id){
        return loginStatuses.containsKey(id);
    }

    synchronized public void login(Integer id, String user){
        loginStatuses.put(id,Boolean.TRUE);
        clientUserMap.put(id, user);
        userStatuses.put(user, Boolean.TRUE);
    }

    synchronized public void logout(Integer id){
        loginStatuses.remove(id);
        userStatuses.remove(clientUserMap.remove(id));
        clientUserMap.remove(id);
    }

    synchronized public void addClient(Integer id){
        loginStatuses.put(id, Boolean.FALSE);
    }

    synchronized public boolean userLogged(String username) {
        return userStatuses.containsKey(username) && userStatuses.get(username);
    }

    synchronized public String getUserOfClient(int connectionId) {
        return clientUserMap.get(connectionId);
    }

    synchronized public Map<Integer, Boolean> getLoginStatuses() {
        return loginStatuses;
    }
}
