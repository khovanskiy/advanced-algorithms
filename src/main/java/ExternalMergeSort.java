import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.*;

/**
 * @author Victor Khovanskiy
 */
@Slf4j
public class ExternalMergeSort {
    private final File input;
    private final File output;
    private final int M;
    private final int k;
    private final File temporaryCatalog;

    public ExternalMergeSort(File input, File output, File temporaryCatalog, int M, int k) throws IOException {
        this.temporaryCatalog = temporaryCatalog;
        temporaryCatalog.mkdirs();
        this.input = input;
        this.output = output;
        this.M = M;
        this.k = k;
    }

    private int fileId;
    private Queue<File> files = new ArrayDeque<>();

    public File createNextFile() {
        File file = new File(temporaryCatalog, "output_" + fileId++);
        file.deleteOnExit();
        return file;
    }

    public void splitPhase() throws IOException {
        byte[] buffer = new byte[M];
        try (FileInputStream inputStream = new FileInputStream(input)) {
            int bufferSize;
            while ((bufferSize = readBuffer(inputStream, buffer)) > 0) {
                Arrays.sort(buffer);
                File file = createNextFile();
                files.add(file);
                try (FileOutputStream outputStream = new FileOutputStream(file, true)) {
                    outputStream.write(buffer, 0, bufferSize);
                }
            }
        }
    }

    public void mergePhase() throws IOException {
        int L = M / k;

        int bufferOffset = 0;
        byte[] buffer = new byte[L];
        while (files.size() > 1) {
            //debug("Merge iteration");
            File output = createNextFile();

            PriorityQueue<Node> queue = new PriorityQueue<>();
            Deque<byte[]> cache = new ArrayDeque<>(k);
            int maxK = Math.min(k, files.size());
            for (int i = 0; i < maxK; ++i) {
                File file = files.poll();
                //debug(file);
                byte[] nodeBuffer;
                if (cache.isEmpty()) {
                    nodeBuffer = new byte[L];
                } else {
                    nodeBuffer = cache.pop();
                }
                queue.add(new Node(file, nodeBuffer));
            }

            try (FileOutputStream outputStream = new FileOutputStream(output, true)) {
                while (!queue.isEmpty()) {
                    Node node = queue.poll();
                    buffer[bufferOffset] = node.next();
                    ++bufferOffset;
                    if (bufferOffset == buffer.length) {
                        outputStream.write(buffer, 0, bufferOffset);
                        bufferOffset = 0;
                    }
                    if (node.hasNext()) {
                        queue.add(node);
                    } else {
                        node.file.delete();
                        cache.push(node.buffer);
                    }
                }
                if (bufferOffset > 0) {
                    outputStream.write(buffer, 0, bufferOffset);
                    bufferOffset = 0;
                }
            }

            files.add(output);
        }
    }

    public void writeOutputFile() throws IOException {
        File result = files.poll();
        byte[] buffer = new byte[M];
        try (FileInputStream inputStream = new FileInputStream(result); FileOutputStream outputStream = new FileOutputStream(output, true)) {
            int bufferSize;
            while ((bufferSize = readBuffer(inputStream, buffer)) > 0) {
                outputStream.write(buffer, 0, bufferSize);
            }
        }
        result.delete();
    }

    public void execute() throws IOException {
        splitPhase();
        mergePhase();
        writeOutputFile();
    }

    public class Node implements Comparable<Node>, Iterator<Byte>, Closeable {
        private final File file;
        private final FileInputStream inputStream;
        private long fileOffset;
        private final long fileAvailable;

        private final byte[] buffer;
        private int bufferOffset;
        private int bufferAvailable;

        public Node(File file, byte[] buffer) throws IOException {
            this.file = file;
            this.inputStream = new FileInputStream(file);
            this.buffer = buffer;
            this.fileAvailable = file.length();
        }

        @Override
        public boolean hasNext() {
            return fileOffset < fileAvailable;
        }

        @Override
        public Byte next() {
            refill();
            ++fileOffset;
            ++bufferOffset;
            return buffer[bufferOffset - 1];
        }

        public Byte current() {
            return buffer[bufferOffset];
        }

        private void refill() {
            if (bufferOffset == bufferAvailable && fileOffset < fileAvailable) {
                try {
                    bufferAvailable = readBuffer(inputStream, buffer);
                } catch (IOException e) {
                    throw new NoSuchElementException(e.getMessage());
                }
                if (bufferAvailable <= 0) {
                    throw new NoSuchElementException();
                }
                bufferOffset = 0;
            }
        }

        @Override
        public void close() throws IOException {
            inputStream.close();
        }

        @Override
        public int compareTo(Node another) {
            refill();
            another.refill();
            return Byte.compare(this.current(), another.current());
        }
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

    public void debug(Object object) {
        System.out.println(object);
    }

    public void debug(Object object, boolean shouldWrite) {
        System.out.println(object);
    }
}
