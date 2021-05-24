import io.swagger.client.api.TextbodyApi;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.LinkedList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * The type Textbody api example.
 */
public class TextbodyApiExample {

  /**
   * The entry point of application.
   *
   * @param args the input arguments
   */
  public static void main(String[] args) {
    try {
      // get number of threads
      int numOfThreads = Integer.parseInt(args[0]);
      // get file name
      String file = args[1];
      // the number of arguments is 2
      if (args.length != 2) {
        throw new IllegalArgumentException("Invalid arguments");
      }
      // the number of threads mush be a positive integer
      if (numOfThreads <= 0) {
        throw new NumberFormatException();
      }
      // create a file reader
      FileReader fileReader = new FileReader(file);
      // create a blocking queue to store texts
      BlockingQueue<String> blockingQueue = new LinkedBlockingQueue<>(
          ClientConstant.NUM_OF_TEXT_WAITING_QUEUE);

      // wait to write in csv
      BlockingQueue<CSVRecord> csvWaitingQueue = new LinkedBlockingQueue<>(
          ClientConstant.NUM_OF_CSV_WAITING_QUEUE);

      // create 2 counters to count the number of successful/unsuccessful requests
      Count syncCountSuccess = new Count(0);
      Count syncCountFailure = new Count(0);

      // create an api instance to call the server
      TextbodyApi apiInstance = new TextbodyApi();
      apiInstance.getApiClient().setBasePath(ClientConstant.API_ENDPOINT);

      // create and store all the consumers
      LinkedList<Thread> threadList = new LinkedList<>();
      for (int i = 0; i < numOfThreads; i++) {
        Thread thread = new Thread(
            new Consumer(blockingQueue, syncCountSuccess, syncCountFailure, apiInstance,
                csvWaitingQueue));
        threadList.add(thread);
        thread.start();
      }

      // a thread writing to csv file
      Thread writer = new Thread(new CSVWriter(ClientConstant.REPORT_CSV, csvWaitingQueue));
      writer.start();

      // start time
      long startTime = System.currentTimeMillis();

      // create a file reader thread
      new Thread(new Reader(blockingQueue, numOfThreads, fileReader)).start();

      // wait until all threads finish their work
      for (int i = 0; i < numOfThreads; i++) {
        threadList.get(i).join();
      }

      // end time
      long endTime = System.currentTimeMillis();

      // total run time (wall time)
      long totalTime = endTime - startTime;

      // throughput(requests per second)
      double throughput =
          (syncCountSuccess.getCount() + syncCountFailure.getCount()) * 1.0 / totalTime;

      System.out.println("Total run time(wall time): " + totalTime);
      System.out.println("Successful requests: " + syncCountSuccess.getCount());
      System.out.println("Unsuccessful requests: " + syncCountFailure.getCount());
      System.out.println("Throughput(requests per second): " + throughput);

      // end sign
      csvWaitingQueue.put(new CSVRecord(0, "", 0, 200));
      writer.join();

    } catch (NumberFormatException e) {
      System.out.println("Please provide valid arguments");
    } catch (ArrayIndexOutOfBoundsException e) {
      System.out.println("Please provide correct number of arguments");
    } catch (FileNotFoundException e) {
      System.out.println("file not exist");
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

  }
}
