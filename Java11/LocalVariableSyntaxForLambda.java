import java.util.HashMap;

public class LocalVariableSyntaxForLambda {
 
    public static void main(String... args) {
        var namesWithAge = new HashMap<String, Integer>();
        namesWithAge.put("Raja", 28);
        namesWithAge.put("Achilleas", 110);
        namesWithAge.put("Hector", 105);

        namesWithAge.forEach((key, value) -> System.out.printf("%s -- %s\n", key, value));
        System.out.println("=================");
        namesWithAge.forEach((String key, Integer value) -> System.out.printf("%s -- %s\n", key, value));
        
        // 'var' cannot be mixed with non-var parametersJava(1073743336)
        //namesWithAge.forEach((var key, Integer value) -> System.out.printf("%s -- %s\n", key, value));
        
        // namesWithAge.forEach((var key, value) -> System.out.printf("%s -- %s\n", key, value));

    }
}