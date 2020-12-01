import java.util.Arrays;
import java.util.stream.Collectors;

public class NestBasedAccessControl {
    private int i = 10;

    private int getNumber() {
        return i;
    }

    public class InnerClass {
        
        private void printNumber() {
            System.out.printf("The Number %s \n", getNumber());
        }
    }

    public static void main(String... args) {
        InnerClass ic = new NestBasedAccessControl().new InnerClass();
        System.out.printf("Inner Class Nest Host : %s\n", InnerClass.class.getNestHost().getName());
        System.out.printf("Outer Class Nest Host : %s\n", NestBasedAccessControl.class.getNestHost().getName());
        System.out.printf("Inner Class Nest Members : %s\n", 
             Arrays.asList(InnerClass.class.getNestMembers())
             .stream().map(clazz -> clazz.getName()).collect(Collectors.joining("  ")));
        System.out.printf("Outer Class Nest Members : %s\n", 
             Arrays.asList(InnerClass.class.getNestMembers())
             .stream().map(clazz -> clazz.getName()).collect(Collectors.joining("  ")));
        ic.printNumber();
    }
}