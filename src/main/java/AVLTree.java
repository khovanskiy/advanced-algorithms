import lombok.ToString;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * victor
 */
@ToString
public class AVLTree<A> {
    private static final int EQ = 0;
    private static final int LT = -1;
    private static final int GT = 1;

    private AVLTree() {
    }

    public static <T> AVLLeaf<T> create() {
        return new AVLLeaf<>();
    }

    private static <T> int cachedHeight(AVLTree<T> tree) {
        if (tree instanceof AVLTree.AVLLeaf) {
            return 0;
        }
        return ((AVLNode<T>) tree).hh;
    }

    private static <T> int bFactor(AVLNode<T> tree) {
        return cachedHeight(tree.right) - cachedHeight(tree.left);
    }

    private static <T> int height(AVLTree<T> l, AVLTree<T> r) {
        int hl = cachedHeight(l);
        int hr = cachedHeight(r);
        return Math.max(hl, hr) + 1;
    }

    private static <T> AVLTree<T> create(T k, AVLTree<T> l, AVLTree<T> r) {
        return new AVLNode<>(k, height(l, r), l, r);
    }

    private static <T> AVLTree<T> rotateRight(AVLTree<T> node) {
        // match
        assert node instanceof AVLTree.AVLNode;
        T p = ((AVLNode<T>) node).key;
        assert ((AVLNode<T>) node).left instanceof AVLTree.AVLNode;
        T q = ((AVLNode<T>) ((AVLNode<T>) node).left).key;
        AVLTree<T> a = ((AVLNode<T>) ((AVLNode<T>) node).left).left;
        AVLTree<T> b = ((AVLNode<T>) ((AVLNode<T>) node).left).right;
        AVLTree<T> c = ((AVLNode<T>) node).right;
        // where
        AVLTree<T> newP = new AVLNode<>(p, height(b, c), b, c);
        return create(q, a, newP);
    }

    private static <T> AVLTree<T> rotateLeft(AVLTree<T> node) {
        // match
        assert node instanceof AVLTree.AVLNode;
        T q = ((AVLNode<T>) node).key;
        AVLTree<T> a = ((AVLNode<T>) node).left;
        assert ((AVLNode<T>) node).right instanceof AVLTree.AVLNode;
        T p = ((AVLNode<T>) ((AVLNode<T>) node).right).key;
        AVLTree<T> b = ((AVLNode<T>) ((AVLNode<T>) node).right).left;
        AVLTree<T> c = ((AVLNode<T>) ((AVLNode<T>) node).right).right;
        // where
        AVLTree<T> newQ = new AVLNode<>(q, height(a, b), a, b);
        return create(p, newQ, c);
    }

    public static <T> AVLTree<T> deepCopy(AVLTree<T> tree) {
        if (tree instanceof AVLLeaf) {
            return new AVLLeaf<>();
        }
        AVLNode<T> node = (AVLNode<T>) tree;
        T k = node.key;
        int h = node.hh;
        AVLTree<T> l = node.left;
        AVLTree<T> r = node.right;
        return new AVLNode<>(k, h, deepCopy(l), deepCopy(r));
    }

    public static <T> boolean isBalanced(AVLTree<T> tree) {
        if (tree instanceof AVLLeaf) {
            return true;
        }
        AVLNode<T> node = (AVLNode<T>) tree;
        return bFactor(node) < 2;
    }

    private static <T> AVLTree<T> balance(AVLTree<T> tree) {
        if (tree instanceof AVLTree.AVLNode) {
            AVLNode<T> node = (AVLNode<T>) tree;
            T k = node.key;
            int h = node.hh;
            AVLTree<T> l = node.left;
            AVLTree<T> r = node.right;
            if (bFactor(node) == 2) {
                assert node.right instanceof AVLTree.AVLNode;
                if (bFactor((AVLNode<T>) node.right) < 0) {
                    return rotateLeft(new AVLNode<>(k, h, l, rotateRight(r)));
                } else {
                    return rotateLeft(node);
                }
            } else if (bFactor(node) == -2) {
                assert node.left instanceof AVLTree.AVLNode;
                if (bFactor((AVLNode<T>) node.left) > 0) {
                    return rotateRight(new AVLNode<>(k, h, rotateLeft(l), r));
                } else {
                    return rotateRight(node);
                }
            }
        }
        return tree;
    }

    private static <T> Pair<T, AVLTree<T>> helper(AVLNode<T> node) {
        T m = node.key;
        AVLTree<T> r = node.right;
        if (node.left instanceof AVLTree.AVLLeaf) {
            return new Pair<>(m, r);
        }
        assert node.left instanceof AVLTree.AVLNode;
        AVLNode<T> l = (AVLNode<T>) node.left;
        Pair<T, AVLTree<T>> temp = helper(l);
        T newK = temp.f;
        AVLTree<T> newL = temp.s;
        return new Pair<>(newK, balance(create(m, newL, r)));
    }

    private static <T> AVLTree<T> deleteRoot(AVLNode<T> node) {
        if (node.left instanceof AVLTree.AVLLeaf) {
            return node.right;
        }
        AVLNode<T> l = (AVLNode<T>) node.left;
        if (node.right instanceof AVLTree.AVLLeaf) {
            return node.left;
        }
        AVLNode<T> r = (AVLNode<T>) node.right;
        if (((AVLNode<T>) node.right).left instanceof AVLTree.AVLLeaf) {
            T y = ((AVLNode<T>) node.right).key;
            AVLTree<T> newR = ((AVLNode<T>) node.right).right;
            return create(y, l, newR);
        }
        Pair<T, AVLTree<T>> temp = helper(r);
        T newK = temp.f;
        AVLTree<T> newR = temp.s;
        return balance(create(newK, l, newR));
    }

    public static <T> List<T> toList(AVLTree<T> tree) {
        List<T> list = new ArrayList<>();
        if (tree instanceof AVLTree.AVLLeaf) {
            return list;
        }
        assert tree instanceof AVLTree.AVLNode;
        T x = ((AVLNode<T>) tree).key;
        AVLTree<T> l = ((AVLNode<T>) tree).left;
        AVLTree<T> r = ((AVLNode<T>) tree).right;
        list.addAll(toList(l));
        list.add(x);
        list.addAll(toList(r));
        return list;
    }

    public static <T extends Comparable<T>> Optional<T> find(AVLTree<T> tree, T m) {
        if (tree instanceof AVLLeaf) {
            return Optional.empty();
        }
        assert tree instanceof AVLNode;
        AVLNode<T> node = (AVLNode<T>) tree;
        T k = node.key;
        AVLTree<T> l = node.left;
        AVLTree<T> r = node.right;
        switch (m.compareTo(k)) {
            case EQ:
                return Optional.of(k);
            case LT:
                return find(l, m);
            default:
                return find(r, m);
        }
    }

    public static <T extends Comparable<T>> AVLTree<T> insert(AVLTree<T> tree, T m) {
        if (tree instanceof AVLLeaf) {
            return new AVLNode<>(m, 0, new AVLLeaf<>(), new AVLLeaf<>());
        }
        assert tree instanceof AVLNode : tree.getClass();
        AVLNode<T> node = (AVLNode<T>) tree;
        T k = node.key;
        int h = node.hh;
        AVLTree<T> l = node.left;
        AVLTree<T> r = node.right;
        switch (m.compareTo(k)) {
            case EQ:
                return new AVLNode<>(m, h, l, r);
            case LT:
                return balance(create(k, insert(l, m), r));
            default:
                return balance(create(k, l, insert(r, m)));
        }
    }

    public static <T extends Comparable<T>> AVLTree<T> delete(AVLTree<T> tree, T m) {
        if (tree instanceof AVLLeaf) {
            return new AVLLeaf<>();
        }
        assert tree instanceof AVLNode;
        AVLNode<T> node = (AVLNode<T>) tree;
        T k = node.key;
        AVLTree<T> l = node.left;
        AVLTree<T> r = node.right;
        switch (m.compareTo(k)) {
            case GT:
                return balance(create(k, l, delete(r, m)));
            case LT:
                return balance(create(k, delete(l, m), r));
            default:
                return deleteRoot(node);
        }
    }

    private static class AVLLeaf<A> extends AVLTree<A> {
        @Override
        public boolean equals(Object obj) {
            return obj instanceof AVLLeaf;
        }

        @Override
        public int hashCode() {
            return 59;
        }

        public String toString() {
            return "Leaf()";
        }
    }

    private static class AVLNode<A> extends AVLTree<A> {
        private final A key;
        private final int hh;
        private final AVLTree<A> left;
        private final AVLTree<A> right;

        public AVLNode(A key, int hh, AVLTree<A> left, AVLTree<A> right) {
            assert key != null;
            assert hh >= 0;
            assert left != null;
            assert right != null;
            this.key = key;
            this.hh = hh;
            this.left = left;
            this.right = right;
        }

        public boolean equals(Object o) {
            if (o instanceof AVLNode) {
                AVLNode another = (AVLNode) o;
                return this.key.equals(another.key) && this.hh == another.hh && this.left.equals(another.left) && this.right.equals(another.right);
            }
            return false;
        }

        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            result = result * PRIME + key.hashCode();
            result = result * PRIME + hh;
            result = result * PRIME + left.hashCode();
            result = result * PRIME + right.hashCode();
            return result;
        }

        public String toString() {
            return "Pointer(key=" + this.key + ", hh=" + this.hh + ", left=" + this.left + ", right=" + this.right + ")";
        }
    }

    private static class Pair<F, S> {
        private final F f;
        private final S s;

        public Pair(F f, S s) {
            this.f = f;
            this.s = s;
        }
    }
}
