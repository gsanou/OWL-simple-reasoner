package cs3019Test;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class TestUtils {
    private final static String basePath = "owl_test_files/approved/description-logic";

    static String[] getConsistentFiles(){
        File[] listOfFiles = getFileList(basePath);

        List<String> inconsistentOntologies =
                Arrays.stream(listOfFiles).map(File::getName).filter(f -> f.contains("consistent") && !f.contains("in") && f.contains("rdf")).collect(Collectors.toList());
        String[] res = new String[inconsistentOntologies.size()];
        for (int i = 0; i < inconsistentOntologies.size(); i++) {
            res[i] = basePath + "/" + inconsistentOntologies.get(i);
        }
        return res;
    }

    static String[] getInconsistentFiles(){
        File folder = new File(basePath);
        File[] listOfFiles = folder.listFiles();
        List<File> inconsistentOntologies = Arrays.stream(listOfFiles).filter(f -> f.getName().contains("inconsistent") && f.getName().contains("rdf")).collect(Collectors.toList());
        String[] res = new String[inconsistentOntologies.size()];
        for (int i = 0; i < inconsistentOntologies.size(); i++) {
            res[i] = basePath + "/" + inconsistentOntologies.get(i).getName();
        }
        return res;
    }

    static File[] getFileList(String path){
        return new File(path).listFiles();
    }
}
