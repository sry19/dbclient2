import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;

/**
 * The type Csv writer.
 */
public class CSVWriter implements Runnable {

  private final String filePath;
  private final BlockingQueue<CSVRecord> csvRecordBlockingQueue;

  /**
   * Instantiates a new Csv writer.
   *
   * @param filePath               the file path
   * @param csvRecordBlockingQueue the csv record blocking queue
   */
  public CSVWriter(String filePath,
      BlockingQueue<CSVRecord> csvRecordBlockingQueue) {
    this.filePath = filePath;
    this.csvRecordBlockingQueue = csvRecordBlockingQueue;
  }

  @Override
  public void run() {
    BufferedWriter bw = null;

    try {
      File file = new File(this.filePath);
      if (!file.exists()) {
        file.createNewFile();
      }
      FileWriter fw = new FileWriter(file);
      bw = new BufferedWriter(fw);
      // continuously consume the texts
      while (true) {
        CSVRecord record = this.csvRecordBlockingQueue.take();
        if (record.getRequestType().equals("")) {
          throw new InterruptedException();
        }
        bw.write(record.toString() + "\n");

      }
    } catch (InterruptedException | IOException e) {
      Thread.currentThread().interrupt();
    } finally {
      if (bw != null) {
        try {
          bw.close();
        } catch (IOException e) {
          System.out.println("Error in closing the BufferedWriter" + e);
        }
      }
    }
  }
}
