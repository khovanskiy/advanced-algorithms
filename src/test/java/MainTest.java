import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;

import java.util.*;

/**
 * victor
 */
public class MainTest {
    private static final Random RANDOM = new Random(55555);
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(MainTest.class);

    @Test
    public void deleteSimple() {
        AVLTree<Integer> tree1 = AVLTree.create();
        log.info("# 1 " + tree1);
        AVLTree<Integer> tree2 = AVLTree.insert(tree1, 42);
        log.info("# 2 " + tree2);
        AVLTree<Integer> tree3 = AVLTree.delete(tree2, 42);
        log.info("# 3 " + tree3);
        Assert.assertEquals("Tree must return the previous state", tree1, tree3);
    }

    @Test
    public void test() {
        Set<Integer> values = new HashSet<>();
        AVLTree<Integer> current = AVLTree.create();
        for (int i = 0; i < 1000; ++i) {
            Operation<Integer> operation = generateOperation(values);
            log.info("#" + (i + 1) + " " + operation);
            switch (operation.type) {
                case FIND: {
                    Optional<Integer> optional = AVLTree.find(current, operation.argument);
                    if (values.contains(operation.argument)) {
                        Assert.assertTrue("Tree must contains element", optional.isPresent());
                        Assert.assertEquals("Tree must contains certain element", operation.argument, optional.get());
                    } else {
                        Assert.assertTrue("Tree must not contains element", !optional.isPresent());
                    }
                }
                break;
                case INSERT: {
                    AVLTree<Integer> previousCopy = AVLTree.deepCopy(current);
                    AVLTree<Integer> previous = current;
                    current = AVLTree.insert(current, operation.argument);
                    Assert.assertTrue("Tree must be balanced", AVLTree.isBalanced(current));
                    Assert.assertEquals("Operation must not change data in the object", previousCopy, previous);
                    values.add(operation.argument);
                }
                break;
                case DELETE: {
                    AVLTree<Integer> previousCopy = AVLTree.deepCopy(current);
                    AVLTree<Integer> previous = current;
                    current = AVLTree.delete(current, operation.argument);
                    Assert.assertTrue("Tree must be balanced", AVLTree.isBalanced(current));
                    Assert.assertEquals("Operation must not change data in the object", previousCopy, previous);
                    values.remove(operation.argument);
                }
                break;
            }
        }
    }

    public <T> T random(Set<T> set) {
        List<T> list = new ArrayList<>(set);
        return list.get(RANDOM.nextInt(list.size()));
    }

    public Operation<Integer> generateOperation(Set<Integer> values) {
        if (RANDOM.nextInt() % 2 == 0 && !values.isEmpty()) {
            return new Operation<>(OperationType.random(), random(values));
        } else {
            return new Operation<>(OperationType.random(), RANDOM.nextInt(1000));
        }
    }

    private enum OperationType {
        FIND,
        INSERT,
        DELETE;

        private static final List<OperationType> VALUES =
                Collections.unmodifiableList(Arrays.asList(values()));
        private static final int SIZE = VALUES.size();

        public static OperationType random() {
            return VALUES.get(RANDOM.nextInt(SIZE));
        }
    }

    private class Operation<T> {
        private final OperationType type;
        private final T argument;

        private Operation(OperationType type, T argument) {
            this.type = type;
            this.argument = argument;
        }

        public String toString() {
            return "Operation(type=" + this.type + ", argument=" + this.argument + ")";
        }
    }
}
