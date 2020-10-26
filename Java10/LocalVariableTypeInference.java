import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.function.Function;

public class LocalVariableTypeInference {

    // var name = "Achilleas"; /* 'var' is not allowed hereJava(1073743335) */

    public static void main(String... args) throws IOException {
        var list = new ArrayList<String>();  // infers ArrayList<String>
        var stream = list.stream();          // infers Stream<String>

        // String[] names = { "Achilleas", "Hector", "Helen"};
        var names = new String[]{ "Achilleas", "Hector", "Helen"}; // Infers from Right Hand side i.e. String[]

        for(var name: names) {
            System.out.print("\t" + name);
        }

        for(var i = 0; i < names.length; i++) {
            System.out.print("\t" + names[i]);
        }

        var var = "var";                    // Valid because 'var' is Reserved word not keyword

        int nothing;
        // var nothing;    /* Cannot use 'var' on variable without initializerJava(1073743327) */

        int a = 10, b = 20;
        // var a = 10, b = 20; /* 'var' is not allowed in a compound declarationJava(1073743324) */

        String[] nmes = { "Achilleas", "Hector", "Helen"};
        // var nmes = { "Achilleas", "Hector", "Helen"};    /* Array initializer needs an explicit target-typeJava(16778722)*/

        Function<String, Integer> wordLength = input -> input.length();
        // var wordLengthS = input -> input.length(); /* Lambda expression needs an explicit target-typeJava(16778723) */
        Function<String, Integer> length = String::length;
        // var lengthS = String::length; /* Method reference needs an explicit target-typeJava(16778724) */

        Runnable run = new Runnable() {

			@Override
			public void run() {
				System.out.println("I am Dummy!");
				
			}

        };

        var ran = new Runnable() {

			@Override
			public void run() {
				System.out.println("I am Dummy!");
				
			}

        };

        System.out.println();

        /* NON-DENOTABLE TYPE*/

        // var nuller = null; /* Cannot infer type for local variable initialized to 'null'Java(16778720) */
    }

    /* 'var' is not allowed hereJava(1073743335) */
   /* public static void print(var arg) {
        System.out.print(arg);
    } */
}