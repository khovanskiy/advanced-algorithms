import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.PriorityQueue;

/**
 * @author Victor Khovanskiy
 */
@Slf4j
public class ExternalMergeSort implements Closeable {
    private final RandomAccessFile input;
    private final RandomAccessFile output;
    private final int blockSize;
    private final long totalSize;

    public ExternalMergeSort(File input, File output, int blockSize) throws IOException {
        this.totalSize = input.length();
        if (blockSize * blockSize < totalSize) {
            throw new IllegalArgumentException(String.format("Block's size must be greater or equal than block's number: %1$d * %1$d < %2$d (min %3$d)", blockSize, totalSize, (int) Math.sqrt(totalSize)));
        }
        this.input = new RandomAccessFile(input, "rw");
        this.output = new RandomAccessFile(output, "rw");
        this.blockSize = blockSize;
    }

    @Override
    public void close() throws IOException {
        try {
            input.close();
        } finally {
            output.close();
        }
    }

    public void execute() throws IOException {
        byte[] block = new byte[blockSize];
        int length;
        int blockNumber = 0;
        do {
            length = readBlock(block, 0, blockSize);
            if (length == 0) {
                continue;
            }
            Arrays.sort(block);
            //log.info(Arrays.toString(block) + " " + length);
            input.seek(blockNumber * blockSize);
            writeBlock(block, length);
            ++blockNumber;
        } while (length == blockSize);
        input.seek(0);
        PriorityQueue<Pointer<Byte>> heap = new PriorityQueue<>();

        int bufferSize = blockSize / blockNumber;
        for (int i = 0; i < blockNumber; ++i) {
            int available;
            if (i < blockNumber - 1) {
                available = blockSize;
            } else {
                available = (int) (totalSize - (blockNumber - 1) * blockSize);
            }
            heap.add(new Pointer<>(i, new Byte[bufferSize], available, this::byte2byte));
        }

        while (!heap.isEmpty()) {
            Pointer<Byte> pointer = heap.poll();
            //System.out.println(pointer);
            byte element = pointer.next();
            //System.out.println("Put " + element);
            output.writeByte(element);
            if (pointer.hasNext()) {
                heap.add(pointer);
            }
        }
    }

    protected void byte2byte(byte[] buf, Byte[] temp, int length) {
        for (int i = 0; i < length; ++i) {
            temp[i] = buf[i];
        }
    }

    @FunctionalInterface
    protected interface Converter<A, B> {
        void accept(A a, B b, int length);
    }

    @ToString
    protected class Pointer<T extends Comparable<T>> implements Comparable<Pointer<T>>, Iterator<T> {
        private final int number;
        private final T[] buffer;

        private int bufferIndex;
        private int bufferAvailable;

        private int blockIndex;
        private final int blockAvailable;

        private final byte[] temp;
        private final Converter<byte[], T[]> converter;

        public Pointer(int number, T[] buffer, int blockAvailable, Converter<byte[], T[]> converter) {
            this.number = number;
            this.buffer = buffer;
            this.temp = new byte[buffer.length];
            this.blockAvailable = blockAvailable;
            this.converter = converter;
        }

        @Override
        public int compareTo(Pointer<T> another) {
            fetch();
            another.fetch();
            assert another.buffer[another.bufferIndex] != null;
            return this.buffer[bufferIndex].compareTo(another.buffer[another.bufferIndex]);
        }

        @Override
        public boolean hasNext() {
            return blockIndex < blockAvailable;
        }

        @Override
        public T next() {
            fetch();
            ++bufferIndex;
            return buffer[bufferIndex - 1];
        }

        protected void fetch() {
            if (bufferIndex == bufferAvailable && blockIndex < blockAvailable) {
                int read;
                try {
                    input.seek(number * blockSize + blockIndex);
                    read = readBlock(temp, 0, buffer.length);
                } catch (IOException e) {
                    throw new NoSuchElementException(e.getMessage());
                }
                if (read == 0) {
                    throw new NoSuchElementException();
                }
                bufferAvailable = read;
                blockIndex += read;
                converter.accept(temp, buffer, read);
                bufferIndex = 0;
            }
        }
    }

    protected int readBlock(byte[] block, int offset, int length) throws IOException {
        int n = 0;
        do {
            int count = input.read(block, offset + n, length - n);
            if (count < 0) {
                return n;
            }
            n += count;
        } while (n < length);
        return n;
    }

    protected void writeBlock(byte[] block, int length) throws IOException {
        input.write(block, 0, length);
    }
}
