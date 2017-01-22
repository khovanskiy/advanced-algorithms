import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * @author Victor Khovanskiy
 */
@Slf4j
public class ExternalMergeSortTest {

    private static final String INPUT_FILENAME = "input";
    private static final String OUTPUT_FILENAME = "output";
    private static final String TEMPORARY_CATALOG = "temp";
    private static final int SEED = 12345;
    private static final Random RANDOM = new Random(SEED);

    /*@Test
    public void internal() {
        log.info("Starting internal sorting...");
        long start = System.currentTimeMillis();
        Arrays.sort(array);
        long duration = System.currentTimeMillis() - start;
        log.info("Done in " + (duration / 1000f) + " s");
    }*/

    @Test
    public void external() {
        List<ExecutionTest> resultList = new ArrayList<>();
        for (int T : Arrays.asList(100000, 1000000, 10000000)) {
            int minM = (int) Math.sqrt(T) + 1;
            for (int M = minM; M < T; M *= 10) {
                for (int k = 2; k <= 32; k *= 2) {
                    ExecutionTest test = new ExecutionTest(T, M, k);
                    test.run();
                    resultList.add(test);
                }
            }
        }
        for (int i = 0; i < resultList.size(); ++i) {
            ExecutionTest result = resultList.get(i);
            System.out.println(String.format("%d | %d | %d | %d | %d", i + 1, result.T, result.M, result.k, result.externalTime));
        }
    }

    @Test
    public void single() {
        ExecutionTest result = new ExecutionTest(100000, 31700, 2);
        result.run();
        System.out.println(String.format("%d\t%d\t%d", result.T, result.M, result.externalTime));
    }

    private boolean delete(String filename) {
        return new File(filename).delete();
    }

    public class ExecutionTest implements Runnable {
        private final int T;
        private final int M;
        private final int k;
        private long externalTime;
        private long internalTime;

        private final byte[] array;

        public ExecutionTest(int T, int M, int k) {
            this.T = T;
            this.M = M;
            this.k = k;
            this.array = new byte[T];
        }

        public void before() throws IOException {
            log.info("Prepare array");
            /*for (int i = array.length - 1; i >= 0; --i) {
                array[i] = (byte) i;
            }*/
            RANDOM.nextBytes(array);

            log.info("Prepare data file");
            if (delete(INPUT_FILENAME)) {
                log.info("INPUT is deleted");
            }
            if (delete(OUTPUT_FILENAME)) {
                log.info("OUTPUT is deleted");
            }
            try (RandomAccessFile file = new RandomAccessFile(new File(INPUT_FILENAME), "rw")) {
                int written = 0;
                while (written < array.length) {
                    int length = Math.min(M, array.length - written);
                    file.write(array, written, length);
                    written += length;
                }
            }
        }

        public void internal() {
            log.info(String.format("Starting internal sorting T = %d, M = %d, k = %d", T, M, k));

            long start = System.currentTimeMillis();
            Arrays.sort(array);
            long duration = System.currentTimeMillis() - start;
            this.internalTime = duration;
            log.info("Done in " + (duration / 1000f) + " s");
        }

        public void external() throws IOException {
            log.info(String.format("Starting external sorting T = %d, M = %d", T, M));

            long start = System.currentTimeMillis();
            ExternalMergeSort mergeSort = new ExternalMergeSort(new File(INPUT_FILENAME), new File(OUTPUT_FILENAME), new File(TEMPORARY_CATALOG), M, k);
            mergeSort.execute();

            long duration = System.currentTimeMillis() - start;
            this.externalTime = duration;
            log.info("Done in " + (duration / 1000f) + " s");

            log.info("Starting testing the result...");
            byte[] block = new byte[M];
            int previous = Byte.MIN_VALUE - 1;
            start = System.currentTimeMillis();
            try (RandomAccessFile file = new RandomAccessFile(OUTPUT_FILENAME, "r")) {
                int read;
                while ((read = file.read(block, 0, block.length)) >= 0) {
                    for (int i = 0; i < read; ++i) {
                        byte current = block[i];
                        Assert.assertTrue("Previous byte must be less or equal than current", previous <= current);
                        previous = current;
                    }
                }
            }

            duration = System.currentTimeMillis() - start;
            log.info("Done in " + (duration / 1000f) + " s");
        }

        public void after() throws IOException {
            log.info("Clean up files");
            if (delete(INPUT_FILENAME)) {
                log.info("INPUT is deleted");
            }
            if (delete(OUTPUT_FILENAME)) {
                log.info("OUTPUT is deleted");
            }
        }

        @Override
        public void run() {
            try {
                before();
                external();
                internal();
                after();
            } catch (Exception e) {
                log.error(e.getLocalizedMessage(), e);
            }
        }
    }
}
