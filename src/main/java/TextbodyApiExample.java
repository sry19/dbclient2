import io.swagger.client.api.TextbodyApi;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
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

      dataAnalysis();

    } catch (NumberFormatException e) {
      System.out.println("Please provide valid arguments");
    } catch (ArrayIndexOutOfBoundsException e) {
      System.out.println("Please provide correct number of arguments");
    } catch (FileNotFoundException e) {
      System.out.println("file not exist");
    } catch (InterruptedException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }

  }

  private static void dataAnalysis() throws IOException {

    List<Integer> timeList = new LinkedList<>();
    File report = new File(ClientConstant.REPORT_CSV);
    FileReader fileReader = new FileReader(report);
    BufferedReader reader = new BufferedReader(fileReader);

    String line = reader.readLine();
    while (line != null) {
      String[] lst = line.split(", ");
      timeList.add(Integer.parseInt(lst[2]));
      line = reader.readLine();
    }

    // calculate mean response time
    long sum = 0;
    int max = 0;
    for (int i = 0; i < timeList.size(); i++) {
      sum += timeList.get(i);
      max = Math.max(max, timeList.get(i));
    }
    long mean = sum / timeList.size();
    System.out.println("Mean response time for POSTs (millisecs): " + mean);
    System.out.println("Max response time for POSTs: " + max);

    int[] timeLst = new int[timeList.size()];
    for (int i = 0; i < timeList.size(); i++) {
      timeLst[i] = timeList.get(i);
    }
    int median;
    if (timeList.size() % 2 == 0) {
      median = (findKthLargest(timeLst, timeLst.length / 2 + 1) + findKthLargest(timeLst,
          timeLst.length / 2)) / 2;
    } else {
      median = findKthLargest(timeLst, timeList.size() / 2 + 1);
    }
    System.out.println("Median response time for POSTs (millisecs): " + median);

    int p99 = findKthLargest(timeLst, (int) Math.floor(timeLst.length * 0.01));
    System.out.println("p99 (99th percentile) response time for POSTs: " + p99);
  }

  public static int getRandomNumber(int min, int max) {
    return (int) ((Math.random() * (max - min)) + min);
  }

  // quick select algorithm
  public static int partition(int[] nums, int low, int high) {
    // choose random pivot
    int randIndex = getRandomNumber(low, high); // [low, high)
    // swap it with last element
    int temp = nums[randIndex];
    nums[randIndex] = nums[high];
    nums[high] = temp;

    int pivot = nums[high];
    int i = low - 1;
    for (int j = low; j < high; j++) {
      if (nums[j] <= pivot) {
        // swap i+1 and j
        i++;
        temp = nums[i];
        nums[i] = nums[j];
        nums[j] = temp;
      }
    }
    // j reached to high
    // swap i+1 and pivot index
    // all elements less than (i+1)th index is less than pivot
    // and all elements greater than (i+1)th index is greater than pivot
    temp = nums[i + 1];
    nums[i + 1] = nums[high];
    nums[high] = temp;
    return (i + 1); // new pivot
  }

  public static int quickSelect(int[] nums, int low, int high, int k) {
    int pivot = partition(nums, low, high);
    if (pivot == k) {
      // kth largest found
      return nums[pivot];
    }
    if (k > pivot) {
      return quickSelect(nums, pivot + 1, high, k);
    }
    return quickSelect(nums, low, pivot - 1, k);

  }

  public static int findKthLargest(int[] nums, int k) {
    return quickSelect(nums, 0, nums.length - 1, nums.length - k);
  }
}

