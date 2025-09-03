import java.util.*;
import java.util.stream.Collectors;

class Solution {

    private static final List<Integer> rowDirections = List.of(1, 0, -1, 0);
    private static final List<Integer> colDirections = List.of(0, 1, 0, -1);
    private static final int target = 123450;

    private static int serialize(List<List<Integer>> list) {
        int result = 0;
        for (var sublist : list) {
            for (var sublist2 : sublist) {
                result *= 10;
                result += sublist2;
            }
        }
        return result;
    }

    private static List<List<Integer>> deserialize(int state) {

        ArrayList<List<Integer>> result = new ArrayList<>();
        result.add(new ArrayList<>());
        result.add(new ArrayList<>());
        for (int i = 1; i >= 0; i--) {
            for (int j = 2; j >= 0; j--) {
                int exponent = i * 3 + j;
                int digit = state / (int)Math.pow(10, exponent) % 10;
                result.get(1 - i).add(digit);
            }
        }
        return result;
    }

    public static int numSteps(List<List<Integer>> initPos) {
        final var initState = serialize(initPos);
        if (initState == target)
            return 0;
        final var positionToMovesMap = new HashMap<Integer, Integer>();
        final var queue = new LinkedList<Integer>();
        positionToMovesMap.put(initState, 0);
        queue.add(initState);
        while (!queue.isEmpty()) {
            final var state = queue.poll();
            var row = 0;
            var col = 0;
            final var positions = deserialize(state);
            for (int i = 0; i < 2; i++) {
                for (int j = 0; j < 3; j++) {
                    if (positions.get(i).get(j) == 0) {
                        row = i; col = j;
                    }
                }
            }
            for (int i = 0; i < rowDirections.size(); i++) {
                int newRow = row + rowDirections.get(i);
                int newCol = col + colDirections.get(i);
                if (newRow >= 0 && newRow < 2 && newCol >= 0 && newCol < 3) {
                    final var newPositions = deserialize(state);
                    newPositions.get(row)
                            .set( col, positions.get(newRow).get(newCol));
                    newPositions.get(newRow)
                            .set(newCol, positions.get(row).get(col));
                    final var newState = serialize(newPositions);
                    if (!positionToMovesMap.containsKey(newState)) {
                        positionToMovesMap.put(newState, positionToMovesMap.get(state) + 1);
                        queue.add(newState);
                        if (newState == target) {
                            return positionToMovesMap.get(newState);
                        }
                    }
                }
            }
        }
        return -1;
    }

    public static List<String> splitWords(String s) {
        return s.isEmpty() ? List.of() : Arrays.asList(s.split(" "));
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int initPosLength = Integer.parseInt(scanner.nextLine());
        List<List<Integer>> initPos = new ArrayList<>();
        for (int i = 0; i < initPosLength; i++) {
            initPos.add(splitWords(scanner.nextLine()).stream().map(Integer::parseInt).collect(Collectors.toList()));
        }
        scanner.close();
        int res = numSteps(initPos);
        System.out.println(res);
    }
}
