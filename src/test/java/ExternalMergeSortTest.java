import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.Random;

/**
 * @author Victor Khovanskiy
 */
@Slf4j
public class ExternalMergeSortTest {

    private static final String INPUT_FILENAME = "input";
    private static final String OUTPUT_FILENAME = "output";
    private static final int COUNT_OF_NUMBERS = 1024 * 1024;
    private static final int BLOCK_SIZE = 1024;
    private static final int SEED = 12345;
    private static final Random RANDOM = new Random(SEED);
    private byte[] array = new byte[COUNT_OF_NUMBERS];

    @Before
    public void before() throws IOException {
        log.info("Prepare array");
        RANDOM.nextBytes(array);

        log.info("Prepare data file");
        delete(INPUT_FILENAME);
        delete(OUTPUT_FILENAME);
        try (RandomAccessFile file = new RandomAccessFile(new File(INPUT_FILENAME), "rw")) {
            for (int i = 0; i < array.length; ++i) {
                file.writeByte(array[i]);
            }
        }
    }

    @Test
    public void internal() {
        log.info("Starting internal sorting...");
        long start = System.currentTimeMillis();
        Arrays.sort(array);
        long duration = System.currentTimeMillis() - start;
        log.info("Done in " + (duration / 1000f) + " s");
    }

    @Test
    public void external() throws IOException {
        log.info("Starting external sorting...");

        long start = System.currentTimeMillis();
        try (ExternalMergeSort mergeSort = new ExternalMergeSort(new File(INPUT_FILENAME), new File(OUTPUT_FILENAME), BLOCK_SIZE)) {
            mergeSort.execute();
        }
        long duration = System.currentTimeMillis() - start;
        log.info("Done in " + (duration / 1000f) + " s");

        log.info("Starting testing the result...");
        byte[] block = new byte[BLOCK_SIZE];
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

    @After
    public void after() throws IOException {
        log.info("Clean up files");
        delete(INPUT_FILENAME);
        delete(OUTPUT_FILENAME);
    }

    private boolean delete(String filename) {
        return new File(filename).delete();
    }
}
