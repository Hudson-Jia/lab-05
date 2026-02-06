package com.example.lab5_starter;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class City implements Serializable {

    private String id;        // Firestore document id
    private String name;
    private String province;

    // Required empty constructor for Firestore / serialization
    public City() { }

    public City(String name, String province) {
        this.name = name;
        this.province = province;
    }

    public City(String id, String name, String province) {
        this.id = id;
        this.name = name;
        this.province = province;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getProvince() { return province; }
    public void setProvince(String province) { this.province = province; }

    public Map<String, Object> toMap() {
        Map<String, Object> m = new HashMap<>();
        m.put("name", name);
        m.put("province", province);
        return m;
    }
}