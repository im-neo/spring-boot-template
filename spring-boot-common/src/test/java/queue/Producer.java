package queue;

import lombok.extern.slf4j.Slf4j;

import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class Producer implements Runnable {

    //这里的正在运行标记不共享，但是需要用volatile保证可以实时接收到它的更新
    private volatile boolean isRunning = true;

    private BlockingQueue<QueueData> queue;

    //生成的数据,这里要使用static保证在多个线程之间共享
    private static AtomicInteger count = new AtomicInteger();

    private static final int SLEEP_TIME = 1000;

    public Producer(BlockingQueue<QueueData> queue) {
        this.queue = queue;
    }

    @Override
    public void run() {
        QueueData data;
        log.info("Producer Thread id: {} started!", Thread.currentThread().getId());
        Random random = new Random();


        try {
            while (isRunning) {
                Thread.sleep(random.nextInt(SLEEP_TIME));
                data = new QueueData(count.incrementAndGet());
                log.info("{}  is put into bq", data.getNum());
                if (queue.offer(data, 2, TimeUnit.SECONDS)) {
                    //生产者向bq中添加数据
                    log.info("producer {} put data : {}", Thread.currentThread().getId(), data.getNum());
                } else {
                    log.info("producer put data failed!");
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            Thread.interrupted();
        }


    }

    //停止线程
    public void stop() {
        isRunning = false;
    }
}
