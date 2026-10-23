package com.saksham.store;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DataStore {
    private final Map<String,String> map=new ConcurrentHashMap<>();
    public void set(String key,String value){map.put(key,value);}
    public String get(String key){return map.get(key);}
    public boolean delete(String key){return map.remove(key)!=null;}
    public boolean exists(String key){return map.containsKey(key);}
}