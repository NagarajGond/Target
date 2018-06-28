import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SumProblem {

    private static void sumOfxn_rec(List<Integer> list, int g, List<Integer> partial) {
        int sum = 0;
        for(Integer p : partial) {
            sum+=p;
        }
        if(sum == g) {
            System.out.println("Sum of " + partial + " = " + g);
        }
        if(sum >= g) {
            return;
        }
        for(int i=0; i<list.size(); i++) {
            int m = list.get(i);
            List<Integer> partial_r = new ArrayList<Integer>(partial);
            partial_r.add(m);
            List<Integer> remaining = new ArrayList<Integer>();
            for(int j=i+1; j<list.size(); j++) {
                remaining.add(list.get(j));
            }
            sumOfxn_rec(remaining, g, partial_r);
        }
    }

    private static void sumOfxn(List<Integer> list, int g) {
        sumOfxn_rec(list, g, new ArrayList<Integer>());
    }

    public static void main(String[] args) {
        Integer[] x = {2, 3, 1, 4}; int G = 5;

        sumOfxn(Arrays.asList(x), G);
    }
}
