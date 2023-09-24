package com.plugin.utils.bean;

import java.io.File;

public class ModuleType {
    private File model;
    private boolean isKts;

    public File getModel() {
        return model;
    }

    public void setModel(File model) {
        this.model = model;
    }

    public boolean isKts() {
        return isKts;
    }

    public void setKts(boolean kts) {
        isKts = kts;
    }

    public ModuleType(File model, boolean isKts) {
        this.model = model;
        this.isKts = isKts;
    }

    @Override
    public String toString() {
        return "ModuleType{" +
                "model=" + model +
                ", isKts=" + isKts +
                '}';
    }
}
