import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class QueueTest {

    public static ExecutorService executorService = new ThreadPoolExecutor(2, 2,
            0, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(512), // 使用有界队列，避免OOM
            new ThreadPoolExecutor.DiscardPolicy());

    private static Queue<Integer> queue = new LinkedList<>();

    static AtomicInteger atomic = new AtomicInteger();

    public static void main(String[] args) {
        int index = 0;
        int limit = 10000;
        while (index < limit) {
            executorService.execute(() -> {
                try {
                    producer();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            index++;
        }
        System.out.println("=============");
        while (!queue.isEmpty()) {
            executorService.execute(() -> {
                try {
                    consumer();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            index++;
        }
    }


    public static void producer() throws Exception {
        Thread.sleep(5);
        queue.offer(atomic.incrementAndGet());
    }


    public static void consumer() throws Exception {
        Thread.sleep(3);
        System.out.println(queue.poll());
    }
}
