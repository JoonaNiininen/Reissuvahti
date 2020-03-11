package com.example.reissuvahti.overpass;

public class OverpassResponse {
    private String version;
    private OverpassLocation[] elements;

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public OverpassLocation[] getElements() {
        return elements;
    }

    public void setElements(OverpassLocation[] elements) {
        this.elements = elements;
    }


}
