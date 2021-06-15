import com.google.gson.Gson;
import io.swagger.client.ApiException;
import io.swagger.client.ApiResponse;
import io.swagger.client.api.TextbodyApi;
import io.swagger.client.model.ErrMessage;
import io.swagger.client.model.ResultVal;
import io.swagger.client.model.TextLine;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;

/**
 * The type Consumer.
 */
public class Consumer implements Runnable {

  private final BlockingQueue<String> queue;
  private final Count syncCountSuccess;
  private final Count syncCountFailure;
  private final TextbodyApi textbodyApi;
  private final BlockingQueue<CSVRecord> csvWaitingQueue;
  private CountDownLatch countDownLatch;

  /**
   * Instantiates a new Consumer.
   *
   * @param queue            the blocking queue
   * @param syncCountSuccess the successful requests counter
   * @param syncCountFailure the unsuccessful requests counter
   * @param textbodyApi      the textbody api
   * @param csvWaitingQueue  the blocking queue
   */
  public Consumer(BlockingQueue<String> queue,
      Count syncCountSuccess, Count syncCountFailure, TextbodyApi textbodyApi,
      BlockingQueue<CSVRecord> csvWaitingQueue, CountDownLatch countDownLatch) {
    this.queue = queue;
    this.syncCountSuccess = syncCountSuccess;
    this.syncCountFailure = syncCountFailure;
    this.textbodyApi = textbodyApi;
    this.csvWaitingQueue = csvWaitingQueue;
    this.countDownLatch = countDownLatch;
  }

  @Override
  public void run() {
    try {
      // continuously consume the texts
      while (true) {
        String line = this.queue.take();
        // no more texts in blocking queue
        if (line.equals("//end of the file//")) {
          throw new InterruptedException();
        }
        process(line);
      }
    } catch (InterruptedException e) {
      countDownLatch.countDown();
      Thread.currentThread().interrupt();
    }
  }

  private void process(String line) throws InterruptedException {
    // constructs request body
    TextLine textLine = new TextLine();
    textLine.setMessage(line);

    CSVRecord csvRecord;

    // start time
    long startTime = System.currentTimeMillis();

    try {
      // send requests to server
      ApiResponse response = textbodyApi.analyzeNewLineWithHttpInfo(textLine, "wordcount");

      // end time
      long endTime = System.currentTimeMillis();
      // total run time (wall time)
      long latency = endTime - startTime;

      // if status code is 200, print result value
      this.syncCountSuccess.inc();
      ResultVal resultVal = (ResultVal) response.getData();
      csvRecord = new CSVRecord(startTime, "POST", latency, 200);
      // System.out.println(resultVal.getMessage());
    } catch (ApiException e) {
      // end time
      long endTime = System.currentTimeMillis();
      // total run time (wall time)
      long latency = endTime - startTime;

      // if status code is 4xx or 5xx, print error message
      this.syncCountFailure.inc();
      ErrMessage errMessage = new Gson().fromJson(e.getResponseBody(), ErrMessage.class);
      // System.err.println(errMessage.getMessage());
      // System.out.println(e.getCode());

      csvRecord = new CSVRecord(startTime, "POST", latency, e.getCode());
    }

    this.csvWaitingQueue.put(csvRecord);


  }


}
