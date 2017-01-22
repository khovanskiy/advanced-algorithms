import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.*;

/**
 * @author Victor Khovanskiy
 */
@Slf4j
public class ExternalMergeSort implements Closeable {
    private final RandomAccessFile input;
    private final RandomAccessFile output;
    private final int M;
    private final long T;
    private final File catalog = new File("catalog/");

    public ExternalMergeSort(File input, File output, int M) throws IOException {
        catalog.mkdirs();
        this.T = input.length();
        this.input = new RandomAccessFile(input, "rw");
        this.output = new RandomAccessFile(output, "rw");
        this.M = M;
    }

    @Override
    public void close() throws IOException {
        try {
            input.close();
        } finally {
            output.close();
        }
    }

    private int fileId;
    private Deque<File> files = new ArrayDeque<>();

    public void splitPhase() throws IOException {
        byte[] buffer = new byte[M];
        try (FileInputStream inputStream = new FileInputStream("input")) {
            int bufferSize;
            while ((bufferSize = readBuffer(inputStream, buffer)) > 0) {
                Arrays.sort(buffer);
                File file = new File(catalog, "output_" + fileId);
                files.add(file);
                try (FileOutputStream outputStream = new FileOutputStream(file, true)) {
                    outputStream.write(buffer, 0, bufferSize);
                }
                ++fileId;
            }
        }
    }

    public void mergePhase() throws IOException {

    }

    public int readBuffer(FileInputStream inputStream, byte[] buffer) throws IOException {
        int bufferOffset = 0;
        do {
            int readCount = inputStream.read(buffer, bufferOffset, buffer.length - bufferOffset);
            if (readCount < 0) {
                break;
            }
            bufferOffset += readCount;
        } while (bufferOffset < buffer.length);
        return bufferOffset;
    }

    public int phase1() throws IOException {
        byte[] block = new byte[M];
        int length;
        int blockNumber = 0;
        do {
            length = readBlock(input, block, 0, M);
            if (length == 0) {
                continue;
            }
            Arrays.sort(block);
            debug(Arrays.toString(block));
            input.seek(blockNumber * M);
            input.write(block, 0, length);
            ++blockNumber;
        } while (length == M);
        return blockNumber;
    }

    public void phase2() throws IOException {
        final int k = 16;

        int bufferSize = M;
        // (k + 1) * L <= M

        RandomAccessFile inputAtIteration = input;
        RandomAccessFile outputAtIteration = output;
        long blockSizeAtIteration = M;

        Deque<byte[]> buffersCache = new ArrayDeque<>();
        byte[] outputBuffer = new byte[bufferSize];
        while (blockSizeAtIteration < T) {
            debug("Новая итерация с blockSizeAtIteration = " + blockSizeAtIteration, true);
            inputAtIteration.seek(0);
            outputAtIteration.seek(0);
            long fileOffset = 0;
            int blocksCountAtIteration = getBlocksNumber(T, blockSizeAtIteration);
            for (int blockMerged = 0; blockMerged < blocksCountAtIteration; blockMerged += k) {
                PriorityQueue<Pointer> heap = new PriorityQueue<>();
                for (int i = 0; i < Math.min(k, blocksCountAtIteration - blockMerged); ++i) {
                    long available = Math.min(T - fileOffset, blockSizeAtIteration);

                    byte[] nodeBuffer;
                    if (buffersCache.isEmpty()) {
                        nodeBuffer = new byte[bufferSize];
                    } else {
                        nodeBuffer = buffersCache.pop();
                    }
                    heap.add(new Pointer(inputAtIteration, i, nodeBuffer, fileOffset, available));
                    fileOffset += blockSizeAtIteration;
                }

                int bufferIndex = 0;
                while (!heap.isEmpty()) {
                    Pointer pointer = heap.poll();
                    debug("Извлекаем " + pointer);
                    byte element = pointer.next();
                    debug("Пишем в буфер " + element);
                    outputBuffer[bufferIndex] = element;
                    ++bufferIndex;
                    // Если буфер для записи заполнен, то пишем его на диск и обнуляем смещение.
                    if (bufferIndex == outputBuffer.length) {
                        debug("Пишем буфер в файл");
                        outputAtIteration.write(outputBuffer, 0, outputBuffer.length);
                        bufferIndex = 0;
                    }
                    // Если узел содержит элементы, то добавляет его обратно в кучу.
                    if (pointer.hasNext()) {
                        heap.add(pointer);
                    } else {
                        buffersCache.push(pointer.buffer);
                    }
                }
                if (bufferIndex > 0) {
                    outputAtIteration.write(outputBuffer, 0, bufferIndex);
                }
            }
            if (inputAtIteration == output) {
                inputAtIteration = input;
                outputAtIteration = output;
            } else {
                inputAtIteration = output;
                outputAtIteration = input;
            }
            blockSizeAtIteration <<= 1;
        }

        if (outputAtIteration != input) {
            input.seek(0);
            output.seek(0);
            byte[] buffer = new byte[M];
            int read;
            while ((read = readBlock(input, buffer, 0, buffer.length)) > 0) {
                output.write(buffer, 0, read);
            }
        }
    }

    public void debug(Object object) {
        //System.out.println(object);
    }

    public void debug(Object object, boolean shouldWrite) {
        System.out.println(object);
    }

    public static int getBlocksNumber(long totalSize, long blockSize) {
        return (int) Math.ceil(1d * totalSize / blockSize);
    }

    public void execute() throws IOException {
        //phase1();
        //phase2();
        splitPhase();
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

    protected class Pointer implements Comparable<Pointer>, Iterator<Byte> {
        private final int number;
        private final byte[] buffer;

        /**
         * Позиция текущего элемента в буфере
         */
        private int bufferIndex;
        /**
         * Доступный размер буфера
         */
        private int bufferAvailable;

        /**
         * Смещение блока относительно начала файла
         */
        private final long fileOffset;
        /**
         * Смещение относительно начала блока
         */
        private long blockOffset;
        /**
         * Размер блока
         */
        private final long blockAvailable;

        private final RandomAccessFile file;

        public Pointer(RandomAccessFile file, int ordinal, byte[] buffer, long fileOffset, long blockAvailable) {
            if (fileOffset + blockAvailable > T) {
                throw new AssertionError();
            }
            this.file = file;
            this.number = ordinal;
            this.buffer = buffer;
            this.fileOffset = fileOffset;
            this.blockAvailable = blockAvailable;
        }

        @Override
        public int compareTo(Pointer another) {
            fetch();
            another.fetch();
            return Byte.compare(this.buffer[bufferIndex], another.buffer[another.bufferIndex]);
        }

        @Override
        public boolean hasNext() {
            return blockOffset < blockAvailable;
        }

        @Override
        public Byte next() {
            fetch();
            ++bufferIndex;
            ++blockOffset;
            return buffer[bufferIndex - 1];
        }

        protected void fetch() {
            if (bufferIndex == bufferAvailable && blockOffset < blockAvailable) {
                int read;
                try {
                    file.seek(fileOffset + blockOffset);
                    read = readBlock(file, buffer, 0, buffer.length);
                } catch (IOException e) {
                    throw new NoSuchElementException(e.getMessage());
                }
                if (read == 0) {
                    throw new NoSuchElementException();
                }
                // Сдвигаем смещение относительно начала блока на количество прочитанных элементов буфера
                //blockOffset += bufferAvailable;
                // Доступный размер буфера равен количеству прочитанных элементов
                bufferAvailable = read;
                // Обнуляем позицию в буфере
                bufferIndex = 0;
            }
        }

        public String toString() {
            return "Pointer(number=" + this.number + ", buffer=" + Arrays.toString(this.buffer) + ", bufferIndex=" + this.bufferIndex + ", bufferAvailable=" + this.bufferAvailable + ", fileOffset=" + this.fileOffset + ", blockOffset=" + this.blockOffset + ", blockAvailable=" + this.blockAvailable + ")";
        }
    }

    protected int readBlock(RandomAccessFile file, byte[] block, int offset, int length) throws IOException {
        int n = 0;
        do {
            int count = file.read(block, offset + n, length - n);
            if (count < 0) {
                return n;
            }
            n += count;
        } while (n < length);
        return n;
    }
}
