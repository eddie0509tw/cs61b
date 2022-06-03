package gitlet;

import java.util.*;
import java.io.Serializable;

public class Staging implements Serializable {

    private TreeMap<String, String> addedFiles;
    private ArrayList<String> removedFiles;

    public Staging() {
        addedFiles = new TreeMap<>();
        removedFiles = new ArrayList<>();
    }

    public void add(String fileName, String sha1) {
        addedFiles.put(fileName, sha1);
    }
    public void remove(String filename){
        addedFiles.remove(filename);
    }

    public void addToRemovedFiles(String fileName) {
        removedFiles.add(fileName);
    }

    public void clear() {
        addedFiles = new TreeMap<>();
        removedFiles = new ArrayList<>();
    }

    public TreeMap<String, String> getAddedFiles() {
        return addedFiles;
    }

    public ArrayList<String> getRemovedFiles() {
        return removedFiles;
    }
}
